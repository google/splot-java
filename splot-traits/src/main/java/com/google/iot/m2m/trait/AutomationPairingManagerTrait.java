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

import com.google.iot.m2m.annotation.Method;
import com.google.iot.m2m.annotation.Property;
import com.google.iot.m2m.annotation.Trait;
import com.google.iot.m2m.base.*;

import java.net.URI;

import static com.google.iot.m2m.annotation.Property.*;

/**
 * Experimental trait for managing automation pairings.
 *
 * @see AutomationPairingTrait
 */
@Trait
public final class AutomationPairingManagerTrait {
    // Prevent instantiation
    private AutomationPairingManagerTrait() {}

    /** Abstract class for implementing trait behavior on a local functional endpoint. */
    public abstract static class AbstractLocalTrait extends LocalAutomationPairingManagerTrait {}

    /** The name of this trait */
    public static final String TRAIT_NAME = "AutomationPairingManager";

    /** The URI that identifies the specification used to implement this trait. */
    public static final String TRAIT_URI = "tag:google.com,2018:m2m:traits:pairing-manager:v1:v0#r0";

    /** The Short ID of this trait (<code>"pmgr"</code>) */
    public static final String TRAIT_ID = "pmgr";

    /** Flag indicating if this trait supports children or not. */
    public static final boolean TRAIT_SUPPORTS_CHILDREN = true;

    /**
     * Property key for the URI that identifies the specification used to implement this trait. This
     * property is present on all traits.
     */
    @Property(READ_ONLY | REQUIRED)
    public static final PropertyKey<String> META_TRAIT_URI =
            new PropertyKey<>(Splot.SECTION_METADATA, TRAIT_ID, "turi", String.class);

    /**
     * Method for creating a new pairing.
     *
     * @see #PARAM_RECYCLABLE
     */
    @Method(REQUIRED)
    public static final MethodKey<FunctionalEndpoint> METHOD_CREATE =
            new MethodKey<>(TRAIT_ID, "create", FunctionalEndpoint.class);

    /**
     * Recyclable flag parameter.
     *
     * @see #METHOD_CREATE
     */
    public static final ParamKey<Boolean> PARAM_RECYCLABLE =
            new ParamKey<>("recy", java.lang.Boolean.class);

    /**
     * Push flag parameter.
     *
     * @see #METHOD_CREATE
     */
    public static final ParamKey<Boolean> PARAM_PUSH =
            new ParamKey<>("push", java.lang.Boolean.class);

    /**
     * Push flag parameter.
     *
     * @see #METHOD_CREATE
     */
    public static final ParamKey<Boolean> PARAM_PULL =
            new ParamKey<>("pull", java.lang.Boolean.class);

    /**
     * Enabled flag parameter.
     *
     * @see #METHOD_CREATE
     */
    public static final ParamKey<Boolean> PARAM_ENABLED =
            new ParamKey<>("en", java.lang.Boolean.class);

    /**
     * Source parameter.
     *
     * @see #METHOD_CREATE
     */
    public static final ParamKey<URI> PARAM_SOURCE =
            new ParamKey<>("src", URI.class);

    /**
     * Destination parameter.
     *
     * @see #METHOD_CREATE
     */
    public static final ParamKey<URI> PARAM_DESTINATION =
            new ParamKey<>("dst", URI.class);

    /**
     * Forward Transform
     *
     * @see #METHOD_CREATE
     */
    public static final ParamKey<String> PARAM_FORWARD_TRANSFORM =
            new ParamKey<>("xfwd", String.class);

    /**
     * Reverse Transform
     *
     * @see #METHOD_CREATE
     */
    public static final ParamKey<String> PARAM_REVERSE_TRANSFORM =
            new ParamKey<>("xrev", String.class);
}
