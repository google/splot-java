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
 * <p>This class is used to obtain {@link FunctionalEndpoint} instances for functional endpoints
 * which are remotely hosted using SMCP. It can also be used for hosting other functional endpoints
 * (Local or otherwise) to allow them to be used by other devices which support SMCP or CoAP.
 */
public final class SmcpTechnology implements Technology, PersistentStateInterface {
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

    private final Map<FunctionalEndpoint, HostedFunctionalEndpointAdapter> mHostedAdapterLookup =
            new HashMap<>();
    private final Map<FunctionalEndpoint, String> mHostedPathLookup = new WeakHashMap<>();

    private final NestedPersistentStateManager mNestedPersistentStateManager =
            new NestedPersistentStateManager();

    private final Map<URI, WeakReference<SmcpFunctionalEndpoint>> mNativeFunctionalEndpoints =
            Collections.synchronizedMap(new WeakHashMap<>());

    final LocalTechnology mLocalTechnology;

    private final Resource<InboundRequestHandler> mRootResource = new Resource<>();

    private final GroupsResource mGroupsResource = new GroupsResource(this);

    /** Gets the CoAP {@link Server} object used to host functional endpoints. */
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
        if (mDefaultLocalEndpoint == null) {
            final MulticastSocket socket = new MulticastSocket(Coap.DEFAULT_PORT_NOSEC);

            mDefaultLocalEndpoint = new LocalEndpointCoap(mLocalEndpointManager, socket);
            getServer().addLocalEndpoint(mDefaultLocalEndpoint);
            socket.setLoopbackMode(true);

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

        mLocalTechnology.prepareToHost();
    }

    @Override
    public Set<FunctionalEndpoint> copyHostedFunctionalEndpointSet() {
        return new HashSet<>(mHostedAdapterLookup.keySet());
    }

    @Override
    public void host(FunctionalEndpoint fe) throws UnacceptableFunctionalEndpointException {
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
                // hosted functional endpoints, not native ones.
                HostedFunctionalEndpointAdapter adapter =
                        new HostedFunctionalEndpointAdapter(this, group.mLocalGroup) {
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
                mGroupsResource.addChild(groupId, adapter);

                mHostedPathLookup.put(fe, "g/" + groupId);
                mWellKnownCoreResource.addResource("/g/" + groupId, adapter);

                try {
                    mNestedPersistentStateManager.flush();
                } catch (IOException e) {
                    throw new TechnologyRuntimeException(e);
                }

            } else if (!isAssociatedWith(fe)) {
                mLocalTechnology.host(fe);

                HostedFunctionalEndpointAdapter adapter =
                        new HostedFunctionalEndpointAdapter(this, fe);
                int index;
                for (index = 1; index < 10000; index++) {
                    if (!mHostedPathLookup.containsValue(Integer.toString(index))) {
                        break;
                    }
                }

                String path = Integer.toString(index);
                mHostedAdapterLookup.put(fe, adapter);
                mHostedPathLookup.put(fe, path);
                mRootResource.addChild(path, adapter);
                mWellKnownCoreResource.addResource("/" + path, adapter);

                fe.fetchMetadata();
            }
        }
    }

    @Override
    public void unhost(FunctionalEndpoint fe) {
        synchronized (mHostedAdapterLookup) {
            HostedFunctionalEndpointAdapter adapter = mHostedAdapterLookup.get(fe);

            if (adapter == null) {
                return;
            }

            mLocalTechnology.unhost(fe);

            mHostedAdapterLookup.remove(fe);
            mHostedPathLookup.remove(fe);
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
    public boolean isHosted(FunctionalEndpoint fe) {
        synchronized (mHostedAdapterLookup) {
            FunctionalEndpoint parent;
            int parentLimit = 4;

            do {
                if (mHostedAdapterLookup.containsKey(fe)) {
                    return true;
                }

                parent = fe.getParentFunctionalEndpoint();

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
    public boolean isNative(FunctionalEndpoint fe) {
        if (fe instanceof SmcpGroup && ((SmcpGroup) fe).getTechnology() == this) {
            return true;
        }

        synchronized (mHostedAdapterLookup) {
            FunctionalEndpoint parent;
            int parentLimit = 4;

            do {
                if ((fe instanceof SmcpFunctionalEndpoint)
                        && mNativeFunctionalEndpoints.containsKey(
                                ((SmcpFunctionalEndpoint) fe).getUri())) {
                    return true;
                }

                parent = fe.getParentFunctionalEndpoint();

                if (parent == null) {
                    break;
                } else {
                    fe = parent;
                }
            } while (parentLimit-- != 0);

            if (fe instanceof SmcpFunctionalEndpoint) {
                return mNativeFunctionalEndpoints.containsKey(
                        ((SmcpFunctionalEndpoint) fe).getUri());
            }
        }

        return false;
    }

    @Override
    public FunctionalEndpoint getFunctionalEndpointForNativeUri(URI uri) {
        if (uri.getQuery() != null) {
            // No query parts are allowed in the functional endpoint URI.
            return null;

        } else if (uri.getScheme() == null) {
            String uriString = uri.getRawPath();

            for (Map.Entry<FunctionalEndpoint, String> entry : mHostedPathLookup.entrySet()) {
                String prefix = "/" + entry.getValue() + "/";
                if (uriString.startsWith(prefix)) {
                    if (uriString.equals(prefix)) {
                        // This is the URI for this functional endpoint.
                        return entry.getKey();
                    }

                    // This is a child. Look up it.
                    uriString = uriString.substring(prefix.length());
                    String[] parts = uriString.split("/", -1);
                    int i = 0;
                    FunctionalEndpoint ret = entry.getKey();

                    while (ret != null && (i < parts.length - 2)) {
                        if (!parts[i++].equals("f")) {
                            // Bad child path.
                            return null;
                        }
                        String traitShortName = parts[i++];
                        String childId = parts[i++];

                        ret = ret.getChild(traitShortName, childId);
                    }

                    if (i == parts.length || ((i == parts.length - 1) && parts[i].isEmpty())) {
                        return ret;
                    }

                    return null;
                }
            }
            return null;
        } else {
            FunctionalEndpoint ret = null;
            synchronized (mNativeFunctionalEndpoints) {
                WeakReference<SmcpFunctionalEndpoint> ref = mNativeFunctionalEndpoints.get(uri);

                if (ref != null) {
                    ret = ref.get();
                }

                if (ret == null) {
                    ret = new SmcpFunctionalEndpoint(new Client(mLocalEndpointManager, uri), this);
                    mNativeFunctionalEndpoints.put(
                            uri, new WeakReference<>((SmcpFunctionalEndpoint) ret));
                }
            }
            return ret;
        }
    }

    @Override
    public URI getNativeUriForFunctionalEndpoint(FunctionalEndpoint fe) {
        if (isNative(fe)) {
            return ((SmcpFunctionalEndpoint) fe).getUri();
        }

        FunctionalEndpoint parentFe = fe.getParentFunctionalEndpoint();

        if (parentFe != null) {
            URI parentUri = getNativeUriForFunctionalEndpoint(parentFe);
            if (parentUri != null) {
                return parentUri.resolve(
                        MethodKey.SECTION_FUNC
                                + "/"
                                + parentFe.getTraitForChild(fe)
                                + "/"
                                + parentFe.getIdForChild(fe)
                                + "/");
            }
        } else if (isHosted(fe)) {
            InboundRequestHandler rh = mHostedAdapterLookup.get(fe);
            if (rh != null) {
                return URI.create("/" + mRootResource.getNameForChild(rh) + "/");
            }
        }

        return null;
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
                groupId = FunctionalEndpoint.generateNewUid();
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
                    } catch (UnacceptableFunctionalEndpointException e) {
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
