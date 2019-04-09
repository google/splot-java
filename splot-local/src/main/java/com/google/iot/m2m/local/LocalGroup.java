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

import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.iot.m2m.base.*;
import com.google.iot.m2m.trait.BaseTrait;
import com.google.iot.m2m.trait.GroupTrait;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Package-private Group implementation for {@link LocalTechnology}. This implementation is fairly
 * dumb because it can't do things like multicast. However, it is still useful in cases where you
 * need group functionality with functional endpoints native to technologies that don't support
 * groups.
 */
final class LocalGroup extends LocalFunctionalEndpoint implements Group {
    private static final boolean DEBUG = false;
    private static final Logger LOGGER = Logger.getLogger(LocalGroup.class.getCanonicalName());

    private final Set<FunctionalEndpoint> mMembers = new HashSet<>();
    private final Set<String> mUnhostedMemberUids = new HashSet<>();
    private final LocalTechnology mTechnology;
    private final String mGroupId;

    private boolean mHasInitialized = false;

    @SuppressWarnings("FieldCanBeLocal")
    private final BaseTrait.AbstractLocalTrait mBaseTrait =
            new BaseTrait.AbstractLocalTrait() {
                @Override
                public String onGetUid() {
                    return mGroupId;
                }

                @Override
                public void onSetUid(@Nullable String uid) throws PropertyReadOnlyException {
                    throw new PropertyReadOnlyException();
                }

                @Override
                public String onGetNameDefault() {
                    return "Unknown Group";
                }

                @Override
                public boolean onCanSaveProperty(PropertyKey<?> key) {
                    if (BaseTrait.META_UID.equals(key)) {
                        return false;
                    }
                    return super.onCanSaveProperty(key);
                }
            };

    private final GroupTrait.AbstractLocalTrait mGroupTrait =
            new GroupTrait.AbstractLocalTrait() {
                @Override
                @NonNull
                @SuppressWarnings("RedundantThrows")
                public String[] onGetLocalMembers() throws TechnologyException {
                    synchronized (mMembers) {
                        synchronized (mUnhostedMemberUids) {
                            String[] ret = new String[mUnhostedMemberUids.size() + mMembers.size()];
                            int i = 0;

                            for (String uid : mUnhostedMemberUids) {
                                ret[i++] = uid;
                            }

                            for (FunctionalEndpoint fe : mMembers) {
                                ret[i++] = fe.getCachedProperty(BaseTrait.META_UID);
                            }

                            if (DEBUG) LOGGER.info("getLocalMembers: " + Sets.newHashSet(ret));
                            return ret;
                        }
                    }
                }

                @Override
                public void onSetLocalMembers(@Nullable String[] value)
                        throws InvalidPropertyValueException, TechnologyException {
                    if (value == null) {
                        value = new String[0];
                    }

                    final Set<String> prev = Sets.newHashSet(onGetLocalMembers());
                    final Set<String> curr = Sets.newHashSet(value);

                    final Set<String> toRemove = Sets.difference(prev, curr);
                    final Set<String> toAdd =
                            Sets.newHashSet(Sets.difference(curr, prev).iterator());

                    if (DEBUG) {
                        LOGGER.info("setLocalMembers: value = " + Sets.newHashSet(value));
                        LOGGER.info("setLocalMembers: toRemove = " + toRemove);
                        LOGGER.info("setLocalMembers: toAdd = " + toAdd);
                    }

                    if (toRemove.isEmpty() && toAdd.isEmpty()) {
                        // No changes.
                        return;
                    }

                    List<FunctionalEndpoint> membersToAdd = new LinkedList<>();

                    for (String uid : new ArrayList<>(toAdd)) {
                        FunctionalEndpoint fe = mTechnology.getHostedFunctionalEndpointForUid(uid);

                        if (fe != null) {
                            if (fe instanceof LocalGroup
                                    && ((LocalGroup) fe).getTechnology() == mTechnology) {
                                throw new InvalidPropertyValueException(
                                        new UnacceptableFunctionalEndpointException(
                                                "Cannot add a native group \""
                                                        + uid
                                                        + "\"as a member of a native group"));
                            }

                            if (DEBUG) LOGGER.info("setLocalMembers: ADDING MEMBER " + uid);

                            membersToAdd.add(fe);
                            toAdd.remove(uid);

                        } else if (mHasInitialized) {
                            LOGGER.warning("setLocalMembers: Attempted to add unhosted member");
                            throw new InvalidPropertyValueException("Unknown FE \"" + uid + "\"");

                        } else {
                            if (DEBUG) LOGGER.info("setLocalMembers: ADDING FUTURE MEMBER " + uid);
                        }
                    }

                    synchronized (mMembers) {
                        synchronized (mUnhostedMemberUids) {
                            mMembers.removeIf(
                                    fe ->
                                            toRemove.contains(
                                                    fe.getCachedProperty(BaseTrait.META_UID)));
                            mMembers.addAll(membersToAdd);
                            mUnhostedMemberUids.removeAll(toRemove);
                            mUnhostedMemberUids.addAll(toAdd);
                        }
                    }

                    mGroupTrait.didChangeLocalMembers(onGetLocalMembers());

                    changedPersistentStateBlocking();
                }
            };

    LocalGroup(LocalTechnology technology, String groupId) {
        mTechnology = technology;
        mGroupId = groupId;
        registerTrait(mBaseTrait);
        registerTrait(mGroupTrait);
    }

    @Override
    protected Executor getExecutor() {
        return mTechnology.getExecutor();
    }

    private <T> ListenableFuture<T> singleValueFromFutureList(
            Iterable<? extends ListenableFuture<? extends T>> futures) {

        ListenableFuture<List<T>> listFuture =
                com.google.common.util.concurrent.Futures.successfulAsList(futures);

        ListenableFutureTask<T> taskFuture =
                ListenableFutureTask.create(
                        () -> {
                            if (listFuture.isCancelled()) {
                                return null;
                            }

                            final List<T> list;

                            list = Futures.getChecked(listFuture, TechnologyException.class);

                            for (T ret : list) {
                                if (ret == null) {
                                    continue;
                                }
                                return ret;
                            }

                            return null;
                        });

        listFuture.addListener(taskFuture, getExecutor());

        taskFuture.addListener(
                () -> {
                    if (taskFuture.isCancelled()) {
                        listFuture.cancel(true);
                    }
                },
                getExecutor());

        return taskFuture;
    }

    void clearAllHostedMembers() {
        mMembers.clear();
        mUnhostedMemberUids.clear();
        try {
            mGroupTrait.didChangeLocalMembers(mGroupTrait.onGetLocalMembers());

        } catch (TechnologyException x) {
            // Note: this is guaranteed not to be thrown.
            throw new AssertionError(x);
        }
    }

    @Override
    public String getGroupId() {
        return mGroupId;
    }

    @Override
    public Technology getTechnology() {
        return mTechnology;
    }

    @Override
    public boolean hasLocalMembers() {
        return !mMembers.isEmpty();
    }

    @Override
    public boolean canAdministerGroup() {
        return true;
    }

    @Override
    public boolean canUseGroup() {
        return true;
    }

    @Override
    public boolean isReliable() {
        return true;
    }

    @Override
    public ListenableFuture<Set<FunctionalEndpoint>> fetchMembers() {
        return submit(
                () -> {
                    synchronized (mMembers) {
                        return new HashSet<>(mMembers);
                    }
                });
    }

    @Override
    @CanIgnoreReturnValue
    public ListenableFuture<Void> addMember(FunctionalEndpoint fe) {
        if (fe instanceof LocalGroup && ((LocalGroup) fe).getTechnology() == mTechnology) {
            return Futures.immediateFailedFuture(
                    new UnacceptableFunctionalEndpointException(
                            "Cannot add a native group as a member"));
        }

        if (mTechnology != null) {
            if (!mTechnology.isAssociatedWith(fe)) {
                return Futures.immediateFailedFuture(
                        new UnacceptableFunctionalEndpointException(
                                "Functional endpoint is not yet associated with this technology"));
            }

            if (!mTechnology.isHosted(this)) {
                return Futures.immediateFailedFuture(
                        new UnacceptableFunctionalEndpointException(
                                "This group must itself be hosted before adding hosted functional endpoints"));
            }
        }

        return submit(
                () -> {
                    synchronized (mMembers) {
                        mMembers.add(fe);
                    }
                    String uid = fe.getCachedProperty(BaseTrait.META_UID);
                    if (uid == null) {
                        // The UID isn't cached, so we go ahead and fetch it here
                        // so that it will be available when we need it.
                        fe.fetchProperty(BaseTrait.META_UID);

                    } else {
                        synchronized (mUnhostedMemberUids) {
                            mUnhostedMemberUids.remove(uid);
                        }
                    }
                    changedPersistentStateBlocking();
                    return null;
                });
    }

    @Override
    @CanIgnoreReturnValue
    public ListenableFuture<Void> removeMember(FunctionalEndpoint fe) {
        return submit(
                () -> {
                    synchronized (mMembers) {
                        mMembers.remove(fe);
                    }
                    changedPersistentStateBlocking();
                    return null;
                });
    }

    @Override
    public ListenableFuture<Map<String, Object>> fetchSection(Section section, Modifier... mods) {
        if (!Section.STATE.equals(section)) {
            // Treat every section other than STATE normally.
            return super.fetchSection(section, mods);
        }

        LinkedList<ListenableFuture<Map<String, Object>>> futures = new LinkedList<>();

        synchronized (mMembers) {
            mMembers.forEach((fe) -> futures.add(fe.fetchSection(section, mods)));
        }

        Futures.FutureCombiner<Map<String, Object>> combiner;
        combiner = Futures.whenAllComplete(futures);

        return combiner.call(
                () -> {
                    Map<String, Object> ret = new HashMap<>();

                    for (ListenableFuture<Map<String, Object>> futureState : futures) {
                        if (futureState.isCancelled()) {
                            continue;
                        }

                        Map<String, Object> state;

                        try {
                            state = Futures.getChecked(futureState, ExecutionException.class);

                        } catch (ExecutionException ignored) {
                            // Groups are best-effort, so we don't deal with
                            // checked exceptions here, if there is a persistent
                            // issue it will crop up in other interactions where
                            // they can be better handled.
                            continue;
                        }

                        for (Map.Entry<String, Object> entry : state.entrySet()) {
                            if (!ret.containsKey(entry.getKey())) {
                                // If the value wasn't present before, add it.
                                ret.put(entry.getKey(), entry.getValue());
                            }

                            if (!Objects.equals(ret.get(entry.getKey()), entry.getValue())) {
                                // If the values differ, we switch to 'null'.
                                ret.put(entry.getKey(), null);
                            }
                        }
                    }
                    return ret;
                },
                getExecutor());
    }

    @Override
    public <T> ListenableFuture<T> fetchProperty(PropertyKey<T> key, Modifier ... modifiers) {
        if (!key.isInSection(Section.STATE)) {
            return super.fetchProperty(key, modifiers);
        }

        LinkedList<ListenableFuture<T>> futures = new LinkedList<>();

        synchronized (mMembers) {
            mMembers.forEach((fe) -> futures.add(fe.fetchProperty(key, modifiers)));
        }

        return singleValueFromFutureList(futures);
    }

    @Override
    public <T> ListenableFuture<?> setProperty(PropertyKey<T> key, @Nullable T value,
                                               Modifier ... modifiers) {
        if (!key.isInSection(Section.STATE)) {
            return super.setProperty(key, value, modifiers);
        }

        LinkedList<ListenableFuture<?>> futures = new LinkedList<>();

        synchronized (mMembers) {
            mMembers.forEach((fe) -> futures.add(fe.setProperty(key, value, modifiers)));
        }

        return Futures.successfulAsList(futures);
    }

    @Override
    public <T extends Number> ListenableFuture<?> incrementProperty(PropertyKey<T> key, T amount,
                                                                    Modifier ... modifiers) {
        if (!key.isInSection(Section.STATE)) {
            return super.incrementProperty(key, amount, modifiers);
        }

        LinkedList<ListenableFuture<?>> futures = new LinkedList<>();

        synchronized (mMembers) {
            mMembers.forEach((fe) -> futures.add(fe.incrementProperty(key, amount, modifiers)));
        }

        return Futures.successfulAsList(futures);
    }

    @Override
    public ListenableFuture<?> toggleProperty(PropertyKey<Boolean> key,Modifier ... modifiers) {
        if (!key.isInSection(Section.STATE)) {
            return super.toggleProperty(key, modifiers);
        }

        LinkedList<ListenableFuture<?>> futures = new LinkedList<>();

        synchronized (mMembers) {
            mMembers.forEach((fe) -> futures.add(fe.toggleProperty(key, modifiers)));
        }

        return Futures.successfulAsList(futures);
    }

    @Override
    public <T> ListenableFuture<?> insertValueIntoProperty(PropertyKey<T[]> key, T value,
                                                           Modifier ... modifiers) {
        if (!key.isInSection(Section.STATE)) {
            return super.insertValueIntoProperty(key, value, modifiers);
        }

        LinkedList<ListenableFuture<?>> futures = new LinkedList<>();

        synchronized (mMembers) {
            mMembers.forEach((fe) -> futures.add(fe.insertValueIntoProperty(key, value, modifiers)));
        }

        return Futures.successfulAsList(futures);
    }

    @Override
    public <T> ListenableFuture<?> removeValueFromProperty(PropertyKey<T[]> key, T value,
                                                           Modifier ... modifiers) {
        if (!key.isInSection(Section.STATE)) {
            return super.removeValueFromProperty(key, value, modifiers);
        }

        LinkedList<ListenableFuture<?>> futures = new LinkedList<>();

        synchronized (mMembers) {
            mMembers.forEach((fe) -> futures.add(fe.removeValueFromProperty(key, value, modifiers)));
        }

        return Futures.successfulAsList(futures);
    }

    @Override
    public ListenableFuture<?> applyProperties(Map<String, Object> properties) {
        if (!hasStateProperties(properties)) {
            return super.applyProperties(properties);
        }
        Map<String, Object> nonStateProperties = new HashMap<>(properties);
        Map<String, Object> stateProperties = extractAndRemoveState(nonStateProperties);

        LinkedList<ListenableFuture<?>> futures = new LinkedList<>();

        synchronized (mMembers) {
            mMembers.forEach((fe) -> futures.add(fe.applyProperties(stateProperties)));
        }

        if (!nonStateProperties.isEmpty()) {
            futures.add(super.applyProperties(nonStateProperties));
        }

        return Futures.successfulAsList(futures);
    }

    @Override
    @CanIgnoreReturnValue
    public ListenableFuture<Boolean> delete() {
        mTechnology.unhost(this);
        return Futures.immediateFuture(true);
    }

    @Override
    @CanIgnoreReturnValue
    public <T> ListenableFuture<T> invokeMethod(
            MethodKey<T> methodKey, Map<String, Object> arguments) {

        if (mGroupTrait == getTraitForMethodKey(methodKey)) {
            return super.invokeMethod(methodKey, arguments);
        }

        LinkedList<ListenableFuture<T>> futures = new LinkedList<>();

        arguments = new HashMap<>(arguments);

        // We always add a group id argument, assuming one wasn't already present.
        if (!GroupTrait.PARAM_GROUP_ID.isInMap(arguments)) {
            GroupTrait.PARAM_GROUP_ID.putInMap(arguments, getGroupId());
        }

        synchronized (mMembers) {
            for (FunctionalEndpoint fe : mMembers) {
                futures.add(fe.invokeMethod(methodKey, arguments));
            }
        }

        if (methodKey.getType().isAssignableFrom(FunctionalEndpoint.class)) {
            // This method is returning a functional endpoint.
            // TODO: Do we need to build an aggregate functional endpoint that will represent the
            // new object?
            return Futures.immediateFuture(null);

        } else {
            return singleValueFromFutureList(futures);
        }
    }

    @Override
    public ListenableFuture<Set<PropertyKey<?>>> fetchSupportedPropertyKeys() {
        return super.fetchSupportedPropertyKeys();
    }

    @Override
    public Map<String, Object> copyPersistentState() {
        return super.copyPersistentState();
    }

    @Override
    public void initWithPersistentState(@Nullable Map<String, Object> persistentState) {
        super.initWithPersistentState(persistentState);
        mHasInitialized = true;
    }

    void checkIfWantsFunctionalEndpoint(FunctionalEndpoint fe) {
        synchronized (mUnhostedMemberUids) {
            if (mUnhostedMemberUids.isEmpty()) {
                return;
            }

            String uid = fe.getCachedProperty(BaseTrait.META_UID);

            if (uid == null) {
                // The UID isn't cached, so we go ahead and fetch it here
                // so that it will be available when we need it.

                ListenableFuture<String> future = fe.fetchProperty(BaseTrait.META_UID);

                future.addListener(
                        () -> {
                            if (future.isCancelled()) {
                                return;
                            }

                            String fetchedUid;

                            try {
                                fetchedUid = Futures.getChecked(future, ExecutionException.class);

                            } catch (ExecutionException e) {
                                if (!(e.getCause() instanceof PropertyException)) {
                                    LOGGER.log(
                                            Level.WARNING, "Exception while getting META_UID", e);
                                }
                                return;
                            }

                            synchronized (mUnhostedMemberUids) {
                                if (mUnhostedMemberUids.contains(fetchedUid)) {
                                    if (DEBUG)
                                        LOGGER.info(
                                                "checkIfWantsFunctionalEndpoint: Upgrading "
                                                        + fetchedUid
                                                        + " to a real member");
                                    addMember(fe);
                                }
                            }
                        },
                        getExecutor());

            } else if (mUnhostedMemberUids.contains(uid)) {
                if (DEBUG)
                    LOGGER.info(
                            "checkIfWantsFunctionalEndpoint: Upgrading "
                                    + uid
                                    + " to a real member");
                addMember(fe);
            }
        }
    }
}
