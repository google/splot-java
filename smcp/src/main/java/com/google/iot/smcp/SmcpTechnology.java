/*
 * Copyright (C) 2018 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.iot.smcp;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;
import com.google.iot.coap.*;
import com.google.iot.m2m.base.*;
import com.google.iot.m2m.local.LocalTechnology;
import com.google.iot.m2m.local.ResourceLink;
import com.google.iot.m2m.local.ResourceLinkManager;
import com.google.iot.m2m.util.NestedPersistentStateManager;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.URI;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Logger;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * M2M Technology class for the Simple Monitoring and Control Protocol (SMCP).
 *
 * <p>This class is used to obtain {@link Thing} instances for things
 * which are remotely hosted using SMCP. It can also be used for hosting other things
 * (Local or otherwise) to allow them to be used by other devices which support SMCP or CoAP.
 */
public final class SmcpTechnology implements Technology, PersistentStateInterface, ResourceLinkManager {
    private static final boolean DEBUG = false;
    private static final Logger LOGGER = Logger.getLogger(SmcpTechnology.class.getCanonicalName());

    private static final String GROUP_PREFIX = "group-";

    final LocalEndpointManager mLocalEndpointManager;
    private final Server mServer;
    LocalEndpoint mDefaultLocalEndpoint = null;

    // We want to be able to look up groups that aren't hosted, but we also want them
    // to be collected if we don't keep references of them around. Thus we are using
    // a WeakHashMap here.
    private final WeakHashMap<String, WeakReference<SmcpGroup>> mGroups = new WeakHashMap<>();

    // Handler for .well-known/core
    private final WellKnownCoreHandler mWellKnownCoreResource = new WellKnownCoreHandler();

    private final Map<Thing, HostedThingAdapter> mHostedAdapterLookup =
            new HashMap<>();

    private final NestedPersistentStateManager mNestedPersistentStateManager =
            new NestedPersistentStateManager();

    private final Map<URI, WeakReference<SmcpThing>> mNativeThings =
            Collections.synchronizedMap(new WeakHashMap<>());

    final LocalTechnology mLocalTechnology;

    private final Resource<InboundRequestHandler> mRootResource = new Resource<>();

    private final GroupsResource mGroupsResource = new GroupsResource(this);

    private final Map<URI, WeakReference<ResourceLink<Object>>> mResourceLinkCache
            = new WeakHashMap<>();

    /** Gets the CoAP {@link Server} object used to host things. */
    public Server getServer() {
        return mServer;
    }

    /** Gets the CoAP {@link LocalEndpointManager} object used to construct this instance. */
    public LocalEndpointManager getLocalEndpointManager() {
        return mLocalEndpointManager;
    }

    public SmcpTechnology(LocalEndpointManager localEndpointManager) {
        BehaviorContext behaviorContext = localEndpointManager.getDefaultBehaviorContext();

        behaviorContext =
                new BehaviorContextPassthru(behaviorContext) {
                    // Use a 250ms ack timeout.
                    @Override
                    public int getCoapAckTimeoutMs() {
                        return 250;
                    }

                    // Maximum of 5 retransmits.
                    @Override
                    public int getCoapMaxRetransmit() {
                        return 5;
                    }
                };

        localEndpointManager.setDefaultBehaviorContext(behaviorContext);

        mLocalEndpointManager = localEndpointManager;

        mServer = new Server(mLocalEndpointManager);
        mServer.setRequestHandler(mRootResource);

        Resource<WellKnownCoreHandler> wellKnown = new Resource<>();
        wellKnown.addChild("core", mWellKnownCoreResource);

        mRootResource.addHiddenChild(".well-known", wellKnown);

        mRootResource.addChild("g", mGroupsResource);

        mWellKnownCoreResource.addResource("/g/", mGroupsResource);

        mLocalTechnology = new LocalTechnology(mLocalEndpointManager.getExecutor());
    }

    ScheduledExecutorService getExecutor() {
        return mLocalEndpointManager.getExecutor();
    }

    private <T> ListenableFuture<T> submit(Callable<T> callable) {
        ListenableFutureTask<T> future = ListenableFutureTask.create(callable);

        getExecutor().execute(future);

        return future;
    }

    @Override
    public void prepareToHost() throws IOException {
        mLocalTechnology.prepareToHost();

        if (mDefaultLocalEndpoint == null) {
            if (getServer().getLocalEndpoints().isEmpty()) {
                final MulticastSocket socket = new MulticastSocket(Coap.DEFAULT_PORT_NOSEC);

                mDefaultLocalEndpoint = new LocalEndpointCoap(mLocalEndpointManager, socket);
                getServer().addLocalEndpoint(mDefaultLocalEndpoint);
                socket.setLoopbackMode(true);

            } else {
                mDefaultLocalEndpoint = getServer().getLocalEndpoints().iterator().next();
            }

            Enumeration<NetworkInterface> iter = NetworkInterface.getNetworkInterfaces();
            while (iter.hasMoreElements()) {
                NetworkInterface netif = iter.nextElement();

                if (!netif.supportsMulticast()) {
                    continue;
                }

                if (!mDefaultLocalEndpoint.attemptToJoinDefaultCoapGroups(netif)) {
                    LOGGER.info("Unable to join default CoAP multicast groups on " + netif);
                }
            }
        }
    }

    @Override
    public Set<Thing> copyHostedThingSet() {
        return new HashSet<>(mHostedAdapterLookup.keySet());
    }

    @Override
    public void host(Thing fe) throws UnacceptableThingException {
        synchronized (mHostedAdapterLookup) {
            if (mHostedAdapterLookup.containsKey(fe)) {
                return;
            }

            if (fe instanceof SmcpGroup && ((SmcpGroup) fe).getTechnology() == this) {
                SmcpGroup group = (SmcpGroup) fe;
                final String groupId = group.getGroupId();

                mNestedPersistentStateManager.startManaging(GROUP_PREFIX + groupId, group);
                group.onHosted();

                // We use the LocalGroup for the adapter here because we want
                // commands received from over the network to only apply to the
                // hosted things, not native ones.
                HostedThingAdapter adapter =
                        new HostedThingAdapter(this, group.mLocalGroup) {
                            @Override
                            public void onChildMethodDelete(
                                    InboundRequest inboundRequest, InboundRequestHandler child) {
                                if (checkForUnsupportedOptions(inboundRequest)) {
                                    unhost(group);
                                    inboundRequest.success();
                                }
                            }

                            @Override
                            public void onChildMethodDeleteCheck(
                                    InboundRequest inboundRequest, InboundRequestHandler child) {
                                checkForUnsupportedOptions(inboundRequest);
                            }
                        };

                mHostedAdapterLookup.put(fe, adapter);
                mLocalTechnology.host(group.mLocalGroup);
                mGroupsResource.addChild(groupId, adapter);
                mWellKnownCoreResource.addResource("g/" + groupId, adapter);

                try {
                    mNestedPersistentStateManager.flush();
                } catch (IOException e) {
                    throw new TechnologyRuntimeException(e);
                }

            } else if (!isAssociatedWith(fe)) {
                mLocalTechnology.host(fe);

                try {
                    HostedThingAdapter adapter =
                            new HostedThingAdapter(this, fe);
                    mHostedAdapterLookup.put(fe, adapter);

                    URI uri = mLocalTechnology.getNativeUriForThing(fe);

                    String path = uri.getRawPath();

                    if (path == null) {
                        throw new TechnologyRuntimeException("Unable to host, path is invalid");
                    }

                    while (path.startsWith("/")) {
                        path = path.substring(1);
                    }

                    while (path.endsWith("/")) {
                        path = path.substring(0, path.length()-1);
                    }

                    mRootResource.addChild(path, adapter);

                    mWellKnownCoreResource.addResource(uri.toASCIIString(), adapter);

                    if (DEBUG) LOGGER.info("Hosting " + fe + " at " + uri + " (" + path + ")");
                } catch (RuntimeException x) {
                    mLocalTechnology.unhost(fe);
                    throw x;
                }

                // Go ahead and fetch to cache.
                fe.fetchSection(Section.METADATA);
            }
        }
    }

    @Override
    public void unhost(Thing fe) {
        synchronized (mHostedAdapterLookup) {
            HostedThingAdapter adapter = mHostedAdapterLookup.get(fe);

            if (adapter == null) {
                return;
            }

            mLocalTechnology.unhost(fe);

            mHostedAdapterLookup.remove(fe);
            mRootResource.removeChild(adapter);
            mWellKnownCoreResource.removeResource(adapter);

            if (fe instanceof SmcpGroup && ((SmcpGroup) fe).getTechnology() == this) {
                SmcpGroup group = (SmcpGroup) fe;
                final String groupId = group.getGroupId();

                group.onHosted();

                mNestedPersistentStateManager.stopManaging(GROUP_PREFIX + groupId);
                mNestedPersistentStateManager.reset(GROUP_PREFIX + groupId);

                group.onUnhosted();

                mGroupsResource.removeChild(adapter);

                try {
                    mNestedPersistentStateManager.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public boolean isHosted(Thing fe) {
        synchronized (mHostedAdapterLookup) {
            Thing parent;
            int parentLimit = 4;

            do {
                if (mHostedAdapterLookup.containsKey(fe)) {
                    return true;
                }

                parent = fe.getParentThing();

                if (parent == null) {
                    break;
                } else {
                    fe = parent;
                }
            } while (parentLimit-- != 0);

            return mHostedAdapterLookup.containsKey(fe);
        }
    }

    @Override
    public boolean isNative(Thing fe) {
        if (fe instanceof SmcpGroup && ((SmcpGroup) fe).getTechnology() == this) {
            return true;
        }

        synchronized (mHostedAdapterLookup) {
            Thing parent;
            int parentLimit = 4;

            do {
                if ((fe instanceof SmcpThing)
                        && mNativeThings.containsKey(
                                ((SmcpThing) fe).getUri())) {
                    return true;
                }

                parent = fe.getParentThing();

                if (parent == null) {
                    break;
                } else {
                    fe = parent;
                }
            } while (parentLimit-- != 0);

            if (fe instanceof SmcpThing) {
                return mNativeThings.containsKey(
                        ((SmcpThing) fe).getUri());
            }
        }

        return false;
    }

    @Override
    public Thing getThingForNativeUri(URI uri) throws UnknownResourceException {
        if (uri.getQuery() != null) {
            // No query parts are allowed in the thing URI.
            return null;

        } else if (uri.getScheme() == null) {
            return mLocalTechnology.getThingForNativeUri(uri);
        } else {
            Thing ret = null;
            synchronized (mNativeThings) {
                WeakReference<SmcpThing> ref = mNativeThings.get(uri);

                if (ref != null) {
                    ret = ref.get();
                }

                if (ret == null) {
                    try {
                        ret = new SmcpThing(new Client(mLocalEndpointManager, uri), this);
                    } catch (UnsupportedSchemeException e) {
                        throw new UnknownResourceException(e);
                    }
                    mNativeThings.put(
                            uri, new WeakReference<>((SmcpThing) ret));
                }
            }
            return ret;
        }
    }

    @Override
    public URI getNativeUriForThing(Thing fe) throws UnassociatedResourceException {
        if (isNative(fe)) {
            return ((SmcpThing) fe).getUri();
        }

        return mLocalTechnology.getNativeUriForThing(fe);
    }

    @Override
    public ResourceLink<Object> getResourceLinkForUri(URI uri) throws UnknownResourceException {
        ResourceLink<Object> ret = null;

        if (uri.getScheme() == null) {
            ret = mLocalTechnology.getResourceLinkForUri(uri);

        } else {
            WeakReference<ResourceLink<Object>> ref = mResourceLinkCache.get(uri);

            if (ref != null) {
                ret = ref.get();
            }

            if (ret == null && mLocalEndpointManager.supportsScheme(uri.getScheme())) {
                Client client;
                try {
                    client = new Client(mLocalEndpointManager, uri);
                } catch (UnsupportedSchemeException e) {
                    throw new UnknownResourceException(e);
                }
                ret = new SmcpResourceLink<>(client, Object.class);
                ref = new WeakReference<>(ret);
                mResourceLinkCache.put(uri, ref);
            }
        }

        if (ret == null) {
            throw new UnknownResourceException("Unsupported URI: " + uri);
        }

        return ret;
    }

    @Override
    public URI getNativeUriForProperty(Thing fe, PropertyKey<?> propertyKey, Operation op, Modifier ... modifiers) throws UnassociatedResourceException {
        StringBuilder query = new StringBuilder();

        query.append(op.id);

        if (modifiers.length != 0) {
            if (query.length() != 0) {
                query.append("&");
            }
            query.append(Modifier.convertToQuery(modifiers));
        }

        if (query.length() == 0) {
            return getNativeUriForThing(fe).resolve(propertyKey.getName());
        } else {
            return getNativeUriForThing(fe).resolve(propertyKey.getName()
                    + "?" + query);
        }
    }

    @Override
    public URI getNativeUriForSection(Thing fe, Section section, Modifier ... modifiers) throws UnassociatedResourceException {
        if (modifiers.length == 0) {
            return getNativeUriForThing(fe).resolve(section.id + "/");
        } else {
            return getNativeUriForThing(fe).resolve(section.id + "/?" + Modifier.convertToQuery(modifiers));
        }
    }

    @Override
    public SmcpDiscoveryBuilder createDiscoveryQueryBuilder() {
        return new SmcpDiscoveryBuilder(this);
    }

    @Nullable
    SmcpGroup findGroupWithId(String groupId) {
        synchronized (mGroups) {
            WeakReference<SmcpGroup> groupRef = mGroups.get(groupId);
            SmcpGroup group = null;
            if (groupRef != null) {
                group = groupRef.get();
            }
            return group;
        }
    }

    Group findOrCreateGroupWithId(String groupId) {
        synchronized (mGroups) {
            SmcpGroup group = findGroupWithId(groupId);
            if (group == null) {
                group = new SmcpGroup(this, groupId);
                mGroups.put(groupId, new WeakReference<>(group));
            }
            return group;
        }
    }

    Group internalCreateNewGroup() {
        synchronized (mGroups) {
            String groupId;

            // We have a loop here for the sake of correctness.
            do {
                groupId = Splot.generateNewUid();
            } while (mGroups.containsKey(groupId));

            return findOrCreateGroupWithId(groupId);
        }
    }

    @Override
    public ListenableFuture<Group> createNewGroup() {
        return submit(this::internalCreateNewGroup);
    }

    @Override
    public ListenableFuture<Group> fetchOrCreateGroupWithId(String groupId) {
        return submit(() -> findOrCreateGroupWithId(groupId));
    }

    @Override
    public Map<String, Object> copyPersistentState() {
        final Map<String, Object> state = mNestedPersistentStateManager.copyPersistentState();
        state.put("localTechnology", mLocalTechnology.copyPersistentState());
        return state;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void initWithPersistentState(@Nullable Map<String, Object> persistentState) {
        if (DEBUG) LOGGER.info("initWithPersistentState: " + persistentState);

        mNestedPersistentStateManager.initWithPersistentState(persistentState);

        if (persistentState != null) {
            mLocalTechnology.initWithPersistentState(
                    (Map<String, Object>) persistentState.get("localTechnology"));

            for (String key : persistentState.keySet()) {
                if (key.startsWith(GROUP_PREFIX)) {
                    // Create the hosted group.
                    try {
                        host(findOrCreateGroupWithId(key.substring(GROUP_PREFIX.length())));
                    } catch (UnacceptableThingException e) {
                        // This should never happen
                        throw new SmcpRuntimeException("Unable to host group '" + key + "'", e);
                    }
                }
            }
        } else {
            mLocalTechnology.initWithPersistentState(null);
        }
    }

    @Override
    public void setPersistentStateListener(@Nullable PersistentStateListener listener) {
        mNestedPersistentStateManager.setPersistentStateListener(listener);
        mLocalTechnology.setPersistentStateListener(listener);
    }
}
