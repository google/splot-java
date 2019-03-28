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
package com.google.iot.m2m.local;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;
import com.google.iot.m2m.base.*;
import com.google.iot.m2m.trait.BaseTrait;
import com.google.iot.m2m.util.NestedPersistentStateManager;
import java.lang.ref.WeakReference;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import static com.google.iot.m2m.base.Splot.*;

/**
 * A {@link Technology} class that allows for dumb, non-multicast-optimized groups and (eventually)
 * automation pairings/rules.
 *
 * <p>This class is used for cases where group and automation functionality is desired but not
 * available from the native technology. It is also used heavily for unit tests.
 *
 * <p>Note that any group created by calling {@link #createNewGroup()} will need to be hosted (using
 * {@link #host}) before it can be used.
 *
 * @see FunctionalEndpoint
 * @see Group
 */
public final class LocalTechnology implements Technology, PersistentStateInterface {
    private static final boolean DEBUG = false;
    private static final Logger LOGGER = Logger.getLogger(LocalTechnology.class.getCanonicalName());

    private static final String GROUP_PREFIX = "group-";
    public static final int MAX_CHILD_DEPTH = 5;

    private final Executor mExecutor;
    private final NestedPersistentStateManager mNestedPersistentStateManager =
            new NestedPersistentStateManager();

    private final Map<FunctionalEndpoint, String> mHostedPathLookup = new WeakHashMap<>();
    private final Map<String, WeakReference<LocalGroup>> mGroups = new WeakHashMap<>();

    private final WeakHashMap<ResourceLink<?>, URI> mResourceLinkUriLookup = new WeakHashMap<>();

    public LocalTechnology(Executor executor) {
        mExecutor = executor;
    }

    public LocalTechnology() {
        this(Utils.getDefaultExecutor());
    }

    Executor getExecutor() {
        return mExecutor;
    }

    private <T> ListenableFuture<T> submit(Callable<T> callable) {
        ListenableFutureTask<T> future = ListenableFutureTask.create(callable);

        getExecutor().execute(future);

        return future;
    }

    @Override
    public Set<FunctionalEndpoint> copyHostedFunctionalEndpointSet() {
        return new HashSet<>(mHostedPathLookup.keySet());
    }

    @Override
    public Collection<Group> copyHostedGroups() {
        Collection<Group> ret = new LinkedList<>();
        synchronized (mHostedPathLookup) {
            mHostedPathLookup.keySet().forEach(
                    g -> {
                        if ((g instanceof Group) && isNative(g)) {
                            ret.add((Group) g);
                        }
                    });
        }
        return ret;
    }

    @Override
    public void prepareToHost() {}

    @Override
    public void host(FunctionalEndpoint fe) throws UnacceptableFunctionalEndpointException {
        synchronized (mHostedPathLookup) {
            if (!isHosted(fe)) {
                if (fe instanceof LocalGroup && ((LocalGroup) fe).getTechnology() == this) {
                    LocalGroup group = (LocalGroup) fe;
                    final String groupId = group.getGroupId();
                    mNestedPersistentStateManager.startManaging(GROUP_PREFIX + groupId, group);
                    mHostedPathLookup.put(fe, "g/" + groupId);

                } else {
                    if (!isAssociatedWith(fe)) {
                        int index;
                        for (index = 1; index < 10000; index++) {
                            if (!mHostedPathLookup.containsValue(Integer.toString(index))) {
                                break;
                            }
                        }
                        mHostedPathLookup.put(fe, Integer.toString(index));

                        fe.fetchMetadata();

                        synchronized (mGroups) {
                            mGroups.values()
                                    .forEach(
                                            (groupRef) -> {
                                                LocalGroup group = groupRef.get();
                                                if (group != null)
                                                    group.checkIfWantsFunctionalEndpoint(fe);
                                            });
                        }
                    }
                }
            }
        }
        if (!isHosted(fe)) {
            throw new UnacceptableFunctionalEndpointException();
        }
    }

    @Override
    public void unhost(FunctionalEndpoint fe) {
        synchronized (mHostedPathLookup) {
            if (!isHosted(fe)) {
                return;
            }
            if (fe instanceof LocalGroup && ((LocalGroup) fe).getTechnology() == this) {
                LocalGroup group = (LocalGroup) fe;
                final String groupId = group.getGroupId();
                mNestedPersistentStateManager.stopManaging(GROUP_PREFIX + groupId);
                mNestedPersistentStateManager.reset(GROUP_PREFIX + groupId);
                group.clearAllHostedMembers();
            } else {
                synchronized (mGroups) {
                    mGroups.values()
                            .forEach(
                                    (groupRef) -> {
                                        LocalGroup group = groupRef.get();
                                        if (group != null) group.removeMember(fe);
                                    });
                }
            }

            mHostedPathLookup.remove(fe);
        }
    }

    @Override
    public boolean isHosted(FunctionalEndpoint fe) {
        synchronized (mHostedPathLookup) {
            FunctionalEndpoint parent;
            int parentLimit = 4;

            do {
                if (mHostedPathLookup.keySet().contains(fe)) {
                    return true;
                }

                parent = fe.getParentFunctionalEndpoint();

                if (parent == null) {
                    break;
                } else {
                    fe = parent;
                }
            } while (parentLimit-- != 0);

            return mHostedPathLookup.keySet().contains(fe);
        }
    }

    @Override
    public boolean isNative(FunctionalEndpoint fe) {
        FunctionalEndpoint parent;
        int parentLimit = 4;

        do {
            if (((fe instanceof Group) && ((Group) fe).getTechnology() == this)) {
                return true;
            }

            parent = fe.getParentFunctionalEndpoint();

            if (parent == null) {
                break;
            } else {
                fe = parent;
            }
        } while (parentLimit-- != 0);

        return ((fe instanceof Group) && ((Group) fe).getTechnology() == this);
    }

    private class FEParser {
        String[] mComponents = null;
        int mComponentIndex = 0;
        FunctionalEndpoint mFe = null;

        FEParser(URI uri) throws UnknownResourceException {
            if (uri.getScheme() != null) {
                throw new UnknownResourceException("Unsupported scheme \"" + uri.getScheme() + "\"");
            }

            String path = uri.getRawPath();

            if (path == null || path.isEmpty()) {
                throw new UnknownResourceException("Empty path");
            }

            if (path.charAt(0) != '/') {
                throw new UnknownResourceException("Path must be absolute (start with '/')");
            }

            mComponents = path.substring(1).split("/",0);

            if (mComponents.length == 0) {
                throw new UnknownResourceException("Empty path");
            }

            path = mComponents[0];

            if (Splot.GROUP_RESOURCE.equals(path)) {
                if (mComponents.length == 1) {
                    throw new UnknownResourceException("Missing group name");
                }
                path = path + "/" + mComponents[1];
                mComponentIndex = 1;
            }

            synchronized (mHostedPathLookup) {
                for (Map.Entry<FunctionalEndpoint,String> entry : mHostedPathLookup.entrySet()) {
                    if (path.equals(entry.getValue())) {
                        mFe = entry.getKey();
                    }
                }
            }

            if (mFe == null) {
                if (DEBUG) LOGGER.info("FE lookup miss, looking for \"" + path + "\" in " + mHostedPathLookup.values());
                throw new UnknownResourceException("Unable to find FE for " + uri);
            }

            // Find the child
            while ((remainingComponents() > 3) && Splot.SECTION_FUNC.equals(getRelativeComponent(1))) {
                String traitName = getRelativeComponent(2);
                String childId = getRelativeComponent(3);

                mFe = mFe.getChild(traitName, childId);

                if (mFe == null) {
                    if (DEBUG) LOGGER.info("FE child lookup miss for \"" + path + "\"");
                    throw new UnknownResourceException("Unable to find child FE for " + uri);
                }

                mComponentIndex += 3;
            }
        }

        String getRelativeComponent(int i) {
            return mComponents[mComponentIndex + i];
        }

        int remainingComponents() {
            return mComponents.length - mComponentIndex;
        }
    }

    public ResourceLink<Object> getResourceLinkForNativeUri(URI uri) throws UnknownResourceException {
        final FEParser parser = new FEParser(uri);

        if (parser.remainingComponents() == 4) {
            final String section = parser.getRelativeComponent(1);
            final String trait = parser.getRelativeComponent(2);
            final String name = parser.getRelativeComponent(3);

            if (uri.getQuery() == null) {
                PropertyKey<Object> key = new PropertyKey<>(section,trait,name,Object.class);
                ResourceLink<Object> ret = PropertyResourceLink.create(parser.mFe, key);
                mResourceLinkUriLookup.put(ret, uri);
                return ret;
            }

            String[] queryComponents = uri.getQuery().split("&");

            if (queryComponents.length != 1) {
                // More than one query component isn't supported.
                throw new UnknownResourceException("More than one query component isn't supported.");
            }

            ResourceLink<Object> ret;

            switch(queryComponents[0]) {
                case PROP_METHOD_INCREMENT: {
                    PropertyKey<Double> key = new PropertyKey<>(section, trait, name,
                            Double.class);
                    ret = ResourceLink.stripType(
                            PropertyResourceLink.createIncrement(parser.mFe, key), Double.class);
                    break;
                }

                case PROP_METHOD_TOGGLE: {
                    PropertyKey<Boolean> key = new PropertyKey<>(section, trait, name,
                            Boolean.class);
                    ret = ResourceLink.stripType(
                            PropertyResourceLink.createToggle(parser.mFe, key), Boolean.class);
                    break;
                }

                case PROP_METHOD_INSERT: {
                    PropertyKey<Object[]> key = new PropertyKey<>(section, trait, name,
                            Object[].class);
                    ret = PropertyResourceLink.createInsert(parser.mFe, key);
                    break;
                }

                case PROP_METHOD_REMOVE: {
                    PropertyKey<Object[]> key = new PropertyKey<>(section, trait, name,
                            Object[].class);
                    ret = PropertyResourceLink.createRemove(parser.mFe, key);
                    break;
                }

                default:
                    // Other operations aren't supported.
                    throw new UnknownResourceException("query component \"" + queryComponents[0] + "\" isn't supported.");
            }

            mResourceLinkUriLookup.put(ret, uri);
            return ret;

        }

        // Resource link tracking a trait in a section
        if (parser.remainingComponents() == 3) {
            // We don't support resource links for traits yet.
            return null;
        }

        // Resource link tracking a section
        if (parser.remainingComponents() == 2) {
            final String section = parser.getRelativeComponent(1);

            @SuppressWarnings("unchecked")
            ResourceLink<Object> ret = ResourceLink.stripType(
                    (ResourceLink) SectionResourceLink.createForSection(parser.mFe, section),
                    Map.class);

            mResourceLinkUriLookup.put(ret, uri);

            return ret;
        }

        return null;
    }

    @Override
    public URI getNativeUriForProperty(FunctionalEndpoint fe, PropertyKey<?> propertyKey) throws UnassociatedResourceException {
        return getNativeUriForFunctionalEndpoint(fe).resolve(propertyKey.getName());
    }

    @Override
    public URI getNativeUriForSection(FunctionalEndpoint fe, String section) throws UnassociatedResourceException {
        return getNativeUriForFunctionalEndpoint(fe).resolve(section + "/");
    }

    @Override
    public URI getNativeUriForResourceLink(ResourceLink<Object> rl) throws UnassociatedResourceException {
        URI ret = mResourceLinkUriLookup.get(rl);
        if (ret == null) {
            throw new UnassociatedResourceException("Unable to lookup URI for " + rl);
        }
        return ret;
    }

    @Override
    public FunctionalEndpoint getFunctionalEndpointForNativeUri(URI uri) throws UnknownResourceException {
        return new FEParser(uri).mFe;
    }

    @Override
    @NonNull
    public URI getNativeUriForFunctionalEndpoint(FunctionalEndpoint fe) throws UnassociatedResourceException {
        if (!isAssociatedWith(fe)) {
            throw new UnassociatedResourceException("Unable to lookup URI for " + fe);
        }

        String path = "/";
        int remaining_depth = MAX_CHILD_DEPTH;

        FunctionalEndpoint parent = fe.getParentFunctionalEndpoint();

        while (parent != null) {
            String trait = parent.getTraitForChild(fe);
            String childId = parent.getIdForChild(fe);
            path = "/f/" + trait + "/" + childId + path;
            fe = parent;
            parent = fe.getParentFunctionalEndpoint();

            if (--remaining_depth == 0) {
                throw new UnassociatedResourceException("Unable to lookup URI for " + fe);
            }
        }

        String hostedPath;

        synchronized (mHostedPathLookup) {
            hostedPath = mHostedPathLookup.get(fe);
        }

        if (hostedPath == null) {
            throw new UnassociatedResourceException("Unable to lookup URI for " + fe);
        }

        if (DEBUG)
            LOGGER.info("LocalTechnology: getNativeUriForFunctionalEndpoint(" + fe
                    + ") = " + hostedPath + path);

        try {
            return new URI(null, null, "/" + hostedPath + path, null, null);

        } catch (URISyntaxException e) {
            // Shouldn't happen.
            throw new TechnologyRuntimeException(e);
        }
    }

    @Nullable
    FunctionalEndpoint getHostedFunctionalEndpointForUid(String uid) {
        synchronized (mHostedPathLookup) {
            for (FunctionalEndpoint fe : mHostedPathLookup.keySet()) {
                if (uid.equals(fe.getCachedProperty(BaseTrait.META_UID))) {
                    return fe;
                }
            }
        }
        return null;
    }

    class DiscoveryQuery extends com.google.iot.m2m.base.DiscoveryQuery {
        private final Set<String> mRequiredTraits;
        private final String mRequiredUid;
        private final int mMaxResults;
        private final HashSet<FunctionalEndpoint> mFunctionalEndpoints = new HashSet<>();
        private final boolean mIncludeGroups;
        private final boolean mIncludeNormal;
        private Listener mListener = null;

        // This is being set to Runnable::run here just to give it a
        // non-null value to suppress warnings. The value here will
        // be changed by setListener() before the first time it is used.
        private Executor mListenerExecutor = Runnable::run;

        DiscoveryQuery(
                @Nullable Set<String> requiredTraits,
                @Nullable String requiredUid,
                int maxResults,
                boolean includeGroups,
                boolean includeNormal) {
            mRequiredTraits = requiredTraits;
            mRequiredUid = requiredUid;
            mMaxResults = maxResults;
            mIncludeGroups = includeGroups;
            mIncludeNormal = includeNormal;
            restart();
        }

        @Override
        public Set<FunctionalEndpoint> get() {
            synchronized (mFunctionalEndpoints) {
                return new HashSet<>(mFunctionalEndpoints);
            }
        }

        @Override
        public void restart() {
            synchronized (mFunctionalEndpoints) {
                mFunctionalEndpoints.clear();

                Collection<FunctionalEndpoint> collection;

                if (mRequiredUid != null) {
                    collection = new ArrayList<>();
                    FunctionalEndpoint fe = getHostedFunctionalEndpointForUid(mRequiredUid);
                    if (fe != null) {
                        collection.add(fe);
                    }
                } else {
                    collection = copyHostedFunctionalEndpointSet();
                }

                if (mRequiredTraits == null || mRequiredTraits.isEmpty()) {
                    mFunctionalEndpoints.addAll(collection);
                    return;
                }

                for (FunctionalEndpoint fe : collection) {
                    if (mFunctionalEndpoints.size() >= mMaxResults) {
                        break;
                    }
                    boolean isGroup =
                            (fe instanceof LocalGroup
                                    && ((LocalGroup) fe).getTechnology() == LocalTechnology.this);

                    if (isGroup && !mIncludeGroups) {
                        continue;
                    }

                    if (!isGroup && mIncludeNormal) {
                        continue;
                    }

                    boolean missingTrait = false;
                    for (String trait : mRequiredTraits) {
                        String traitUri =
                                fe.getCachedProperty(
                                        new PropertyKey<>(
                                                Splot.SECTION_METADATA
                                                        + "/"
                                                        + trait
                                                        + "/turi",
                                                String.class));
                        if (traitUri == null) {
                            missingTrait = true;
                            break;
                        }
                    }
                    if (!missingTrait) {
                        mFunctionalEndpoints.add(fe);
                    }
                }
            }

            done();
        }

        @Override
        public void stop() {
            /* Nothing to do */
        }

        private void done() {
            if (mListener != null) {
                mListenerExecutor.execute(mListener::onDiscoveryQueryIsDone);
            }
        }

        @Override
        public boolean isDone() {
            /* We are always done */
            return true;
        }

        @Override
        public void setListener(Executor executor, @Nullable Listener listener) {
            mListenerExecutor = executor;
            mListener = listener;
            done();
        }
    }

    class DiscoveryBuilder extends com.google.iot.m2m.base.DiscoveryBuilder {

        final Set<String> mRequiredTraits = new HashSet<>();
        String mRequiredUid = null;
        int mMaxResults = Integer.MAX_VALUE;
        boolean mIncludeGroups = true;
        boolean mIncludeNormal = true;

        DiscoveryBuilder() {}

        @Override
        public com.google.iot.m2m.base.DiscoveryBuilder includeHosted(boolean includeHosted) {
            /* This method does nothing on this technology */
            return this;
        }

        @Override
        public com.google.iot.m2m.base.DiscoveryBuilder mustBeGroup() {
            mIncludeGroups = true;
            mIncludeNormal = false;
            return this;
        }

        @Override
        public com.google.iot.m2m.base.DiscoveryBuilder mustNotBeGroup() {
            mIncludeGroups = false;
            mIncludeNormal = true;
            return this;
        }

        @Override
        public com.google.iot.m2m.base.DiscoveryBuilder setTimeout(long timeout, TimeUnit units) {
            /* This method does nothing on this technology */
            return this;
        }

        @Override
        public com.google.iot.m2m.base.DiscoveryBuilder mustHaveTrait(String traitShortName) {
            mRequiredTraits.add(traitShortName);
            return this;
        }

        @Override
        public com.google.iot.m2m.base.DiscoveryBuilder mustHaveUid(String uid) {
            if (mRequiredUid != null) {
                throw new IllegalStateException("Already set mustHaveUid");
            }
            mRequiredUid = uid;
            return this;
        }

        @Override
        public com.google.iot.m2m.base.DiscoveryBuilder setMaxResults(int count) {
            if (count <= 0) {
                mMaxResults = Integer.MAX_VALUE;
            } else {
                mMaxResults = count;
            }
            return this;
        }

        @Override
        public com.google.iot.m2m.base.DiscoveryQuery buildAndRun() {
            return new DiscoveryQuery(
                    mRequiredTraits, mRequiredUid, mMaxResults, mIncludeGroups, mIncludeNormal);
        }
    }

    @Override
    public com.google.iot.m2m.base.DiscoveryBuilder createDiscoveryQueryBuilder() {
        return new DiscoveryBuilder();
    }

    @Override
    public ListenableFuture<Group> createNewGroup() {
        return submit(
                () -> {
                    synchronized (mGroups) {
                        String groupId;

                        // We have a loop here for the sake of correctness.
                        do {
                            groupId = FunctionalEndpoint.generateNewUid();
                        } while (mGroups.containsKey(groupId));

                        return findOrCreateGroupWithId(groupId);
                    }
                });
    }

    public Group findOrCreateGroupWithId(String groupId) {
        synchronized (mGroups) {
            WeakReference<LocalGroup> groupRef = mGroups.get(groupId);
            LocalGroup group = null;
            if (groupRef != null) {
                group = groupRef.get();
            }
            if (group == null) {
                group = new LocalGroup(this, groupId);
                mGroups.put(groupId, new WeakReference<>(group));
            }
            return group;
        }
    }

    @Override
    public ListenableFuture<Group> fetchOrCreateGroupWithId(String groupId) {
        return submit(() -> findOrCreateGroupWithId(groupId));
    }

    @Override
    public Map<String, Object> copyPersistentState() {
        return mNestedPersistentStateManager.copyPersistentState();
    }

    @Override
    public void initWithPersistentState(@Nullable Map<String, Object> persistentState) {
        mNestedPersistentStateManager.initWithPersistentState(persistentState);

        if (persistentState != null) {
            for (String key : persistentState.keySet()) {
                if (key.startsWith(GROUP_PREFIX)) {
                    // Create the group.
                    try {
                        host(findOrCreateGroupWithId(key.substring(GROUP_PREFIX.length())));
                    } catch (UnacceptableFunctionalEndpointException e) {
                        // This should never happen
                        throw new TechnologyRuntimeException(
                                "Unable to host group '" + key + "'", e);
                    }
                }
            }
        }
    }

    @Override
    public void setPersistentStateListener(@Nullable PersistentStateListener listener) {
        mNestedPersistentStateManager.setPersistentStateListener(listener);
    }
}
