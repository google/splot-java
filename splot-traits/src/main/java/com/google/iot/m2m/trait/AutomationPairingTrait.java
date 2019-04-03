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

import java.net.URI;

/**
 * Experimental trait representing an automation pairing. An automation pairing allows you to create
 * a relationship between two properties on two functional endpoints that are associated with the
 * same {@link com.google.iot.m2m.base.Technology} instance.
 */
@Trait
public final class AutomationPairingTrait {
    // Prevent instantiation
    private AutomationPairingTrait() {}

    /** Abstract class for implementing trait behavior on a local functional endpoint. */
    public abstract static class AbstractLocalTrait extends LocalAutomationPairingTrait {}

    /** The name of this trait */
    public static final String TRAIT_NAME = "AutomationPairing";

    /** The URI that identifies the specification used to implement this trait. */
    public static final String TRAIT_URI = "tag:google.com,2018:m2m:traits:pairing:v1:v0#r0";

    /** The Short ID of this trait (<code>"pair"</code>) */
    public static final String TRAIT_ID = "pair";

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
     * The number of times this pairing has "fired". This count may be reset by writing zero.
     * Writing any other value results in an error. The count will generally not volatile and is
     * lost after a power cycle.
     */
    @Property(READ_ONLY | RESET)
    public static final PropertyKey<Integer> STAT_COUNT =
            new PropertyKey<>(Splot.SECTION_STATE, TRAIT_ID, "c", java.lang.Integer.class);

    /**
     * The number of seconds ago that this pairing last fired. This value is not cacheable.
     * Observing it will only indicate changes to "zero".
     */
    @Property(READ_ONLY)
    public static final PropertyKey<Integer> STAT_LAST =
            new PropertyKey<>(Splot.SECTION_STATE, TRAIT_ID, "last", java.lang.Integer.class);

    /**
     * Source resource path. Local resources (absolute paths without authority) are always
     * supported, support for remote resources (absolute URIs) as the source is optional. The
     * specific requirements for absolute URIs are technology-dependent.
     */
    @Property(READ_WRITE | REQUIRED)
    public static final PropertyKey<URI> CONF_SOURCE =
            new PropertyKey<>(Splot.SECTION_CONFIG, TRAIT_ID, "src", URI.class);

    /**
     * Destination resource path. Local resources (absolute paths without authority) are always
     * supported, support for remote resources (absolute URIs) as the source is optional. The
     * specific requirements for absolute URIs are technology-dependent.
     */
    @Property(READ_WRITE | REQUIRED)
    public static final PropertyKey<URI> CONF_DESTINATION =
            new PropertyKey<>(Splot.SECTION_CONFIG, TRAIT_ID, "dst", URI.class);

    /**
     * Push flag. If true, this pairing will monitor changes to the source resource and apply them
     * to the destination resource.
     */
    @Property(READ_WRITE | GET_REQUIRED)
    public static final PropertyKey<Boolean> CONF_PUSH =
            new PropertyKey<>(
                    Splot.SECTION_CONFIG, TRAIT_ID, "push", java.lang.Boolean.class);

    /**
     * Pull flag. If true, this pairing will monitor changes to the destination resource and apply
     * them to the source resource.
     */
    @Property(READ_WRITE | GET_REQUIRED)
    public static final PropertyKey<Boolean> CONF_PULL =
            new PropertyKey<>(
                    Splot.SECTION_CONFIG, TRAIT_ID, "pull", java.lang.Boolean.class);

    /**
     * Forward value transform. This string contains a simple RPN expression for modifying the
     * numeric value of the source before applying it to the destination during "push" operations.
     * The value read from the source is the first item on the stack. The return value is the
     * top-most value on the stack after evaluation. Thus, an empty forward transform is the
     * identity function. If the stack is empty or the last pushed value is "DROP", the value
     * does not propagate --- this behavior can be used to implement a predicate.
     *
     * <p>Example: The algebraic expression <i>x' = (cos(x/2 - 0.5) + 1)/2</i> would become
     * {@code "2 / 0.5 - COS 1 + 2 /"}. Note that {@code COS}/<i>cos()</i> takes <em>turns</em>
     * instead of radians for its argument.
     *
     * <p>Example: {@code "POP 0.1858 - SWAP POP 0.3320 - SWAP DROP SWAP / -449 3525 -6823.3 5520.33 POLY3"}
     * would convert CIE xy chromaticity coordinates in an array into an approximate correlated
     * color temperature in K.</p>
     *
     * @see #CONF_REVERSE_TRANSFORM
     */
    @Property
    public static final PropertyKey<String> CONF_FORWARD_TRANSFORM =
            new PropertyKey<>(Splot.SECTION_CONFIG, TRAIT_ID, "xfwd", java.lang.String.class);

    /**
     * Reverse value transform. This string contains a simple RPN expression for modifying the
     * numeric value of the destination before applying it to the source during "pull" operations.
     * The value read from the destination is the first item on the stack. The return value is the
     * top-most value on the stack after evaluation. Thus, an empty forward transform is the
     * identity function. If the stack is empty the value does not propagate --- this behavior can
     * be used to implement a predicate.
     *
     * <p>Example: For the forward transform {@code "2 *"} (<i>x' = x * 2</i>), the correct
     * reverse transform would be {@code "2 /"} (<i>x' = x/2</i>).
     *
     * @see #CONF_FORWARD_TRANSFORM
     */
    @Property
    public static final PropertyKey<String> CONF_REVERSE_TRANSFORM =
            new PropertyKey<>(Splot.SECTION_CONFIG, TRAIT_ID, "xrev", java.lang.String.class);

    public static String TRAP_SOURCE_WRITE_FAIL = "src-write-fail";
    public static String TRAP_DESTINATION_WRITE_FAIL = "dst-write-fail";
    public static String TRAP_SOURCE_READ_FAIL = "src-read-fail";
    public static String TRAP_DESTINATION_READ_FAIL = "dst-read-fail";
}
