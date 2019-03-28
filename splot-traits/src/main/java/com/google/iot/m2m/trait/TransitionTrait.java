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
import com.google.iot.m2m.base.PropertyKey;
import com.google.iot.m2m.base.Splot;

/**
 * The Transition trait describes a FE that supports smooth transitions for some of its state
 * properties.
 */
@Trait
public final class TransitionTrait {
    // Prevent instantiation
    private TransitionTrait() {}

    /** Abstract class for implementing trait behavior on a local functional endpoint. */
    public abstract static class AbstractLocalTrait extends LocalTransitionTrait {}

    /** The name of this trait */
    public static final String TRAIT_NAME = "Transition";

    /** The URI that identifies the specification used to implement this trait. */
    public static final String TRAIT_URI = "tag:google.com,2018:m2m:traits:transition:v1:v0#r0";

    /** The Short ID of this trait (<code>"tran"</code>) */
    public static final String TRAIT_ID = "tran";

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
     * Transition duration, in seconds. When updated simultaneously with other state changes, it
     * indicates the duration of the transition between the old state and the specified state. When
     * read it indicates the time remaining in the current transition. The current transition can be
     * halted by setting this to zero. Sometimes physical limitations will force a minimum duration
     * that is longer than specified. The maximum value that is required to be supported is 604800,
     * or one week. The resolution must be at or below one tenth of a second for durations of less
     * than one hour.
     */
    @Property(READ_WRITE | REQUIRED | NO_SAVE)
    public static final PropertyKey<Float> STAT_DURATION =
            new PropertyKey<>(Splot.SECTION_STATE, TRAIT_ID, "d", java.lang.Float.class);

    /**
     * Transition speed, in percentage of maximum speed. This is an alternative to specifying the
     * duration of a transition for functional endpoints where certain properties cannot be
     * physically transitioned faster than a certain speed. The units of this property are a
     * percentage of full speed. The implementation SHOULD allow this parameter to be adjusted as
     * the transition is occurring. This property SHOULD NOT be implemented unless it makes sense
     * for the underlying hardware.
     */
    @Property(READ_WRITE | NO_SAVE)
    public static final PropertyKey<Float> STAT_SPEED =
            new PropertyKey<>(Splot.SECTION_STATE, TRAIT_ID, "sp", java.lang.Float.class);
}
