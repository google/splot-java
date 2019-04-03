/*
 * Copyright (C) 2019 Google Inc.
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
 * Experimental trait for managing automation rules.
 *
 * @see AutomationRuleTrait
 */
@Trait
public final class AutomationRuleManagerTrait {
    // Prevent instantiation
    private AutomationRuleManagerTrait() {}

    /** Abstract class for implementing trait behavior on a local functional endpoint. */
    public abstract static class AbstractLocalTrait extends LocalAutomationRuleManagerTrait {}

    /** The name of this trait */
    public static final String TRAIT_NAME = "AutomationRuleManager";

    /** The URI that identifies the specification used to implement this trait. */
    public static final String TRAIT_URI = "tag:google.com,2018:m2m:traits:rule-manager:v1:v0#r0";

    /** The Short ID of this trait (<code>"rmgr"</code>) */
    public static final String TRAIT_ID = "rmgr";

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
     * Method for creating a new rule.
     *
     * @see #PARAM_ENABLED
     * @see #PARAM_CONDITIONS
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
     * Enabled flag parameter.
     *
     * @see #METHOD_CREATE
     */
    public static final ParamKey<Boolean> PARAM_ENABLED =
            new ParamKey<>("en", Boolean.class);


    /**
     * Name of the created rule.
     *
     * @see #METHOD_CREATE
     */
    public static final ParamKey<String> PARAM_NAME = new ParamKey<>("name", String.class);

    /**
     * Match
     *
     * @see #METHOD_CREATE
     * @see AutomationRuleTrait#CONF_MATCH
     * @see AutomationRuleTrait#MATCH_ANY
     * @see AutomationRuleTrait#MATCH_ALL
     */
    public static final ParamKey<String> PARAM_MATCH = new ParamKey<>("mtch", String.class);

    /**
     * List of conditions.
     *
     * <p>This parameter takes the same arguments as {@link AutomationRuleTrait#CONF_CONDITIONS}.
     *
     * @see #METHOD_CREATE
     * @see AutomationRuleTrait#PARAM_COND_PATH
     * @see AutomationRuleTrait#PARAM_COND_EXPR
     * @see AutomationRuleTrait#PARAM_COND_SKIP
     * @see AutomationRuleTrait#PARAM_COND_DESC
     */
    @SuppressWarnings("unchecked")
    public static final ParamKey<Map<String, Object>[]> PARAM_CONDITIONS = new ParamKey("cond", Map[].class);

    /**
     * List of actions. If only one action is needed, use {@link #PARAM_ACTION_PATH},
     * {@link #PARAM_ACTION_METH}, and {@link #PARAM_ACTION_BODY} instead.
     *
     * <p>This parameter takes the same arguments as {@link ActionsTrait#CONF_ACTIONS}.
     *
     * @see #METHOD_CREATE
     * @see ActionsTrait#CONF_ACTIONS
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
