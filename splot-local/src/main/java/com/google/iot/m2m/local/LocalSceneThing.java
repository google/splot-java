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

import com.google.common.collect.Maps;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.iot.m2m.base.*;
import com.google.iot.m2m.trait.BaseTrait;
import com.google.iot.m2m.trait.SceneTrait;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.logging.Logger;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Abstract class for more easily implementing local {@link Thing}s that support
 * scenes.
 *
 * <p>By subclassing this class instead of {@link LocalThing}, scene support will be
 * largely implemented for you automatically. Like {@link LocalThing}, Subclasses of
 * this class should <em>NOT</em> be used to implement non-local Things: in those cases
 * a class directly implementing the methods of {@link Thing} should be used.
 *
 * <p>If scene support doesn't make sense for your thing (for example, if it is an
 * input rather than an output), then you should subclass {@link LocalThing} instead.
 *
 * <p>If you need transition support in addition to scenes, use {@link
 * LocalTransitioningThing} instead.
 *
 * @see LocalThing
 * @see LocalTransitioningThing
 */
public abstract class LocalSceneThing extends LocalThing {
    private static final String SCENES_KEY = "scenes";

    private static final boolean DEBUG = false;
    private static final Logger LOGGER =
            Logger.getLogger(LocalSceneThing.class.getCanonicalName());

    private Scene mCurrentScene = null;
    private String mCurrentGroupId = null;

    private class Scene implements Thing {
        private final String mSceneId;
        private final HashMap<String, Object> mState = new HashMap<>();

        Scene(String sceneId) {
            mSceneId = sceneId;
        }

        String getSceneId() {
            return mSceneId;
        }

        HashMap<String, Object> getState() {
            return mState;
        }

        @Override
        public boolean isLocal() {
            return true;
        }

        String getGroupId() {
            return SceneTrait.STAT_GROUP_ID.getFromMapNoThrow(mState);
        }

        void setGroupId(String groupId) {
            SceneTrait.STAT_GROUP_ID.putInMap(mState, groupId);
        }

        @Override
        public String toString() {
            return "Scene-" + mSceneId;
        }

        @Override
        public <T> ListenableFuture<?> setProperty(PropertyKey<T> key, @Nullable T value, Modifier[] modifiers) {
            LocalTrait trait = getTraitForPropertyKey(key);

            if (trait == null) {
                return Futures.immediateFailedFuture(new PropertyNotFoundException());
            }

            if (!key.isInSection(Section.STATE)) {
                return Futures.immediateFailedFuture(new PropertyOperationUnsupportedException());
            }

            if (!SceneTrait.STAT_GROUP_ID.equals(key) && !trait.onCanSaveProperty(key)) {
                return Futures.immediateFailedFuture(new PropertyOperationUnsupportedException());
            }

            key.putInMap(mState, value);

            changedPersistentState();

            return Futures.immediateFuture(null);
        }

        @Override
        public <T extends Number> ListenableFuture<?> incrementProperty(
                PropertyKey<T> key, T value, Modifier[] modifiers) {
            // Incrementing properties in scenes isn't supported.
            return Futures.immediateFailedFuture(
                    new PropertyOperationUnsupportedException("Scenes only support get/set"));
        }

        @Override
        public <T> ListenableFuture<?> insertValueIntoProperty(PropertyKey<T[]> key, T value, Modifier[] modifiers) {
            // Inserting values into properties in scenes isn't supported.
            return Futures.immediateFailedFuture(
                    new PropertyOperationUnsupportedException("Scenes only support get/set"));
        }

        @Override
        public <T> ListenableFuture<?> removeValueFromProperty(PropertyKey<T[]> key, T value, Modifier[] modifiers) {
            // Removing values from properties in scenes isn't supported.
            return Futures.immediateFailedFuture(
                    new PropertyOperationUnsupportedException("Scenes only support get/set"));
        }

        @Override
        public ListenableFuture<?> toggleProperty(PropertyKey<Boolean> key, Modifier[] modifiers) {
            // Toggling properties in scenes isn't supported.
            return Futures.immediateFailedFuture(
                    new PropertyOperationUnsupportedException("Scenes only support get/set"));
        }

        @Override
        public <T> ListenableFuture<T> fetchProperty(PropertyKey<T> key, Modifier[] modifiers) {
            T ret = getCachedProperty(key);
            if (ret == null) {
                return Futures.immediateFailedFuture(new PropertyNotFoundException());
            }
            return Futures.immediateFuture(ret);
        }

        @Override
        public ListenableFuture<Set<PropertyKey<?>>> fetchSupportedPropertyKeys() {
            return LocalSceneThing.this.fetchSupportedPropertyKeys();
        }

        @Override
        public ListenableFuture<Map<String, Object>> fetchSection(Section section, Modifier... mods) {
            return Futures.immediateFuture(copyCachedSection(section));
        }

        @Nullable
        @Override
        public <T> T getCachedProperty(PropertyKey<T> key) {
            if (key.isInSection(Section.STATE)) {
                return key.getFromMapNoThrow(mState);
            }

            if (BaseTrait.META_UID.equals(key)) {
                return key.cast(mSceneId);
            }

            return null;
        }

        @Override
        public Map<String, Object> copyCachedSection(Section section) {
            Map<String, Object> ret = new HashMap<>();

            if (Section.STATE.equals(section)) {
                ret.putAll(mState);
            } else if (Section.METADATA.equals(section)) {
                BaseTrait.META_UID.putInMap(ret, mSceneId);
            }
            return ret;
        }

        @Override
        public ListenableFuture<?> applyProperties(Map<String, Object> properties) {
            for (Map.Entry<String, Object> entry : properties.entrySet()) {
                final PropertyKey<Object> key = new PropertyKey<>(entry.getKey(), Object.class);
                LocalTrait trait = getTraitForPropertyKey(key);

                if (trait == null) {
                    continue;
                }

                if (!key.isInSection(Section.STATE)
                        || !trait.onCanSaveProperty(key)
                        || SceneTrait.STAT_SCENE_ID.equals(key)) {
                    continue;
                }

                key.putInMap(mState, entry.getValue());
            }
            return Futures.immediateFuture(null);
        }

        @Override
        public <T> void registerPropertyListener(
                Executor executor, PropertyKey<T> key, PropertyListener<T> listener) {
            // We don't support property listeners.
        }

        @Override
        public <T> void unregisterPropertyListener(
                PropertyKey<T> key, PropertyListener<T> listener) {
            // We don't support property listeners.
        }

        @Override
        public void registerSectionListener(Executor executor, Section section, SectionListener listener) {
            // We don't support section listeners.
        }

        @Override
        public void unregisterSectionListener(SectionListener listener) {
            // We don't support section listeners.
        }

        @Override
        public void registerChildListener(
                Executor executor, ChildListener listener, String traitShortName) {
            // We don't support children.
        }

        @Override
        public void unregisterChildListener(ChildListener listener, String traitShortName) {
            // We don't support children.
        }

        @Override
        public void unregisterAllListeners() {
            // We don't support any listeners.
        }

        @Override
        public ListenableFuture<Boolean> delete() {
            boolean ret = mSceneMap.containsKey(mSceneId);
            // This next call will end up calling mSceneTrait.didRemoveChild(this)
            removeSceneId(mSceneId);
            return Futures.immediateFuture(ret);
        }

        @Override
        public <T> ListenableFuture<T> invokeMethod(
                MethodKey<T> methodKey, @Nullable Map<String, Object> arguments) {
            /* Scenes don't support any methods */
            return Futures.immediateFailedFuture(new MethodNotFoundException());
        }

        @Override
        public ListenableFuture<Collection<Thing>> fetchChildrenForTrait(
                String traitShortId) {
            return Futures.immediateFuture(null);
        }

        @Nullable
        @Override
        public String getTraitForChild(Thing child) {
            return null;
        }

        @Nullable
        @Override
        public String getIdForChild(Thing child) {
            return null;
        }

        @Nullable
        @Override
        public Thing getChild(String traitShortId, String childId) {
            return null;
        }

        @Nullable
        @Override
        public Thing getParentThing() {
            return LocalSceneThing.this;
        }
    }

    private final Map<String, Scene> mSceneMap = new HashMap<>();

    private final SceneTrait.AbstractLocalTrait mSceneTrait =
            new SceneTrait.AbstractLocalTrait() {
                @Override
                public String onGetSceneId() {
                    Scene currentScene = mCurrentScene;
                    if (currentScene == null) {
                        return null;
                    }
                    return currentScene.mSceneId;
                }

                @Override
                public void onSetSceneId(@Nullable String value) {
                    // Do nothing, this is handled at a higher level in
                    // order to properly support transitions.
                }

                @Override
                public String onGetGroupId() {
                    return mCurrentGroupId;
                }

                @Override
                public Thing onInvokeSave(Map<String, Object> args)
                        throws InvalidMethodArgumentsException {
                    if (!SceneTrait.PARAM_SCENE_ID.isInMap(args)) {
                        throw new InvalidMethodArgumentsException(
                                "Missing argument \"" + SceneTrait.PARAM_SCENE_ID + "\"");
                    }

                    String sceneId = SceneTrait.PARAM_SCENE_ID.getFromMapNoThrow(args);
                    String groupId = SceneTrait.PARAM_GROUP_ID.getFromMapNoThrow(args);

                    if (sceneId == null) {
                        throw new InvalidMethodArgumentsException(
                                "Invalid \"" + SceneTrait.PARAM_SCENE_ID + "\"");
                    }

                    synchronized (mSceneMap) {
                        // This will call didAddChild(scene) if necessary.
                        saveCurrentStateToSceneId(sceneId);

                        Scene scene = mSceneMap.get(sceneId);

                        if (groupId != null) {
                            scene.setGroupId(groupId);
                        }

                        return scene;
                    }
                }

                @Override
                public String onGetIdForChild(Thing child) {
                    if (child instanceof Scene) {
                        return ((Scene) child).mSceneId;
                    }
                    return null;
                }

                @Override
                public Thing onGetChild(String childId) {
                    return mSceneMap.get(childId);
                }

                @Override
                public Set<Thing> onCopyChildrenSet() {
                    synchronized (mSceneMap) {
                        return new HashSet<>(mSceneMap.values());
                    }
                }
            };

    protected LocalSceneThing() {
        registerTrait(mSceneTrait);
    }

    private void updateCurrentScene(@Nullable Scene scene) {
        if (!Objects.equals(mCurrentScene, scene)) {
            mCurrentScene = scene;
            mSceneTrait.didChangeSceneId(mCurrentScene != null ? mCurrentScene.getSceneId() : null);
            if (scene != null && !Objects.equals(mCurrentGroupId, scene.getGroupId())) {
                mCurrentGroupId = scene.getGroupId();
                mSceneTrait.didChangeGroupId(mCurrentGroupId);
            }
        }
    }

    private HashMap<String, Object> getSceneIdState(String sceneId) {
        synchronized (mSceneMap) {
            Scene scene = mSceneMap.get(sceneId);
            return scene != null ? scene.mState : null;
        }
    }

    /**
     * Returns a the set of scene IDs that this thing currently has.
     *
     * <p>Note that this method is only present in {@link LocalSceneThing}, but you can
     * get the same information with any other Thing that supports {@link SceneTrait}
     * by instead using the following more generic code:
     *
     * <pre>{@code
     * Collection<Thing> sceneFeList = fetchChildrenForTrait(SceneTrait.PARAM_SCENE_ID).get();
     * HashSet<String> sceneIdSet = new HashSet<>();
     * for (Thing sceneFe : sceneFeList) {
     *     sceneIdSet.add(getIdForChild(sceneFe));
     * }
     * }</pre>
     *
     * The above code will work for any Thing that implements support for the scene
     * trait, whereas this method will only be available for for subclasses of {@link
     * LocalSceneThing}.
     *
     * @see #fetchChildrenForTrait(String)
     * @see #getIdForChild(Thing)
     */
    public final Set<String> getSceneIds() {
        synchronized (mSceneMap) {
            return new HashSet<>(mSceneMap.keySet());
        }
    }

    /**
     * Saves the current state to the given scene id.
     *
     * <p>Note that this method is only present in {@link LocalSceneThing}, but you can
     * easily make the same call to other Things that support {@link SceneTrait} by
     * instead using the following more generic code:
     *
     * <pre>{@code
     * invokeMethod(SceneTrait.METHOD_SAVE, SceneTrait.PARAM_SCENE_ID, sceneId).get();
     * }</pre>
     *
     * The above code will work for any Thing that implements support for the scene
     * trait, whereas this method will only be available for for subclasses of {@link
     * LocalSceneThing}.
     *
     * @see #invokeMethod(MethodKey, TypedKeyValue[])
     */
    public final void saveCurrentStateToSceneId(String sceneId) {
        Map<String, Object> state = copyCachedSection(Section.STATE);

        // Remove all properties that aren't persistent
        for (String keyName : new HashSet<>(state.keySet())) {
            final PropertyKey<?> key = new PropertyKey<>(keyName, Object.class);
            final LocalTrait trait = getTraitForPropertyKey(key);

            if (!trait.onCanSaveProperty(key)) {
                key.removeFromMap(state);
            }
        }

        synchronized (mSceneMap) {
            Scene scene = mSceneMap.get(sceneId);

            if (scene == null) {
                scene = new Scene(sceneId);
                if (mSceneMap.put(sceneId, scene) == null) {
                    mSceneTrait.didAddChild(scene);
                }
            }

            scene.mState.putAll(state);
        }
    }

    /**
     * Removes the scene with the given {@code sceneId} from this thing.
     *
     * <p>Note that this method is only present in {@link LocalSceneThing}, but you can
     * easily make the same call to other Things that support {@link SceneTrait} by
     * instead using the following more generic code:
     *
     * <pre>{@code
     * Thing sceneFe = this.getChild(SceneTrait.TRAIT_ID, sceneId);
     * if (sceneFe != null) {
     *     sceneFe.delete().get();
     * }
     * }</pre>
     *
     * The above code will work for any Thing that implements support for the scene
     * trait, whereas this method will only be available for for subclasses of {@link
     * LocalSceneThing}.
     *
     * @see #getChild(String, String)
     * @see #delete()
     */
    public final void removeSceneId(String sceneId) {
        synchronized (mSceneMap) {
            Scene scene = mSceneMap.get(sceneId);
            if (scene != null) {
                mSceneTrait.didRemoveChild(scene);
                mSceneMap.remove(sceneId);
            }
        }
    }

    /** TODO: Consider making this protected. */
    @Override
    Map<String, Object> expandProperties(Map<String, Object> properties)
            throws PropertyException, TechnologyException {

        final String sceneId;

        try {
            sceneId = SceneTrait.STAT_SCENE_ID.getFromMap(properties);
        } catch (InvalidValueException e) {
            throw new InvalidPropertyValueException(e);
        }

        if (sceneId != null) {
            Map<String, Object> sceneState = getSceneIdState(sceneId);
            if (sceneState != null) {
                Map<String, Object> newState = new HashMap<>(sceneState);
                newState.putAll(super.expandProperties(properties));
                SceneTrait.STAT_GROUP_ID.removeFromMap(newState);
                return newState;
            }
        }

        return super.expandProperties(properties);
    }

    /** TODO: Consider making this protected. */
    @Override
    void applyPropertiesHook(Map<String, Object> properties)
            throws PropertyException, TechnologyException {
        if (hasStateProperties(properties)) {
            try {
                final String sceneId = SceneTrait.STAT_SCENE_ID.getFromMap(properties);
                updateCurrentScene(mSceneMap.get(sceneId));
            } catch (InvalidValueException e) {
                throw new InvalidPropertyValueException(e);
            }
        }
        super.applyPropertiesHook(properties);
    }

    @Override
    public Map<String, Object> copyPersistentState() {
        Map<String, Object> ret = super.copyPersistentState();

        synchronized (mSceneMap) {
            if (!mSceneMap.isEmpty()) {
                Map<String, Map<String, Object>> scenes = new HashMap<>();
                ret.put(SCENES_KEY, scenes);

                for (Map.Entry<String, Scene> entry : mSceneMap.entrySet()) {
                    scenes.put(entry.getKey(), entry.getValue().getState());
                }
            }
        }

        return ret;
    }

    @Override
    public void initWithPersistentState(@Nullable Map<String, Object> persistentState) {
        if (persistentState != null && persistentState.containsKey(SCENES_KEY)) {
            @SuppressWarnings("unchecked")
            Map<String, Map<String, Object>> scenes =
                    (Map<String, Map<String, Object>>) persistentState.get(SCENES_KEY);

            synchronized (mSceneMap) {
                for (Map.Entry<String, Map<String, Object>> entry : scenes.entrySet()) {
                    final Scene scene = new Scene(entry.getKey());

                    scene.mState.putAll(entry.getValue());

                    mSceneMap.put(entry.getKey(), scene);
                }
            }

            persistentState =
                    Maps.filterEntries(
                            persistentState, (e) -> e != null && !SCENES_KEY.equals(e.getKey()));
        }
        super.initWithPersistentState(persistentState);
    }
}
