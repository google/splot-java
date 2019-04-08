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
import com.google.iot.m2m.base.InvalidPropertyValueException;
import com.google.iot.m2m.base.PropertyKey;
import com.google.iot.m2m.base.Section;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * The Level trait describes an FE that can have its level adjusted, such as the brightness of a
 * light, or the amount that a window should be opened. It is also used on controllers for these
 * types of devices.
 */
@Trait
public final class LevelTrait {
    private LevelTrait() {}

    /** Abstract class for implementing trait behavior on a local functional endpoint. */
    @SuppressWarnings("RedundantThrows")
    public abstract static class AbstractLocalTrait extends LocalLevelTrait {
        @Override
        @Nullable
        public Float onSanitizeValue(@Nullable Float value) throws InvalidPropertyValueException {
            if (value == null) {
                return 0.0f;
            }
            return Math.max(Math.min(value, 1.0f), 0.0f);
        }
    }

    /** The name of this trait */
    public static final String TRAIT_NAME = "Level";

    /** The URI that identifies the specification used to implement this trait. */
    public static final String TRAIT_URI = "tag:google.com,2018:m2m:traits:level:v1:v0#r0";

    /** The Short ID of this trait (<code>"levl"</code>) */
    public static final String TRAIT_ID = "levl";

    /** Flag indicating if this trait supports children or not. */
    public static final boolean TRAIT_SUPPORTS_CHILDREN = false;

    /**
     * Property key for the URI that identifies the specification used to implement this trait. This
     * property is present on all traits.
     */
    @Property(READ_ONLY | REQUIRED)
    public static final PropertyKey<String> META_TRAIT_URI =
            new PropertyKey<>(Section.METADATA, TRAIT_ID, "turi", String.class);

    /**
     * Describes the "level" of a functional endpoint. The level is encoded as a floating-point
     * value between 0.0 and 1.0. The exact meaning of this value is dependent on the type of
     * device, but in general the value 0.0 represents one extreme state, the value 1.0 represents
     * the opposite extreme state, and the values between those two represent a perceptually uniform
     * distribution between those two states. When paired with the {@link OnOffTrait}, the value 0.0
     * is intended to be closest to the off state that isn't actually off.
     *
     * <p>Some things to note:
     *
     * <p>
     *
     * <ul>
     *   <li>With the exception of physical actuators, perceptual uniformity is generally not
     *       linear. For example, reducing the light output by 50% will only reduce perceived light
     *       output by around 25%.
     *   <li>If this trait is paired with an OnOff trait, then setting the level to 0.0 will likely
     *       behave differently than if the OnOff trait was turned off.
     * </ul>
     */
    @Property(READ_WRITE | GET_REQUIRED)
    public static final PropertyKey<Float> STAT_VALUE =
            new PropertyKey<>(Section.STATE, TRAIT_ID, "v", java.lang.Float.class);
}
