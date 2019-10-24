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
 * The Energy trait contains properties that relate to the energy consumption of a device. A FE
 * would rarely implement all of the described properties: only the relevant properties would be
 * implemented.
 */
@Trait
public final class EnergyTrait {
    // Prevent instantiation
    private EnergyTrait() {}

    /** Abstract class for implementing trait behavior on a local thing. */
    public abstract static class AbstractLocalTrait extends LocalEnergyTrait {}

    /** The name of this trait */
    public static final String TRAIT_NAME = "Energy";

    /** The URI that identifies the specification used to implement this trait. */
    public static final String TRAIT_URI = "tag:google.com,2018:m2m:traits:energy:v1:v0#r0";

    /** The Short ID of this trait (<code>"enrg"</code>) */
    public static final String TRAIT_ID = "enrg";

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
     * Instantaneous power draw, measured in watts. Unlike the property volt-amps, this property
     * takes into consideration power factor when measuring AC.
     */
    @Property(READ_ONLY)
    public static final PropertyKey<Float> STAT_WATTS =
            new PropertyKey<>(Section.STATE, TRAIT_ID, "watt", java.lang.Float.class);

    /** Instantaneous electric current, measured in amps. */
    @Property(READ_ONLY)
    public static final PropertyKey<Float> STAT_AMPS =
            new PropertyKey<>(Section.STATE, TRAIT_ID, "amps", java.lang.Float.class);

    /** Instantaneous electric potential, measured in volts. */
    @Property(READ_ONLY)
    public static final PropertyKey<Float> STAT_VOLTS =
            new PropertyKey<>(Section.STATE, TRAIT_ID, "volt", java.lang.Float.class);

    /**
     * Apparent instantaneous power draw, measured in volt-amps. Note that this is literally the
     * volts multiplied by the amps, so this will differ if the power factor is anything other than
     * 1.0. Only really meaningful when measuring AC.
     */
    @Property(READ_ONLY)
    public static final PropertyKey<Float> STAT_VOLT_AMPS =
            new PropertyKey<>(Section.STATE, TRAIT_ID, "voam", java.lang.Float.class);

    /** The power factor measured by this FE. Only meaningful when measuring AC. Unitless. */
    @Property(READ_ONLY)
    public static final PropertyKey<Float> STAT_POWER_FACTOR =
            new PropertyKey<>(Section.STATE, TRAIT_ID, "pwft", java.lang.Float.class);

    /**
     * The accumulated power (energy) used over time by the FE, measured in watt-hours. If this
     * thing allows this value to be reset, it can be reset by setting its value to
     * zero or null. Setting to any other value MUST fail.
     */
    @Property(READ_ONLY | RESET)
    public static final PropertyKey<Long> STAT_ENERGY =
            new PropertyKey<>(Section.STATE, TRAIT_ID, "enrg", java.lang.Long.class);

    /**
     * The maximum power that the load is allowed to draw before the FE automatically turns off the
     * load. Set to null to disable. When tripped, {@link BaseTrait#STAT_TRAP} is set to <code>
     * "tag:google.com,2018:m2m:traits:energy:exception#max-watts"</code> until the condition is
     * reset by turning the load on again.
     */
    @Property
    public static final PropertyKey<Float> CONF_MAX_WATTS =
            new PropertyKey<>(Section.CONFIG, TRAIT_ID, "mxwt", java.lang.Float.class);

    /**
     * The maximum apparent power (volt-amps) that the load is allowed to draw before the FE
     * automatically turns off the load. Set to null to disable. This property requires that the
     * OnOff trait also be supported. When tripped, {@link BaseTrait#STAT_TRAP} is set to <code>
     * "tag:google.com,2018:m2m:traits:energy:exception#max-volt-amps"</code> until the condition is
     * reset by turning the load on again.
     */
    @Property
    public static final PropertyKey<Float> CONF_MAX_VOLT_AMPS =
            new PropertyKey<>(Section.CONFIG, TRAIT_ID, "mxva", java.lang.Float.class);

    /**
     * The voltage above which the load will be automatically turned off by the FE. Set to null to
     * disable. When tripped, {@link BaseTrait#STAT_TRAP} is set to <code>
     * "tag:google.com,2018:m2m:traits:energy:exception#max-volts"</code> until the condition is
     * reset by turning the load on again.
     */
    @Property
    public static final PropertyKey<Float> CONF_MAX_VOLTS =
            new PropertyKey<>(Section.CONFIG, TRAIT_ID, "mxvo", java.lang.Float.class);

    /**
     * The voltage below which the load will be automatically turned off by the FE. Set to null to
     * disable. When tripped, {@link BaseTrait#STAT_TRAP} is set to <code>
     * "tag:google.com,2018:m2m:traits:energy:exception#min-volts"</code> until the condition is
     * reset by turning the load on again.
     */
    @Property
    public static final PropertyKey<Float> CONF_MIN_VOLTS =
            new PropertyKey<>(Section.CONFIG, TRAIT_ID, "mnvo", java.lang.Float.class);

    /**
     * The maximum number of amps that the load is allowed to draw before the FE automatically turns
     * off the load. Set to null to disable. When tripped, {@link BaseTrait#STAT_TRAP} is set to
     * <code>"tag:google.com,2018:m2m:traits:energy:exception#max-amps"</code> until the condition
     * is reset by turning the load on again.
     */
    @Property
    public static final PropertyKey<Float> CONF_MAX_AMPS =
            new PropertyKey<>(Section.CONFIG, TRAIT_ID, "mxam", java.lang.Float.class);

    /** The maximum number of watts that the FE is capable of drawing. */
    @Property
    public static final PropertyKey<Float> META_MAX_DRAW_WATTS =
            new PropertyKey<>(
                    Section.METADATA, TRAIT_ID, "mxwt", java.lang.Float.class);

    /** The maximum number of amps that the FE is capable of drawing. */
    @Property
    public static final PropertyKey<Float> META_MAX_DRAW_AMPS =
            new PropertyKey<>(
                    Section.METADATA, TRAIT_ID, "mxam", java.lang.Float.class);
}
