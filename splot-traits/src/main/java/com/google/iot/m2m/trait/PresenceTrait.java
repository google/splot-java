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

/** Presence detection trait. Used for motion sensors, pressure mats, etc. */
@Trait
public final class PresenceTrait {
    // Prevent instantiation
    private PresenceTrait() {}

    /** Abstract class for implementing trait behavior on a local functional endpoint. */
    public abstract static class AbstractLocalTrait extends LocalPresenceTrait {}

    /** The name of this trait */
    public static final String TRAIT_NAME = "Presence";

    /** The URI that identifies the specification used to implement this trait. */
    public static final String TRAIT_URI = "tag:google.com,2018:m2m:traits:presence:v1:v0#r0";

    /** The Short ID of this trait (<code>"pres"</code>) */
    public static final String TRAIT_ID = "pres";

    /** Flag indicating if this trait supports children or not. */
    public static final boolean TRAIT_SUPPORTS_CHILDREN = false;

    /**
     * Property key for the URI that identifies the specification used to implement this trait. This
     * property is present on all traits.
     */
    @Property(READ_ONLY | REQUIRED)
    public static final PropertyKey<String> META_TRAIT_URI =
            new PropertyKey<>(PropertyKey.SECTION_METADATA, TRAIT_ID, "turi", String.class);

    /** Indicator of presence. True if presence is detected, false if presence is not detected. */
    @Property(READ_WRITE | GET_REQUIRED)
    public static final PropertyKey<Boolean> STAT_VALUE =
            new PropertyKey<>(PropertyKey.SECTION_STATE, TRAIT_ID, "v", Boolean.class);
}
