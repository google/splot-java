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
import com.google.iot.m2m.base.Section;

/**
 * Trait for temperature, pressure, humidity, etc. This would typically be used by environmental
 * sensors.
 */
@Trait
public final class AmbientEnvironmentTrait {
    // Prevent instantiation
    private AmbientEnvironmentTrait() {}

    /** Abstract class for implementing trait behavior on a local functional endpoint. */
    public abstract static class AbstractLocalTrait extends LocalAmbientEnvironmentTrait {}

    /** The name of this trait */
    public static final String TRAIT_NAME = "AmbientEnvironment";

    /** The URI that identifies the specification used to implement this trait. */
    public static final String TRAIT_URI =
            "tag:google.com,2018:m2m:traits:ambient-environment:v1:v0#r0";

    /** The Short ID of this trait (<code>"aenv"</code>) */
    public static final String TRAIT_ID = "aenv";

    /** Flag indicating if this trait supports children or not. */
    public static final boolean TRAIT_SUPPORTS_CHILDREN = false;

    /**
     * Property key for the URI that identifies the specification used to implement this trait. This
     * property is present on all traits.
     */
    @Property(READ_ONLY | REQUIRED)
    public static final PropertyKey<String> META_TRAIT_URI =
            new PropertyKey<>(Section.METADATA, TRAIT_ID, "turi", String.class);

    /** Ambient air pressure <em>(Units TBD)</em>. */
    @Property(READ_ONLY)
    public static final PropertyKey<Float> STAT_PRESSURE =
            new PropertyKey<>(Section.STATE, TRAIT_ID, "pres", java.lang.Float.class);

    /** Ambient temperature in °C. */
    @Property(READ_ONLY)
    public static final PropertyKey<Float> STAT_TEMPERATURE =
            new PropertyKey<>(Section.STATE, TRAIT_ID, "temp", java.lang.Float.class);

    /** Relative humidity as a value between 0.0 (0%) and 1.0 (100%). */
    @Property(READ_ONLY)
    public static final PropertyKey<Float> STAT_HUMIDITY =
            new PropertyKey<>(Section.STATE, TRAIT_ID, "humi", java.lang.Float.class);

    /** Ambient light level <em>(Units TBD)</em>. */
    @Property(READ_ONLY)
    public static final PropertyKey<Float> STAT_LIGHT_LEVEL =
            new PropertyKey<>(Section.STATE, TRAIT_ID, "llvl", java.lang.Float.class);
}
