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

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.iot.m2m.base.*;
import com.google.iot.m2m.trait.BaseTrait;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;

import com.google.iot.m2m.trait.TransitionTrait;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Abstract class for more easily implementing local {@link Thing}s. This class is
 * intended by be subclassed by developers who are implementing Things which are
 * locally hosted---meaning those things which will be used to manipulate aspects of
 * the device this code is running on. Subclasses of this class should <em>NOT</em> be used to
 * implement non-local Things: in those cases a class directly implementing the methods
 * of {@link Thing} should be used.
 *
 * <p>Note that there are special subclasses of this class which provide native support for scenes
 * and transitions: {@link LocalSceneThing} and {@link
 * LocalTransitioningThing}, respectively. If your thing needs to support
 * either, you should subclass one of those classes instead.
 *
 * @see LocalSceneThing
 * @see LocalTransitioningThing
 */
public abstract class LocalThing
        implements Thing, PersistentStateInterface {
    private static final boolean DEBUG = false;
    private static final Logger LOGGER =
            Logger.getLogger(LocalThing.class.getCanonicalName());

    private final Map<String, LocalTrait> mTraits = new HashMap<>();
    private final Map<PropertyKey<?>, LocalTrait> mPropertyMap = new HashMap<>();
    private final Map<MethodKey, LocalTrait> mMethodMap = new HashMap<>();

    private final Map<PropertyKey, Set<PropertyListenerEntry>> mPropertyListenerMap =
            new ConcurrentHashMap<>();
    private final Map<SectionListener, Executor> mStateListenerMap = new ConcurrentHashMap<>();
    private final Map<SectionListener, Executor> mConfigListenerMap = new ConcurrentHashMap<>();
    private final Map<SectionListener, Executor> mMetadataListenerMap = new ConcurrentHashMap<>();
    private final Map<String, Set<ChildListenerEntry>> mChildListenerMap =
            new ConcurrentHashMap<>();

    private PersistentStateListener mPersistentStateListener = null;

    class PropertyListenerEntry {
        final Executor mExecutor;
        final PropertyListener<?> mListener;

        <T> PropertyListenerEntry(Executor executor, PropertyListener<T> listener) {
            mExecutor = executor;
            mListener = listener;
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }

            if (!(o instanceof PropertyListenerEntry)) {
                return false;
            }

            return mListener.equals(((PropertyListenerEntry) o).mListener);
        }

        @Override
        public int hashCode() {
            return mListener.hashCode();
        }
    }

    class ChildListenerEntry {
        final Executor mExecutor;
        final ChildListener mListener;

        ChildListenerEntry(Executor executor, ChildListener listener) {
            mExecutor = executor;
            mListener = listener;
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }
            if (!(o instanceof ChildListenerEntry)) {
                return false;
            }
            return mListener.equals(((ChildListenerEntry) o).mListener);
        }

        @Override
        public int hashCode() {
            return mListener.hashCode();
        }
    }

    /**
     * Returns the executor that will be used to internally schedule some callbacks. This method can
     * optionally be overridden to specify your own {@link Executor} instance for this class to use.
     * {@hide}
     */
    protected Executor getExecutor() {
        // Default executor returns immediately, which is OK for subclasses of
        // LocalThing and LocalSceneThing but not
        // adequate for LocalTransitioningThing. That class provides
        // its own override of this method.
        return Runnable::run;
    }

    <T> ListenableFuture<T> submit(Callable<T> callable) {
        ListenableFutureTask<T> future = ListenableFutureTask.create(callable);

        getExecutor().execute(future);

        return future;
    }

    protected LocalThing() {}

    /**
     * Registers the given trait with the local thing. This method is only called
     * during construction.
     *
     * @param trait The trait to register with this FE.
     */
    protected final synchronized void registerTrait(LocalTrait trait) {
        mTraits.put(trait.getTraitId(), trait);

        trait.setCallback(
                new LocalTrait.Callback() {
                    @Override
                    public void onChildAdded(
                            LocalTrait localTrait, Thing thing) {
                        LocalThing.this.onChildAdded(localTrait, thing);
                    }

                    @Override
                    public void onChildRemoved(
                            LocalTrait localTrait, Thing thing) {
                        LocalThing.this.onChildRemoved(localTrait, thing);
                    }

                    @Override
                    public <T> void onPropertyChanged(
                            LocalTrait localTrait, PropertyKey<T> propertyKey, T t) {
                        LocalThing.this.onPropertyChanged(localTrait, propertyKey, t);
                    }
                });

        for (PropertyKey<?> key : trait.getSupportedPropertyKeys()) {
            if (mPropertyMap.containsKey(key)) {
                // This should never happen
                throw new TechnologyRuntimeException(
                        "Two traits with the same property key \"" + key + "\"");
            }
            mPropertyMap.put(key, trait);
        }

        for (MethodKey key : trait.getSupportedMethodKeys()) {
            if (mMethodMap.containsKey(key)) {
                // This should never happen
                throw new TechnologyRuntimeException(
                        "Two traits with the same method key \"" + key + "\"");
            }
            mMethodMap.put(key, trait);
        }
    }

    private void onChildAdded(LocalTrait localTrait, Thing child) {
        changedPersistentState();

        if (DEBUG) {
            LOGGER.info("Adding " + child + " to trait " + localTrait.getTraitId());
        }

        if (mChildListenerMap.containsKey(localTrait.getTraitId())) {
            // Keys are only ever added to the mChildListenerMap,
            // never removed, so this check is OK without synchronization
            for (ChildListenerEntry entry : mChildListenerMap.get(localTrait.getTraitId())) {
                if (DEBUG) {
                    LOGGER.info("Notifying " + entry.mListener);
                }
                entry.mExecutor.execute(
                        () -> entry.mListener.onChildAdded(this, localTrait.getTraitId(), child));
            }
        }
    }

    private void onChildRemoved(LocalTrait localTrait, Thing child) {
        changedPersistentState();

        if (DEBUG) {
            LOGGER.info("Removing " + child + " from trait " + localTrait.getTraitId());
        }

        if (mChildListenerMap.containsKey(localTrait.getTraitId())) {
            // Keys are only ever added to the mChildListenerMap,
            // never removed, so this check is OK without synchronization
            for (ChildListenerEntry entry : mChildListenerMap.get(localTrait.getTraitId())) {
                entry.mExecutor.execute(
                        () -> entry.mListener.onChildRemoved(this, localTrait.getTraitId(), child));
            }
        }
    }

    private Map<SectionListener, Executor> getSectionListenerMap(Section section) {
        switch (section) {
            case STATE:
                return mStateListenerMap;

            case CONFIG:
                return mConfigListenerMap;

            case METADATA:
                return mMetadataListenerMap;
        }
        throw new AssertionError(new InvalidSectionException("Invalid section: " + section));
    }

    private <T> void onPropertyChanged(
            @SuppressWarnings("unused") LocalTrait localTrait, PropertyKey<T> key, T value) {
        if (!key.isInSection(Section.STATE)) {
            changedPersistentState();
        }

        if (mPropertyListenerMap.containsKey(key)) {
            // Keys are only ever added to the mPropertyListenerMap,
            // never removed, so this check is OK without synchronization
            for (PropertyListenerEntry entry : mPropertyListenerMap.get(key)) {
                @SuppressWarnings("unchecked")
                PropertyListener<T> listener = ((PropertyListener<T>) entry.mListener);
                entry.mExecutor.execute(() -> listener.onPropertyChanged(this, key, value));
            }
        }

        Section section = key.getSection();

        Map<String, Object> map = copyCachedSection(section);

        key.putInMap(map, value);

        getSectionListenerMap(section).forEach(
                (listener, exec) -> exec.execute( () -> listener.onSectionChanged(this, map)));
    }

    @Override
    public boolean isLocal() {
        return true;
    }

    LocalTrait getTraitForPropertyKey(PropertyKey<?> key) {
        return mPropertyMap.get(key);
    }

    LocalTrait getTraitForMethodKey(MethodKey<?> key) {
        return mMethodMap.get(key);
    }

    @Override
    public <T> ListenableFuture<?> setProperty(PropertyKey<T> key, @Nullable T value,
                                               Modifier ... modifiers) {
        Map<String, Object> map = new HashMap<>();
        key.putInMap(map, value);
        final LocalTrait trait = getTraitForPropertyKey(key);

        if (trait == null) {
            return Futures.immediateFailedFuture(
                    new PropertyNotFoundException("Unknown property " + key));
        }

        for (Modifier mod : modifiers) {
            if (mod instanceof Modifier.Duration) {
                TransitionTrait.STAT_DURATION.putInMap(map,
                        (float)((Modifier.Duration)mod).getDuration());
            }
        }

        return applyProperties(map);
    }

    @Override
    public <T extends Number> ListenableFuture<?> incrementProperty(PropertyKey<T> key, T amount,
                                                                    Modifier ... modifiers) {
        Number targetValue;

        try {
            targetValue = getPropertyTargetValue(key);

        } catch (PropertyException | TechnologyException | InvalidModifierListException e) {
            return Futures.immediateFailedFuture(e);
        }

        if (targetValue == null) {
            return Futures.immediateFailedFuture(
                    new PropertyOperationUnsupportedException("Can't increment null"));

        } else if (targetValue instanceof Float) {
            targetValue = targetValue.floatValue() + amount.floatValue();

        } else if (targetValue instanceof Integer) {
            targetValue = targetValue.intValue() + amount.intValue();

        } else if (targetValue instanceof Long) {
            targetValue = targetValue.longValue() + amount.longValue();

        } else if (targetValue instanceof Short) {
            targetValue = targetValue.shortValue() + amount.shortValue();

        } else if (targetValue instanceof Byte) {
            targetValue = targetValue.byteValue() + amount.byteValue();

        } else {
            targetValue = targetValue.doubleValue() + amount.doubleValue();
        }

        return this.setProperty(key, key.cast(targetValue), modifiers);
    }

    @Override
    @CanIgnoreReturnValue
    public ListenableFuture<?> toggleProperty(PropertyKey<Boolean> key, Modifier ... modifiers) {
        try {
            Boolean value = getPropertyTargetValue(key);
            if (value == null) {
                return Futures.immediateFailedFuture(
                        new PropertyOperationUnsupportedException("Can't toggle null"));
            } else {
                return this.setProperty(key, !value, modifiers);
            }
        } catch (PropertyException | TechnologyException | InvalidModifierListException x) {
            return Futures.immediateFailedFuture(x);
        }
    }

    @Override
    public <T> ListenableFuture<?> insertValueIntoProperty(PropertyKey<T[]> key, T value,
                                                           Modifier ... modifiers) {
        try {

            final T[] oldArray = getPropertyTargetValue(key);
            final ArrayList<T> newArray;

            if (oldArray != null) {
                newArray = new ArrayList<>(oldArray.length + 1);
                newArray.addAll(Arrays.asList(oldArray));
            } else {
                newArray = new ArrayList<>(1);
            }

            newArray.add(value);

            @SuppressWarnings("unchecked")
            final T[] retArray =
                    (T[]) Array.newInstance(key.getType().getComponentType(), newArray.size());

            return this.setProperty(key, newArray.toArray(retArray), modifiers);

        } catch (PropertyException | TechnologyException | InvalidModifierListException x) {
            return Futures.immediateFailedFuture(x);
        }
    }

    @Override
    public <T> ListenableFuture<?> removeValueFromProperty(PropertyKey<T[]> key, T value,
                                                           Modifier ... modifiers) {
        try {

            T[] oldArray = getPropertyTargetValue(key);

            if (oldArray == null) {
                return Futures.immediateFailedFuture(
                        new PropertyException("Can't remove item from null value"));
            }

            List<T> newList = new LinkedList<>(Arrays.asList(oldArray));

            newList.removeIf(x -> Objects.equals(x, value));

            @SuppressWarnings("unchecked")
            T[] newArray =
                    newList.toArray(
                            (T[])
                                    java.lang.reflect.Array.newInstance(
                                            oldArray.getClass().getComponentType(),
                                            newList.size()));

            return this.setProperty(key, newArray, modifiers);

        } catch (PropertyException | TechnologyException | InvalidModifierListException x) {
            return Futures.immediateFailedFuture(x);
        }
    }

    @Nullable
    private <T> T getPropertyCurrentValue(PropertyKey<T> key)
            throws PropertyException, TechnologyException {
        final LocalTrait trait = getTraitForPropertyKey(key);

        if (trait == null) {
            throw new PropertyNotFoundException("Unknown property " + key);
        }

        return trait.getValueForPropertyKey(key);
    }

    /** This is overridden by subclasses to implement transitions. */
    @Nullable
    protected <T> T getPropertyTargetValue(PropertyKey<T> key)
            throws PropertyException, TechnologyException {
        return this.getPropertyCurrentValue(key);
    }

    @Override
    @Nullable
    public <T> T getCachedProperty(PropertyKey<T> key) {
        try {
            return getPropertyCurrentValue(key);
        } catch (PropertyException | TechnologyException ignored) {
            return null;
        }
    }

    @Override
    public <T> ListenableFuture<T> fetchProperty(PropertyKey<T> key, Modifier ... modifiers) {
        for (Modifier mod : modifiers) {
            if (mod instanceof Modifier.Duration || mod instanceof Modifier.TransitionTarget) {
                return submit(() -> this.getPropertyTargetValue(key));
            }
        }
        return submit(() -> this.getPropertyCurrentValue(key));
    }

    /**
     * An immediately returning variant of {@link #fetchSupportedPropertyKeys()} that is only
     * available on subclasses of {@link LocalThing}.
     *
     * @return a set containing all of the property keys that are supported by this functional
     *     endpoint.
     */
    public Set<PropertyKey<?>> getSupportedPropertyKeys() {
        return new HashSet<>(mPropertyMap.keySet());
    }

    @Override
    public ListenableFuture<Set<PropertyKey<?>>> fetchSupportedPropertyKeys() {
        return submit(this::getSupportedPropertyKeys);
    }

    @Override
    public ListenableFuture<Map<String, Object>> fetchSection(Section section,
                                                              Modifier... mods) {
        boolean getAllKeys = false;

        for (Modifier mod : mods) {
            if (mod instanceof Modifier.All) {
                getAllKeys = true;
            }
        }

        if (!getAllKeys) {
            return submit(() -> copyCachedSection(section));
        }

        return submit(() -> {
            Map<String, Object> ret = new LinkedHashMap<>();
            for (Map.Entry<PropertyKey<?>, LocalTrait> entry : mPropertyMap.entrySet()) {
                PropertyKey<?> key = entry.getKey();

                if (!key.isInSection(section)) {
                    continue;
                }

                final LocalTrait trait = entry.getValue();

                Object value = null;

                try {
                    value = trait.getValueForPropertyKey(key);
                } catch (PropertyException | TechnologyException ignored) {
                }

                // In this case we put the value in the map whether
                // it is null or not.
                ret.put(key.getName(), value);
            }
            return ret;
        });
    }

    @Override
    public Map<String, Object> copyCachedSection(Section section) {
        Map<String, Object> ret = new LinkedHashMap<>();
        for (Map.Entry<PropertyKey<?>, LocalTrait> entry : mPropertyMap.entrySet()) {
            PropertyKey<?> key = entry.getKey();

            if (!key.isInSection(section)) {
                continue;
            }

            LocalTrait trait = entry.getValue();

            Object value = null;

            try {
                value = trait.getValueForPropertyKey(key);
            } catch (PropertyException | TechnologyException ignored) {
            }

            // We only return values that aren't null
            if (value != null) {
                ret.put(key.getName(), value);
            }
        }
        return ret;
    }

    /** Extracts and removes all state properties from map, returning the removed properties. */
    static Map<String, Object> extractAndRemoveState(Map<String, Object> map) {
        Map<String, Object> ret = new HashMap<>();
        for (String key : new ArrayList<>(map.keySet())) {
            if (key.startsWith(Splot.SECTION_STATE + "/")) {
                ret.put(key, map.get(key));
                map.remove(key);
            }
        }
        return ret;
    }

    static boolean hasStateProperties(Map<String, Object> map) {
        for (String key : map.keySet()) {
            if (key.startsWith(Splot.SECTION_STATE + "/")) {
                return true;
            }
        }
        return false;
    }

    /** TODO: Consider making this protected. */
    Map<String, Object> expandProperties(Map<String, Object> properties)
            throws PropertyException, TechnologyException {
        Map<String, Object> newState = new HashMap<>();

        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            final PropertyKey<Object> key = new PropertyKey<>(entry.getKey(), Object.class);
            final LocalTrait trait = getTraitForPropertyKey(key);
            final Object value;

            if (trait == null) {
                value = entry.getValue();
            } else {
                value = trait.sanitizeValueForPropertyKey(key, entry.getValue());
            }

            newState.put(entry.getKey(), value);
        }

        return newState;
    }

    void applyPropertiesImmediately(Map<String, Object> properties)
            throws PropertyException, TechnologyException {
        PropertyException exception = null;

        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            final PropertyKey<Object> key = new PropertyKey<>(entry.getKey(), Object.class);
            final LocalTrait trait = getTraitForPropertyKey(key);

            if (trait == null) {
                if (DEBUG) {
                    LOGGER.warning("No trait for key " + key);
                }
                continue;
            }

            try {
                trait.setValueForPropertyKey(key, entry.getValue());

            } catch (PropertyException x) {
                if (DEBUG) {
                    LOGGER.warning(
                            "Attempt to set " + key + " to " + entry.getValue() + " threw " + x);
                    x.printStackTrace();
                }
                exception = x;
            }
        }

        if (exception != null) {
            throw exception;
        }
    }

    /** TODO: Consider making this protected instead of package-private */
    void applyPropertiesHook(Map<String, Object> properties)
            throws PropertyException, TechnologyException {
        applyPropertiesImmediately(properties);
    }

    @Override
    public ListenableFuture<?> applyProperties(Map<String, Object> properties) {
        return submit(
                () -> {
                    applyPropertiesHook(expandProperties(properties));

                    // We need this next line because we may throw checked exceptions
                    // and we can't throw checked exceptions from a Runnable.
                    return null;
                });
    }

    @Override
    public final synchronized <T> void registerPropertyListener(
            Executor executor, PropertyKey<T> key, PropertyListener<T> listener) {
        final PropertyListenerEntry entry = new PropertyListenerEntry(executor, listener);
        final Set<PropertyListenerEntry> keySet =
                mPropertyListenerMap.computeIfAbsent(key, k -> new HashSet<>());
        keySet.add(entry);
        executor.execute(()->{
            try {
                listener.onPropertyChanged(this, key, getPropertyCurrentValue(key));
            } catch (PropertyException|TechnologyException ignored) {
                // We eat these exceptions since they simply indicate that
                // the property couldn't be directly accessed at this time.
            }
        });
    }

    @Override
    public final synchronized <T> void unregisterPropertyListener(
            PropertyKey<T> key, PropertyListener<T> listener) {
        final Set<PropertyListenerEntry> keySet = mPropertyListenerMap.get(key);
        if (keySet != null) {
            if (!keySet.removeIf(
                    propertyListenerEntry -> propertyListenerEntry.mListener == listener)) {
                if (DEBUG) {
                    LOGGER.warning(
                            "unregisterPropertyListener() was called for "
                                    + key
                                    + ", but couldn't find listener "
                                    + listener);
                }
            }
        }
    }

    @Override
    public void registerChildListener(
            Executor executor, ChildListener listener, String traitShortName) {
        final LocalTrait trait = mTraits.get(traitShortName);

        if (trait == null) {
            // No trait, nothing to do.
            LOGGER.warning(
                    "Attempted to register a child listener "
                            + listener
                            + " on trait \""
                            + traitShortName
                            + "\", which doesn't exist on "
                            + this);
            return;
        }

        Set<Thing> children = trait.onCopyChildrenSet();

        if (children == null) {
            // Trait doesn't support children.
            LOGGER.warning(
                    "Attempted to register a child listener "
                            + listener
                            + " on trait \""
                            + traitShortName
                            + "\", which doesn't support children.");
            return;
        }

        final ChildListenerEntry entry = new ChildListenerEntry(executor, listener);
        final Set<ChildListenerEntry> keySet =
                mChildListenerMap.computeIfAbsent(traitShortName, k -> new HashSet<>());

        keySet.add(entry);

        if (!children.isEmpty()) {
            // Pre announce children
            entry.mExecutor.execute(
                    () -> {
                        if (DEBUG) LOGGER.info("Pre-announcing children to " + listener);
                        for (Thing child : children) {
                            entry.mListener.onChildAdded(this, traitShortName, child);
                        }
                    });
        }
    }

    @Override
    public void unregisterChildListener(ChildListener listener, String traitShortName) {
        final Set<ChildListenerEntry> keySet = mChildListenerMap.get(traitShortName);
        if (keySet != null) {
            if (!keySet.removeIf(childListenerEntry -> childListenerEntry.mListener == listener)) {
                if (DEBUG) {
                    LOGGER.warning(
                            "unregisterChildListener() was called for "
                                    + traitShortName
                                    + ", but couldn't find listener "
                                    + listener);
                }
            }
        }
    }

    @Override
    public final synchronized void registerSectionListener(Executor executor,
                                                           Section section,
                                                           SectionListener listener) {
        getSectionListenerMap(section).put(listener, executor);
        executor.execute(()->listener.onSectionChanged(this, copyCachedSection(section)));
    }

    @Override
    public final synchronized void unregisterSectionListener(SectionListener listener) {
        mStateListenerMap.remove(listener);
        mConfigListenerMap.remove(listener);
        mMetadataListenerMap.remove(listener);
    }

    @Override
    public void unregisterAllListeners() {
        mMetadataListenerMap.clear();
        mConfigListenerMap.clear();
        mStateListenerMap.clear();
        mPropertyListenerMap.clear();
        mChildListenerMap.clear();
    }

    @Override
    @CanIgnoreReturnValue
    public ListenableFuture<Boolean> delete() {
        // Default behavior is you can't delete a local thing.
        return Futures.immediateFuture(false);
    }

    @Override
    public <T> ListenableFuture<T> invokeMethod(MethodKey<T> key, Map<String, Object> arguments) {
        final LocalTrait trait = getTraitForMethodKey(key);

        if (trait == null) {
            return Futures.immediateFailedFuture(
                    new MethodNotFoundException("Unknown method " + key));
        }

        return submit(() -> trait.invokeMethod(key, arguments));
    }

    @Override
    public ListenableFuture<Collection<Thing>> fetchChildrenForTrait(
            String traitShortId) {
        final LocalTrait trait = mTraits.get(traitShortId);

        if (trait == null) {
            return Futures.immediateFuture(null);
        }

        return submit(trait::onCopyChildrenSet);
    }

    @Nullable
    @Override
    public final String getTraitForChild(Thing child) {
        for (Map.Entry<String, LocalTrait> entry : mTraits.entrySet()) {
            Set<Thing> children = entry.getValue().onCopyChildrenSet();
            if (children != null && children.contains(child)) {
                return entry.getKey();
            }
        }
        return null;
    }

    @Nullable
    @Override
    public String getIdForChild(Thing child) {
        for (LocalTrait trait : mTraits.values()) {
            Set<Thing> children = trait.onCopyChildrenSet();
            if (children != null && children.contains(child)) {
                return trait.onGetIdForChild(child);
            }
        }
        return null;
    }

    @Nullable
    @Override
    public Thing getChild(String traitShortId, String childId) {
        final LocalTrait trait = mTraits.get(traitShortId);

        if (trait == null) {
            return null;
        }

        return trait.onGetChild(childId);
    }

    @Nullable
    @Override
    public Thing getParentThing() {
        return null;
    }

    /**
     * Called whenever the persistent state of this thing has changed.
     *
     * <p>The persistent state will be written to non-volatile storage in the background.
     */
    final void changedPersistentState() {
        final PersistentStateListener listener = mPersistentStateListener;

        if (listener != null) {
            listener.onSavePersistentState(this);
        }
    }

    /**
     * Called whenever the persistent state of this thing has changed, and execution
     * should be blocked until the state has been committed.
     *
     * <p>This method will block until it has confirmed that the persistent state has been
     * successfully written to non-volatile storage.
     *
     * @throws TechnologyException if there are problems saving the state.
     */
    final void changedPersistentStateBlocking() throws TechnologyException {
        final PersistentStateListener listener = mPersistentStateListener;

        if (listener != null) {
            try {
                listener.onSavePersistentStateBlocking(this);
            } catch (IOException x) {
                throw new TechnologyException(x);
            }
        }
    }

    @Override
    public Map<String, Object> copyPersistentState() {
        Map<String, Object> ret = new HashMap<>();

        for (Map.Entry<PropertyKey<?>, LocalTrait> entry : mPropertyMap.entrySet()) {
            final PropertyKey<?> key = entry.getKey();
            final LocalTrait trait = entry.getValue();

            if (!trait.onCanSaveProperty(key)) {
                continue;
            }

            Object value = null;

            try {
                value = trait.getValueForPropertyKey(key);
            } catch (PropertyException | TechnologyException ignored) {
            }

            if (DEBUG) LOGGER.info("copyPersistentState: " + key + " = " + value);

            if (value != null) {
                ret.put(key.getName(), value);
            }
        }

        return ret;
    }

    @Override
    public void initWithPersistentState(@Nullable Map<String, Object> persistentState) {
        if (DEBUG) LOGGER.info("initWithPersistentState: " + persistentState);

        if (persistentState == null) {
            return;
        }

        try {
            applyPropertiesImmediately(expandProperties(persistentState));
        } catch (PropertyException x) {
            LOGGER.warning("initWithPersistentState: " + x + ", " + persistentState);
        } catch (TechnologyException x) {
            LOGGER.warning("initWithPersistentState: " + x + ", " + persistentState);
            throw new TechnologyRuntimeException(x);
        }
    }

    @Override
    public void setPersistentStateListener(@Nullable PersistentStateListener listener) {
        mPersistentStateListener = listener;
    }

    @Override
    public String toString() {
        String id = getCachedProperty(BaseTrait.META_UID);
        if (id != null) {
            return getClass().getSimpleName() + "-" + id;
        }
        return super.toString();
    }
}
