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

import static com.google.iot.m2m.annotation.Property.*;

/**
 * Experimental trait representing a timer or schedule. An automation timer allows you to trigger
 * events to occur at certain times. They may be repeating or restartable.
 *
 * <p>This trait is typically used with {@link ActionsTrait}.
 * @see ActionsTrait
 */
@Trait
public final class AutomationTimerTrait {
    // Prevent instantiation
    private AutomationTimerTrait() {}

    /** Abstract class for implementing trait behavior on a local functional endpoint. */
    public abstract static class AbstractLocalTrait extends LocalAutomationTimerTrait {}

    /** The name of this trait */
    public static final String TRAIT_NAME = "AutomationTimer";

    /** The URI that identifies the specification used to implement this trait. */
    public static final String TRAIT_URI = "tag:google.com,2018:m2m:traits:timer:v1:v0#r0";

    /** The Short ID of this trait (<code>"timr"</code>) */
    public static final String TRAIT_ID = "timr";

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
     * The number of seconds until the timer fires next. This value is not cacheable.
     * A value of zero indicates that this timer has fired and is not running.
     */
    @Property(READ_ONLY)
    public static final PropertyKey<Float> STAT_NEXT =
            new PropertyKey<>(Splot.SECTION_STATE, TRAIT_ID, "next", Float.class);

    /**
     * Flag indicating if the timer is running or not. Setting this to false will disarm the timer.
     * Setting this to true will (re)arm the timer.
     *
     * <p>When arming with {@link #CONF_SCHEDULE_PROGRAM} specified, {@link #STAT_NEXT} will be
     * recalculated. If {@link #CONF_SCHEDULE_PROGRAM} is empty, then the timer will
     * resume from its previous remaining time.
     *
     * <p>If the timer is already running, setting this property to true will do nothing.
     */
    @Property(READ_WRITE | REQUIRED)
    public static final PropertyKey<Boolean> STAT_RUNNING =
            new PropertyKey<>(Splot.SECTION_STATE, TRAIT_ID, "run", Boolean.class);

    /**
     * Schedule program. This RPN expression is used to calculate the number of seconds until
     * the next fire time.
     * <p>If the expression returns a value that is not a positive number the timer is stopped.
     *
     * <p>Some implementations may strictly limit how this program is structured.
     *
     * <h2>Available variables</h2>
     *
     * <ul>
     *     <li>{@code c}: The value of {@link ActionsTrait#STAT_COUNT}</li>
     *   <li>{@code rtc.tod}: Pushes Time of day, in hours. 0.000 - 23.999.
     *   <li>{@code rtc.dow}: Pushes Day of week. 0-6. Monday is 0, Sunday
     *       is 6.
     *   <li>{@code rtc.dom}: Pushes Day of month. Zero-based Integer, 0-30.
     *   <li>{@code rtc.moy}: Pushes Current month. zero-based Integer, 0 = January, 11 =
     *       December
     *   <li>{@code rtc.awm}: Aligned week of month. Pushes Number of times this weekday has
     *   happened this month. zero-based Integer.
     *   <li>{@code rtc.wom}: Pushes Week of month. zero-based Integer.
     *   <li>{@code rtc.woy}: Pushes Week of year. zero-based Integer, 0-51. Week starts on
     *       monday.
     *   <li>{@code rtc.y}: Pushes gregorian year.
     * </ul>
     *
     * <h2>Additional functions/operators/flags</h2>
     * <ul>
     *   <li>{@code rtc.wss}: Indicates that future instances of {@code rtc.woy} and
     *       {@code rtc.dow} should be calculated with the week starting on Sunday. Otherwise,
     *       they will assume the week starts on Monday. Pushes nothing to the stack.
     *   <li>{@code rtc.utc}: Indicates that all time-based operators should use UTC instead of
     *       local time. Otherwise, all time-based operators use local time. Pushes nothing to the
     *       stack.
     *     <li>{@code H>S}: Convert hours to seconds</li>
     *     <li>{@code D>S}: Convert days to seconds</li>
     * </ul>
     *
     * <h2>Examples</h2>
     * <ul>
     *     <li>{@code 12 rtc.tod - 24 % H>S}: Every day at noon</li>
     *     <li>{@code 13.5 rtc.tod - 24 % H>S}: Every day at 1:30 PM</li>
     *     <li>{@code 12 rtc.tod - 24 % H>S 1 rtc.dow - 7 % D>S +}: Every tuesday at noon</li>
     *     <li>{@code 20 RND 2 * + rtc.tod - 24 % H>S}: Every day at some random time between 8pm and 10pm</li>
     * </ul>
     */
    @Property(READ_WRITE | REQUIRED)
    public static final PropertyKey<String> CONF_SCHEDULE_PROGRAM =
            new PropertyKey<>(Splot.SECTION_CONFIG, TRAIT_ID, "schd", String.class);

    /**
     * Predicate program. This program is evaluated whenever the timer expires. If the
     * predicate evaluates to true, then the actions are fired. If it evaluates to false,
     * then the timer is reset, and {@link ActionsTrait#STAT_COUNT} is not incremented. The intent of
     * the predicate is to allow for complex schedules to be implemented, such as
     * "every second tuesday of the month at noon" or "Every easter at 9am".
     *
     * <p>Some implementations may strictly limit how this program is structured.
     *
     * <h2>Available variables</h2>
     *
     * <ul>
     *     <li>{@code c}: The value of {@link ActionsTrait#STAT_COUNT}</li>
     *   <li>{@code rtc.tod}: Pushes Time of day, in hours. 0.000 - 23.999.
     *   <li>{@code rtc.dow}: Pushes Day of week. 0-6. Monday is 0, Sunday
     *       is 6.
     *   <li>{@code rtc.dom}: Pushes Day of month. Zero-based Integer, 0-30.
     *   <li>{@code rtc.moy}: Pushes Current month. zero-based Integer, 0 = January, 11 =
     *       December
     *   <li>{@code rtc.awm}: Aligned week of month. Pushes Number of times this weekday has
     *   happened this month. zero-based Integer.
     *   <li>{@code rtc.wom}: Pushes Week of month. zero-based Integer.
     *   <li>{@code rtc.woy}: Pushes Week of year. zero-based Integer, 0-51. Week starts on
     *       monday.
     *   <li>{@code rtc.y}: Pushes gregorian year.
     * </ul>
     *
     * <p>If the RTC has not been set (or is not present) then none of the 'rtc' variables
     * are present.</p>
     *
     * <h2>Additional functions/operators/flags</h2>
     * <ul>
     *   <li>{@code rtc.wss}: Indicates that future instances of {@code rtc.woy} and
     *       {@code rtc.dow} should be calculated with the week starting on Sunday. Otherwise,
     *       they will assume the week starts on Monday. Pushes nothing to the stack.
     *   <li>{@code rtc.utc}: Indicates that all time-based operators should use UTC instead of
     *       local time. Otherwise, all time-based operators use local time. Pushes nothing to the
     *       stack.
     *     <li>{@code H>S}: Convert hours to seconds</li>
     *     <li>{@code D>S}: Convert days to seconds</li>
     * </ul>
     *
     * <h2>Examples</h2>
     * <ul>
     *     <li>{@code 2 rtc.dow == 1 rtc.awm == &&}: Only fire on the second Wednesday of the month.</li>
     *     <li>{@code 0.2 RNG &gt;}: Only fire 20% of the time.</li>
     *     <li>{@code 1 rtc.moy == 28 rtc.dom &&}: Only fire on leap day</li>
     * </ul>
     */
    @Property
    public static final PropertyKey<String> CONF_PREDICATE_PROGRAM =
            new PropertyKey<>(Splot.SECTION_CONFIG, TRAIT_ID, "pred", String.class);

    /**
     * Auto restart flag. If this flag is true, then the timer will automatically restart after
     * firing. It will continue to run until it is explicitly stopped or until the schedule program
     * fails to return a positive number.
     */
    @Property(READ_WRITE | REQUIRED)
    public static final PropertyKey<Boolean> CONF_AUTO_RESET =
            new PropertyKey<>(
                    Splot.SECTION_CONFIG, TRAIT_ID, "arst", Boolean.class);

    /**
     * Auto delete flag.
     *
     * <p>If this flag is true, then the timer will automatically delete itself
     * once {@link #STAT_RUNNING} <em>automatically</em> transitions from true to false. Explicitly
     * setting {@link #STAT_RUNNING} to false will NOT cause the timer to be deleted.
     *
     * <p>Thus, it does make sense to have cases where both this flag and {@code #CONF_AUTO_RESET}
     * are both true. In such a case, deletion will be triggered by {@link #CONF_SCHEDULE_PROGRAM}
     * returning a non-positive value.
     */
    @Property(READ_WRITE | REQUIRED)
    public static final PropertyKey<Boolean> CONF_AUTO_DELETE =
            new PropertyKey<>(
                    Splot.SECTION_CONFIG, TRAIT_ID, "adel", Boolean.class);

    /**
     * Method for resetting the timer. Calling this method will always restart the timer,
     * even if the timer is already running.
     */
    @Method(REQUIRED)
    public static final MethodKey<Float> METHOD_RESET =
            new MethodKey<>(TRAIT_ID, "reset", Float.class);
}
