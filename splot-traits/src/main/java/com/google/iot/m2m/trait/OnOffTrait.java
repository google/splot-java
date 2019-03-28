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
 * The OnOff trait describes a FE that can be turned on or off, such as a light bulb or a power
 * controller.
 */
@Trait
public final class OnOffTrait {
    // Prevent instantiation
    private OnOffTrait() {}

    /** Abstract class for implementing trait behavior on a local functional endpoint. */
    public abstract static class AbstractLocalTrait extends LocalOnOffTrait {}

    /** The name of this trait */
    public static final String TRAIT_NAME = "OnOff";

    /** The URI that identifies the specification used to implement this trait. */
    public static final String TRAIT_URI = "tag:google.com,2018:m2m:traits:onoff:v1:v0#r0";

    /** The Short ID of this trait (<code>"onof"</code>) */
    public static final String TRAIT_ID = "onof";

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
     * On/Off state. On is <code>true</code>, off is <code>false</code>. A value of <code>null
     *  </code> may indicate an indeterminate state.
     */
    @Property(READ_WRITE | GET_REQUIRED)
    public static final PropertyKey<Boolean> STAT_VALUE =
            new PropertyKey<>(Splot.SECTION_STATE, TRAIT_ID, "v", java.lang.Boolean.class);

    /**
     * Default off duration. Indicates the default duration (in seconds) when transitioning from the
     * on state to the off state. This property is only present on functional endpoints which also
     * have the {@link TransitionTrait}.
     */
    @Property
    public static final PropertyKey<Float> CONF_DURATION_OFF =
            new PropertyKey<>(Splot.SECTION_CONFIG, TRAIT_ID, "doff", java.lang.Float.class);

    /**
     * Default on duration. Indicates the default duration (in seconds) when transitioning from the
     * off state to the on state. This property is only present on functional endpoints which also
     * have the {@link TransitionTrait}.
     */
    @Property
    public static final PropertyKey<Float> CONF_DURATION_ON =
            new PropertyKey<>(Splot.SECTION_CONFIG, TRAIT_ID, "don", java.lang.Float.class);

    /**
     * Power-on scene. Indicates the scene to recall when the device is physically powered on or
     * rebooted. When <code>null</code>, a reboot will restore the previous state. On some types of
     * devices this may be read-only.
     */
    @Property
    public static final PropertyKey<String> CONF_SCENE_ID_ON =
            new PropertyKey<>(Splot.SECTION_CONFIG, TRAIT_ID, "scon", java.lang.String.class);

    /**
     * Functional-endpoint-is-a-light flag. This property allows a functional endpoint that controls
     * a generic load (Like a smart power switch) to be explicitly identified as controlling a
     * light. If this is set to true, this functional endpoint will be included in the “lights”
     * group. If this functional endpoint contains the light trait, then this FE is already assumed
     * to be a light and this property MUST NOT be present.
     */
    @Property
    public static final PropertyKey<Boolean> CONF_IS_LIGHT =
            new PropertyKey<>(
                    Splot.SECTION_CONFIG, TRAIT_ID, "islt", java.lang.Boolean.class);
}
