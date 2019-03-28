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

import static com.google.iot.m2m.annotation.Property.READ_ONLY;
import static com.google.iot.m2m.annotation.Property.REQUIRED;

import com.google.iot.m2m.annotation.Property;
import com.google.iot.m2m.annotation.Trait;
import com.google.iot.m2m.base.PropertyKey;
import com.google.iot.m2m.base.Splot;

/**
 * The Battery trait is used on Functional Endpoints which are backed by a battery.
 *
 * <p>All of the properties in this trait are optional, but some properties have defined
 * relationships with other properties that, if present, should be maintained.
 *
 * <p>Some functional endpoints may simply adopt this trait and implement none of the properties,
 * simply to indicate that the functional endpoint is battery-powered. Others might only want to
 * indicate if the battery is low, but offer no additional information about the charge level or
 * capacity.
 *
 * <p>On the other hand, some functional endpoints might implement most of these properties,
 * providing a rich amount of detail on the overall state and health of the battery.
 */
@Trait
public final class BatteryTrait {
    // Prevent instantiation
    private BatteryTrait() {}

    /** Abstract class for implementing trait behavior on a local functional endpoint. */
    public abstract static class AbstractLocalTrait extends LocalBatteryTrait {}

    /** The name of this trait */
    public static final String TRAIT_NAME = "Battery";

    /** The URI that identifies the specification used to implement this trait. */
    public static final String TRAIT_URI = "tag:google.com,2018:m2m:traits:battery:v1:v0#r0";

    /** The Short ID of this trait (<code>"batt"</code>) */
    public static final String TRAIT_ID = "batt";

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
     * Remaining battery charge, as a floating-point value from zero (empty) to one (full). This
     * field is optional, but highly recommended. If implemented, the value of this field is defined
     * to be the following:
     *
     * <ul>
     *   <li>Primary: <code>energyRemaining</code> divided by <code>energyCapacity</code>
     *   <li>Rechargeable: <code>energyRemaining</code> divided by the product of <code>
     *       capacityRemaining</code> and <code>energyCapacity</code>.
     * </ul>
     */
    @Property(READ_ONLY)
    public static final PropertyKey<Float> STAT_CHARGE_REMAINING =
            new PropertyKey<>(Splot.SECTION_STATE, TRAIT_ID, "vpct", java.lang.Float.class);

    /**
     * Energy remaining, in milliwatt-hours.
     *
     * <p>If implemented, its value is defined to be the following:
     *
     * <ul>
     *   <li>Primary: Product of {@link #STAT_CHARGE_REMAINING} and {@link #META_ENERGY_CAPACITY}
     *   <li>Rechargeable: Product of {@link #STAT_CHARGE_REMAINING}, {@link
     *       #STAT_CAPACITY_REMAINING}, and {@link #STAT_CHARGE_REMAINING}
     * </ul>
     *
     * The maximum energy remaining value for this interface is around 2 megawatt-hours.
     */
    @Property(READ_ONLY)
    public static final PropertyKey<Integer> STAT_ENERGY_REMAINING =
            new PropertyKey<>(Splot.SECTION_STATE, TRAIT_ID, "vnrg", java.lang.Integer.class);

    /**
     * Battery needs service indicator. True if the battery needs to be serviced, false otherwise.
     * For example, this would become "true" if the battery was considered low.
     */
    @Property(READ_ONLY)
    public static final PropertyKey<Boolean> STAT_NEEDS_SERVICE =
            new PropertyKey<>(Splot.SECTION_STATE, TRAIT_ID, "sreq", java.lang.Boolean.class);

    /**
     * Rechargable battery state. String describing the current state of the battery:
     *
     * <ul>
     *   <li><code>charged</code>: Battery is fully charged. Connected to external power.
     *   <li><code>charging</code>: Battery is currently charging from external power.
     *   <li><code>discharging</code>: Battery is discharging normally.
     *   <li><code>low</code>: Battery is discharging but little power remains.
     *   <li><code>disconnected</code>: Battery has been disconnected, power being provided
     *       externally.
     *   <li><code>trouble</code>: Something is wrong with the battery or charging system.
     * </ul>
     *
     * @see #CHARGE_STATE_CHARGED
     * @see #CHARGE_STATE_CHARGING
     * @see #CHARGE_STATE_DISCHARGING
     * @see #CHARGE_STATE_LOW
     * @see #CHARGE_STATE_DISCONNECTED
     * @see #CHARGE_STATE_TROUBLE
     */
    @Property(READ_ONLY)
    public static final PropertyKey<String> STAT_CHARGE_STATE =
            new PropertyKey<>(Splot.SECTION_STATE, TRAIT_ID, "stat", java.lang.String.class);

    /**
     * Battery is fully charged; Connected to external power.
     *
     * @see #STAT_CHARGE_STATE
     */
    public static final String CHARGE_STATE_CHARGED = "charged";

    /**
     * Battery is currently charging from external power.
     *
     * @see #STAT_CHARGE_STATE
     */
    public static final String CHARGE_STATE_CHARGING = "charging";

    /**
     * Battery is discharging normally.
     *
     * @see #STAT_CHARGE_STATE
     */
    public static final String CHARGE_STATE_DISCHARGING = "discharging";

    /**
     * Battery is discharging but little power remains.
     *
     * @see #STAT_CHARGE_STATE
     */
    public static final String CHARGE_STATE_LOW = "low";

    /**
     * Battery has been disconnected, power being provided externally.
     *
     * @see #STAT_CHARGE_STATE
     */
    public static final String CHARGE_STATE_DISCONNECTED = "disconnected";

    /**
     * Something is wrong with the battery or charging system.
     *
     * @see #STAT_CHARGE_STATE
     * @see BaseTrait#STAT_TRAP
     */
    public static final String CHARGE_STATE_TROUBLE = "trouble";

    /**
     * Relative battery capacity. Only used for rechargeable batteries. This is a ratio of the
     * current maximum capacity of the battery versus the maximum capacity of the battery when it
     * was new. (Value is between 0.0 and 1.0)
     */
    @Property(READ_ONLY)
    public static final PropertyKey<Float> STAT_CAPACITY_REMAINING =
            new PropertyKey<>(Splot.SECTION_STATE, TRAIT_ID, "rcap", java.lang.Float.class);

    /** The total number of battery charge cycles. Only used for rechargeable batteries. */
    @Property(READ_ONLY)
    public static final PropertyKey<Integer> STAT_CHARGE_CYCLES =
            new PropertyKey<>(Splot.SECTION_STATE, TRAIT_ID, "cycl", java.lang.Integer.class);

    /**
     * The voltages of the individual cells in the battery. This can be used to determine the
     * general health of the battery pack.
     */
    @Property(READ_ONLY)
    public static final PropertyKey<float[]> STAT_CELL_VOLTAGE =
            new PropertyKey<>(Splot.SECTION_STATE, TRAIT_ID, "celV", float[].class);

    /**
     * The original energy capacity of the battery when new and fully charged, in milliwatt-hours.
     * The maximum energy capacity value for this interface is around 2 megawatt-hours.
     */
    @Property(READ_ONLY)
    public static final PropertyKey<Integer> META_ENERGY_CAPACITY =
            new PropertyKey<>(
                    Splot.SECTION_METADATA, TRAIT_ID, "enrg", java.lang.Integer.class);

    /** The nominal voltage of the battery, in volts */
    @Property(READ_ONLY)
    public static final PropertyKey<Float> META_NOMINAL_BATTERY_VOLTAGE =
            new PropertyKey<>(
                    Splot.SECTION_METADATA, TRAIT_ID, "volt", java.lang.Float.class);

    /** The nominal voltage of a single cell in the battery, in volts */
    @Property(READ_ONLY)
    public static final PropertyKey<Float> META_NOMINAL_CELL_VOLTAGE =
            new PropertyKey<>(
                    Splot.SECTION_METADATA, TRAIT_ID, "celV", java.lang.Float.class);

    /** The number of cells in the battery. */
    @Property(READ_ONLY)
    public static final PropertyKey<Integer> META_CELL_COUNT =
            new PropertyKey<>(
                    Splot.SECTION_METADATA, TRAIT_ID, "ccnt", java.lang.Integer.class);

    /** Indicates if the battery is rechargeable or not. */
    @Property(READ_ONLY)
    public static final PropertyKey<Boolean> META_RECHARGEABLE =
            new PropertyKey<>(
                    Splot.SECTION_METADATA, TRAIT_ID, "rech", java.lang.Boolean.class);
}
