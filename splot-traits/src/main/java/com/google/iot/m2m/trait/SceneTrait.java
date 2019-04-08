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
package com.google.iot.m2m.trait;

import static com.google.iot.m2m.annotation.Method.WANTS_GROUP_ID;
import static com.google.iot.m2m.annotation.Property.*;

import com.google.iot.m2m.annotation.Method;
import com.google.iot.m2m.annotation.Property;
import com.google.iot.m2m.annotation.Trait;
import com.google.iot.m2m.base.*;

/**
 * The Scene trait describes a FE that supports scenes: named collections of state properties that
 * can be easily recalled.
 */
@Trait
public final class SceneTrait {
    // Prevent Instantiation
    private SceneTrait() {}

    /** Abstract class for implementing trait behavior on a local functional endpoint. */
    public abstract static class AbstractLocalTrait extends LocalSceneTrait {}

    /** The name of this trait */
    public static final String TRAIT_NAME = "Scene";

    /** The URI that identifies the specification used to implement this trait. */
    public static final String TRAIT_URI = "tag:google.com,2018:m2m:traits:scene:v1:v0#r0";

    /** The Short ID of this trait (<code>"scen"</code>) */
    public static final String TRAIT_ID = "scen";

    /** Flag indicating if this trait supports children or not. */
    public static final boolean TRAIT_SUPPORTS_CHILDREN = true;

    /**
     * Property key for the URI that identifies the specification used to implement this trait. This
     * property is present on all traits.
     */
    @Property(READ_ONLY | REQUIRED)
    public static final PropertyKey<String> META_TRAIT_URI =
            new PropertyKey<>(Section.METADATA, TRAIT_ID, "turi", String.class);

    /**
     * Saves the current state to the given SceneId, optionally including a group id. Returns Sub-FE
     * for the created/updated scene.
     *
     * @see #PARAM_SCENE_ID
     * @see #PARAM_GROUP_ID
     */
    @Method(REQUIRED | WANTS_GROUP_ID)
    public static final MethodKey<FunctionalEndpoint> METHOD_SAVE =
            new MethodKey<>(TRAIT_ID, "save", FunctionalEndpoint.class);

    /**
     * Current scene identifier. When written to, applies the scene to the state. When read, it will
     * return the last state that was loaded if no changes to the state have been made since that
     * time. Otherwise reading will return <code>null</code>.
     */
    @Property(READ_WRITE | REQUIRED | NO_SAVE)
    public static final PropertyKey<String> STAT_SCENE_ID =
            new PropertyKey<>(Section.STATE, TRAIT_ID, "sid", java.lang.String.class);

    /**
     * Group id of the last scene that was set. This property is not cleared when the state is
     * mutated.
     */
    @Property(READ_ONLY | REQUIRED | NO_SAVE)
    public static final PropertyKey<String> STAT_GROUP_ID =
            new PropertyKey<>(Section.STATE, TRAIT_ID, "gid", java.lang.String.class);

    @Property(READ_ONLY)
    public static final PropertyKey<String> CONF_SCENE_ID_POWER_ON =
            new PropertyKey<>(Section.CONFIG, TRAIT_ID, "spor", java.lang.String.class);

    /**
     * Method parameter key for Scene ID.
     *
     * @see #METHOD_SAVE
     */
    public static final ParamKey<String> PARAM_SCENE_ID =
            new ParamKey<>("sid", java.lang.String.class);

    /**
     * Method parameter key for Group ID.
     *
     * @see #METHOD_SAVE
     */
    public static final ParamKey<String> PARAM_GROUP_ID =
            new ParamKey<>("gid", java.lang.String.class);
}
