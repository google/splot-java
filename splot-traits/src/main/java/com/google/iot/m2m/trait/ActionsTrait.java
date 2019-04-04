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
import com.google.iot.m2m.base.MethodKey;
import com.google.iot.m2m.base.ParamKey;
import com.google.iot.m2m.base.PropertyKey;
import com.google.iot.m2m.base.Splot;

import java.net.URI;
import java.util.Map;

import static com.google.iot.m2m.annotation.Property.*;

/**
 * Experimental trait for functional endpoints that trigger actions, such as rules and timers.
 */
@Trait
public final class ActionsTrait {
    // Prevent instantiation
    private ActionsTrait() {}

    /** Abstract class for implementing trait behavior on a local functional endpoint. */
    public abstract static class AbstractLocalTrait extends LocalActionsTrait {}

    /** The name of this trait */
    public static final String TRAIT_NAME = "Actions";

    /** The URI that identifies the specification used to implement this trait. */
    public static final String TRAIT_URI = "tag:google.com,2018:m2m:traits:actions:v1:v0#r0";

    /** The Short ID of this trait (<code>"actn"</code>) */
    public static final String TRAIT_ID = "actn";

    /** Flag indicating if this trait supports children or not. */
    public static final boolean TRAIT_SUPPORTS_CHILDREN = false;

    /**
     * Property key for the URI that identifies the specification used to implement this trait. This
     * property is present on all traits.
     */
    @Property(READ_ONLY | REQUIRED)
    public static final PropertyKey<String> META_TRAIT_URI =
            new PropertyKey<>(Splot.SECTION_METADATA, TRAIT_ID, "turi", String.class);

    /**
     * The number of times this automation has "fired". This count may be reset by writing zero.
     * Writing any other value results in an error. The count is generally not volatile and is
     * lost after a power cycle.
     */
    @Property(READ_ONLY | RESET)
    public static final PropertyKey<Integer> STAT_COUNT =
            new PropertyKey<>(Splot.SECTION_STATE, TRAIT_ID, "c", Integer.class);

    /**
     * The number of seconds ago that the actions last fired. This value is not cacheable.
     * Observing it will only indicate changes to "zero".
     */
    @Property(READ_ONLY)
    public static final PropertyKey<Integer> STAT_LAST =
            new PropertyKey<>(Splot.SECTION_STATE, TRAIT_ID, "last", Integer.class);

    /**
     * Actions to perform when this automation fires.
     * <p>Each criteria is defined as a map keyed by strings. The string keys are the following:
     *
     * <ul>
     *   <li>({@link #PARAM_ACTION_PATH}): URL or absolute path to perform an action on
     *   <li>({@link #PARAM_ACTION_SKIP}): True if this action should be skipped.
     *   <li>({@link #PARAM_ACTION_DESC}): Human-readable description of the action
     *   <li>({@link #PARAM_ACTION_METH}): The REST method to perform on the path
     *   <li>({@link #PARAM_ACTION_BODY}): The body of the action
     *   <li>({@link #PARAM_ACTION_SYNC}): If this action should complete before the next action
     * </ul>
     */
    @Property(READ_WRITE | REQUIRED)
    @SuppressWarnings("unchecked")
    public static final PropertyKey<Map<String, Object>[]> CONF_ACTIONS =
            new PropertyKey(Splot.SECTION_CONFIG, TRAIT_ID, "acti", Map[].class);

    /**
     * Path for action.
     * @see #CONF_ACTIONS
     */
    public static final ParamKey<URI> PARAM_ACTION_PATH = new ParamKey<>("p", URI.class);

    /**
     * The REST method to use for the action. Can be one of the following strings:
     * <ul>
     *     <li>{@code "POST"}</li>
     *     <li>{@code "PUT"}</li>
     *     <li>{@code "DELETE"}</li>
     * </ul>
     * @see #CONF_ACTIONS
     */
    public static final ParamKey<String> PARAM_ACTION_METH = new ParamKey<>("m", String.class);

    /**
     * The body to use for the action.
     * @see #CONF_ACTIONS
     */
    public static final ParamKey<Object> PARAM_ACTION_BODY = new ParamKey<>("b", Object.class);

    /**
     * The <a href="https://tools.ietf.org/html/rfc7252#section-12.3">CoAP content-format</a> to
     * use for rendering the body when performing the action.
     *
     * @see #CONF_ACTIONS
     * @see <a href="https://tools.ietf.org/html/rfc7252#section-12.3">RFC7252 Section 12.3</a>
     * @see <a href="https://goo.gl/ZHfEs6">IANA CoAP Content-Format Registry</a>
     */
    public static final ParamKey<Integer> PARAM_ACTION_CONTENT_TYPE = new ParamKey<>("ct", Integer.class);

    /**
     * Flag indicating if this action should be skipped. If absent, it is assumed to be false.
     * @see #CONF_ACTIONS
     */
    public static final ParamKey<Boolean> PARAM_ACTION_SKIP = new ParamKey<>("s", Boolean.class);

    /**
     * Human readable description of the action. Optional.
     * @see #CONF_ACTIONS
     */
    public static final ParamKey<String> PARAM_ACTION_DESC = new ParamKey<>("c", String.class);

    /**
     * Determines if this action should block execution or not.
     *
     * The parameter can have one of three possible values:
     *
     * <ul>
     *     <li>{@code 0} ({@link #SYNC_DO_NOT_WAIT}): Trigger this action asynchronously. The next
     *         action is scheduled to happen ASAP. This is the default behavior.</li>
     *     <li>{@code 1} ({@link #SYNC_WAIT_TO_FINISH}): Trigger this action synchronously. The
     *         next action is scheduled to happen once this action is complete, either with success
     *         or failure.</li>
     *     <li>{@code 2} ({@link #SYNC_STOP_ON_ERROR}): Trigger this action synchronously, stopping
     *         on error. The next action is scheduled to happen once this action has completed
     *         successfully, otherwise later actions are canceled.</li>
     * </ul>
     * @see #CONF_ACTIONS
     */
    public static final ParamKey<Integer> PARAM_ACTION_SYNC = new ParamKey<>("b", Integer.class);

    /**
     * Do not wait for action to finish before processing other actions.
     *
     * @see #PARAM_ACTION_SYNC
     */
    public static int SYNC_DO_NOT_WAIT = 0;

    /**
     * Wait for this action to finish (or fail) before processing other actions.
     *
     * @see #PARAM_ACTION_SYNC
     */
    public static int SYNC_WAIT_TO_FINISH = 1;

    /**
     * Stop processing actions if this action fails.
     * @see #PARAM_ACTION_SYNC
     */
    public static int SYNC_STOP_ON_ERROR = 2;

    /**
     * {@code GET} RESTful method.
     *
     * @see #PARAM_ACTION_METH
     */
    public static final String METHOD_GET = "GET";

    /**
     * {@code PUT} RESTful method.
     *
     * @see #PARAM_ACTION_METH
     */
    public static final String METHOD_PUT = "PUT";

    /**
     * {@code POST} RESTful method.
     *
     * @see #PARAM_ACTION_METH
     */
    public static final String METHOD_POST = "POST";

    /**
     * {@code DELETE} RESTful method.
     *
     * @see #PARAM_ACTION_METH
     */
    public static final String METHOD_DELETE = "DELETE";

}
