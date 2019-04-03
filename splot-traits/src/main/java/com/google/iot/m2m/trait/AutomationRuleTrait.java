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

import static com.google.iot.m2m.annotation.Property.*;

import com.google.iot.m2m.annotation.Property;
import com.google.iot.m2m.annotation.Trait;
import com.google.iot.m2m.base.ParamKey;
import com.google.iot.m2m.base.PropertyKey;
import com.google.iot.m2m.base.Splot;

import java.net.URI;
import java.util.Map;

/**
 * Experimental trait representing an automation rule. An automation rule allows you to create
 * if-this-then-that style relationships across functional endpoints that are associated with the
 * same {@link com.google.iot.m2m.base.Technology} instance.
 *
 * <p>This trait is typically used with {@link ActionsTrait}.
 * @see ActionsTrait
 */
@Trait
public final class AutomationRuleTrait {
    // Prevent instantiation
    private AutomationRuleTrait() {}

    /** Abstract class for implementing trait behavior on a local functional endpoint. */
    public abstract static class AbstractLocalTrait extends LocalAutomationRuleTrait {}

    /** The name of this trait */
    public static final String TRAIT_NAME = "AutomationRule";

    /** The URI that identifies the specification used to implement this trait. */
    public static final String TRAIT_URI = "tag:google.com,2018:m2m:traits:rule:v1:v0#r0";

    /** The Short ID of this trait (<code>"rule"</code>) */
    public static final String TRAIT_ID = "rule";

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
     * Criteria table for determining when the action should fire. All of the given criteria must be
     * satisfied for the action to fire.
     *
     * <p>Each criteria is defined as a map keyed by strings. The string keys are the following:
     *
     * <ul>
     *   <li><code>p</code> ({@link #PARAM_COND_PATH}): URL or absolute path to the resource being evaluated
     *   <li><code>c</code> ({@link #PARAM_COND_EXPR}): RPN condition to evaluate.
     *   <li><code>s</code> ({@link #PARAM_COND_SKIP}): True if this condition should be skipped.
     *   <li><code>d</code> ({@link #PARAM_COND_DESC}): Human-readable description of the criteria
     * </ul>
     *
     * If a <code>path</code> is present, then this value is observed. When the observed path
     * changes, it's previous value is pushed onto the stack, followed by the just observed new
     * value. <code>cond</code> is then evaluated. After evaluation, if the top-most item on the
     * stack is greater than or equal to 0.5, then the condition is considered satisfied.
     *
     * <p>Once all conditions are considered satisfied, the action fires.
     *
     * <p>If the path is empty, the value "1.0" is passed onto the stack.
     *
     * <p>Some technologies may have strict requirements on how the condition string is formatted,
     * since not all technologies directly support evaluating arbitrary RPN expressions.
     *
     * <p>In the RPN evaluation context, the following additional operators are available:
     *
     * <ul>
     *   <li><code>rtc.tod</code>: Pushes Time of day, in hours. 0.000 - 23.999.
     *   <li><code>rtc.dow</code>: Pushes Day of week. One-based Integer, 1-7. Monday is 1, Sunday
     *       is 7.
     *   <li><code>rtc.dom</code>: Pushes Day of month. One-based Integer, 1-30.
     *   <li><code>rtc.moy</code>: Pushes Current month. One-based Integer, 1 = January, 12 =
     *       December
     *   <li><code>rtc.dim</code>: Pushes Number of times this weekday has happened this month.
     *       One-based Integer.
     *   <li><code>rtc.wom</code>: Pushes Week of month. One-based Integer.
     *   <li><code>rtc.woy</code>: Pushes Week of year. One-based Integer, 1-52. Week starts on
     *       monday.
     *   <li><code>rtc.wss</code>: Indicates that future instances of <code>rtc.woy</code> and
     *       <code>rtc.dow</code> should be calculated with the week starting on Sunday. Otherwise,
     *       they will assume the week starts on Monday. Pushes nothing to the stack.
     *   <li><code>rtc.utc</code>: Indicates that all time-based operators should use UTC instead of
     *       local time. Otherwise, all time-based operators use local time. Pushes nothing to the
     *       stack.
     *   <li><code>dt_dx</code>: Pushes seconds since the path was last changed, max value if never.
     *   <li><code>dt_cs</code>: Pushes seconds since this condition was last satisfied, max value
     *       if never.
     *   <li><code>dt_rt</code>: Pushes seconds since the enclosing rule was last triggered, max
     *       value if never.
     * </ul>
     */
    @Property(READ_WRITE | REQUIRED)
    @SuppressWarnings("unchecked")
    public static final PropertyKey<Map<String, Object>[]> CONF_CONDITIONS =
            new PropertyKey(Splot.SECTION_CONFIG, TRAIT_ID, "cond", java.util.Map[].class);


    @Property(READ_WRITE | REQUIRED)
    @SuppressWarnings("unchecked")
    public static final PropertyKey<String> CONF_MATCH =
            new PropertyKey(Splot.SECTION_CONFIG, TRAIT_ID, "mtch", String.class);

    public static final String MATCH_ALL = "all";

    public static final String MATCH_ANY = "any";

    /**
     * Path for condition. Optional.
     * @see #CONF_CONDITIONS
     */
    public static final ParamKey<URI> PARAM_COND_PATH = new ParamKey<>("p", URI.class);

    /**
     * Expression for evaluating if the condition is satisfied.
     * @see #CONF_CONDITIONS
     */
    public static final ParamKey<String> PARAM_COND_EXPR = new ParamKey<>("c", String.class);

    /**
     * Flag indicating if this condition should be skipped. If absent, it is assumed to be false.
     * @see #CONF_CONDITIONS
     */
    public static final ParamKey<Boolean> PARAM_COND_SKIP = new ParamKey<>("s", Boolean.class);

    /**
     * Human readable description of the rule. Optional.
     * @see #CONF_CONDITIONS
     */
    public static final ParamKey<String> PARAM_COND_DESC = new ParamKey<>("c", String.class);
}
