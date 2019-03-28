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
import java.util.Map;

import static com.google.iot.m2m.annotation.Property.READ_ONLY;
import static com.google.iot.m2m.annotation.Property.REQUIRED;

/**
 * Experimental trait for managing automation timers.
 *
 * @see AutomationTimerTrait
 */
@Trait
public final class AutomationTimerManagerTrait {
    // Prevent instantiation
    private AutomationTimerManagerTrait() {}

    /** Abstract class for implementing trait behavior on a local functional endpoint. */
    public abstract static class AbstractLocalTrait extends LocalAutomationTimerManagerTrait {}

    /** The name of this trait */
    public static final String TRAIT_NAME = "AutomationTimerManager";

    /** The URI that identifies the specification used to implement this trait. */
    public static final String TRAIT_URI = "tag:google.com,2018:m2m:traits:timer-manager:v1:v0#r0";

    /** The Short ID of this trait (<code>"tmgr"</code>) */
    public static final String TRAIT_ID = "tmgr";

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
     * Method for creating a new timer.
     *
     * @see #PARAM_AUTO_RESET
     * @see #PARAM_AUTO_DELETE
     * @see #PARAM_DURATION
     * @see #PARAM_ENABLED
     * @see #PARAM_SCHEDULE_PROGRAM
     * @see #PARAM_PREDICATE_PROGRAM
     * @see #PARAM_ACTIONS
     * @see #PARAM_ACTION_BODY
     * @see #PARAM_ACTION_METH
     * @see #PARAM_ACTION_PATH
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
            new ParamKey<>("recy", Boolean.class);

    /**
     * Auto restart flag.
     *
     * @see #METHOD_CREATE
     */
    public static final ParamKey<Boolean> PARAM_AUTO_RESET =
            new ParamKey<>("arst", Boolean.class);

    /**
     * Auto delete flag.
     *
     * @see #METHOD_CREATE
     */
    public static final ParamKey<Boolean> PARAM_AUTO_DELETE =
            new ParamKey<>("adel", Boolean.class);

    /**
     * Enabled flag parameter.
     *
     * @see #METHOD_CREATE
     */
    public static final ParamKey<Boolean> PARAM_ENABLED =
            new ParamKey<>("en", Boolean.class);

    /**
     * Simple timer duration, in seconds. This will set
     * {@link AutomationTimerTrait#CONF_SCHEDULE_PROGRAM} on the resulting timer
     * to return this value as a constant.
     *
     * @see #METHOD_CREATE
     */
    public static final ParamKey<Double> PARAM_DURATION =
            new ParamKey<>("dura", Double.class);

    /**
     * Schedule program.
     *
     * @see #METHOD_CREATE
     */
    public static final ParamKey<String> PARAM_SCHEDULE_PROGRAM =
            new ParamKey<>("schd", String.class);

    /**
     * Predicate program.
     *
     * @see #METHOD_CREATE
     */
    public static final ParamKey<String> PARAM_PREDICATE_PROGRAM =
            new ParamKey<>("pred", String.class);

    /**
     * List of actions. If only one action is needed, use {@link #PARAM_ACTION_PATH},
     * {@link #PARAM_ACTION_METH}, and {@link #PARAM_ACTION_BODY} instead.
     *
     * <p>This parameter takes the same arguments as {@link AutomationTimerTrait#CONF_ACTIONS}.
     *
     * @see #METHOD_CREATE
     * @see AutomationTimerTrait#CONF_ACTIONS
     */
    @SuppressWarnings("unchecked")
    public static final ParamKey<Map<String, Object>[]> PARAM_ACTIONS = new ParamKey("acti", Map[].class);

    /**
     * Path for a single action.
     * If more than one action is needed, use {@link #PARAM_ACTIONS}.
     * @see #METHOD_CREATE
     */
    public static final ParamKey<URI> PARAM_ACTION_PATH = new ParamKey<>("actp", URI.class);

    /**
     * The REST method to use for a single action. Can be one of the following strings:
     * <ul>
     *     <li>{@code "POST"}</li>
     *     <li>{@code "PUT"}</li>
     *     <li>{@code "DELETE"}</li>
     * </ul>
     * If more than one action is needed, use {@link #PARAM_ACTIONS}.
     * @see #METHOD_CREATE
     */
    public static final ParamKey<String> PARAM_ACTION_METH = new ParamKey<>("actm", String.class);

    /**
     * The body to use for a single action.
     * If more than one action is needed, use {@link #PARAM_ACTIONS}.
     * @see #METHOD_CREATE
     */
    public static final ParamKey<Object> PARAM_ACTION_BODY = new ParamKey<>("actb", Object.class);
}
