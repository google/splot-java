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
import com.google.iot.m2m.base.TechnologyException;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Trait for describing and controlling various characteristics of a variety of illuminaries.
 *
 * <p>This trait is typically paired with {@link OnOffTrait} and {@link LevelTrait}.
 */
@Trait
public final class LightTrait {
    private static float[] whitepointD50() {
        return new float[] {0.3457f, 0.3585f};
    }

    private static float[] whitepointD65() {
        return new float[] {0.3127f, 0.3290f};
    }

    /** Abstract class for implementing trait behavior on a local thing. */
    public abstract static class AbstractLocalTrait extends LocalLightTrait {
        @Override
        public float[] onSanitizeChromaXy(
                @SuppressWarnings("NullableProblems") @Nullable float[] value) {
            // Check for input sanity.
            if (value == null
                    || value.length < 2
                    || Float.isNaN(value[0])
                    || Float.isNaN(value[1])) {

                // Input is garbage, return D50
                return whitepointD50();
            }

            // Ideally we would clamp these values to the convex hull of the
            // primaries, but for now we just clamp it to a bounding
            // box that includes the visible locus.
            float x = Math.min(0.75f, Math.max(0.001f, value[0]));
            float y = Math.min(0.85f, Math.max(0.001f, value[1]));

            return new float[] {x, y};
        }

        @Override
        public boolean onCanSaveProperty(PropertyKey<?> key) {
            boolean ret = super.onCanSaveProperty(key);

            try {
                if (ret
                        && key.isInSection(Section.STATE)
                        && !STAT_EFFECT.equals(key)
                        && !STAT_MODE.equals(key)) {
                    String mode = onGetMode();

                    if (mode != null) {
                        switch (mode) {
                            case MODE_SRGB:
                                ret = STAT_SRGB.equals(key) || STAT_WHITEPOINT.equals(key);
                                break;

                            case MODE_CIE_XY:
                                ret = STAT_CHROMA_XY.equals(key);
                                break;

                            case MODE_COLOR_TEMP:
                                ret = STAT_MIREDS.equals(key);
                                break;

                            case MODE_CIE_LCH:
                                ret =
                                        STAT_CIE_HD.equals(key)
                                                || STAT_CIE_CS.equals(key)
                                                || STAT_WHITEPOINT.equals(key);
                                break;
                        }
                    }
                }
            } catch (TechnologyException ignored) {
                // This is thrown by the call to onGetMode().
                // We don't really care about technology exceptions here.
            }
            return ret;
        }
    }

    /** The name of this trait. */
    public static final String TRAIT_NAME = "Light";

    /** The URI that identifies the specification used to implement this trait. */
    public static final String TRAIT_URI = "tag:google.com,2018:m2m:traits:light:v1:v0#r0";

    /** The Short ID of this trait. */
    public static final String TRAIT_ID = "lght";

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
     * Enumeration indicating the last color system used.
     *
     * <p>Can be one of the following values: {@link #MODE_COLOR_TEMP}, {@link #MODE_SRGB}, {@link
     * #MODE_CIE_XY}, {@link #MODE_CIE_LCH}.
     *
     * <p>This property is REQUIRED if the other implemented properties have side effects that would
     * set this property to more than one specific value.
     *
     * <p>Internally, this property is used to determine which properties need to be stored in order
     * to be able to properly recall the state later on.
     *
     * @see #MODE_COLOR_TEMP
     * @see #STAT_MIREDS
     * @see #MODE_SRGB
     * @see #STAT_SRGB
     * @see #MODE_CIE_XY
     * @see #STAT_CHROMA_XY
     * @see #MODE_CIE_LCH
     * @see #STAT_CIE_CS
     * @see #STAT_CIE_HD
     */
    @Property
    public static final PropertyKey<String> STAT_MODE =
            new PropertyKey<>(Section.STATE, TRAIT_ID, "mode", java.lang.String.class);

    /**
     * Color temperature light mode.
     *
     * @see #STAT_MODE
     * @see #STAT_MIREDS
     */
    public static final String MODE_COLOR_TEMP = "ct";

    /**
     * sRGB light mode.
     *
     * @see #STAT_MODE
     * @see #STAT_SRGB
     */
    public static final String MODE_SRGB = "sRGB";

    /**
     * CIE xy light mode.
     *
     * @see #STAT_MODE
     * @see #STAT_CHROMA_XY
     */
    public static final String MODE_CIE_XY = "xy";

    /**
     * CIE LCh light mode.
     *
     * @see #STAT_MODE
     * @see #STAT_CIE_LS
     * @see #STAT_CIE_CS
     * @see #STAT_CIE_HD
     */
    public static final String MODE_CIE_LCH = "LCh";

    /**
     * Identifies the current special effect in progress.
     *
     * <p>The interpretation of this field is vendor-specific, with the following exceptions: {@code
     * null} indicates no effect is in use, {@link #EFFECT_COLORCYCLE} indicates that the hue of the
     * light is cycling through the colors, and {@link #EFFECT_CANDLE} indicates that the light is
     * emulating the flicker of a candle.
     *
     * <p>Vendors MAY implement other effects with unique string identifiers. If this property is
     * implemented, {@link #META_EFFECTS} MUST also be implemented.
     *
     * @see #META_EFFECTS
     * @see #EFFECT_COLORCYCLE
     * @see #EFFECT_CANDLE
     */
    @Property
    public static final PropertyKey<String> STAT_EFFECT =
            new PropertyKey<>(Section.STATE, TRAIT_ID, "efct", java.lang.String.class);

    /**
     * Color-cycle special effect.
     *
     * @see #STAT_EFFECT
     */
    public static final String EFFECT_COLORCYCLE = "colorcycle";

    /**
     * Candle special effect.
     *
     * @see #STAT_EFFECT
     */
    public static final String EFFECT_CANDLE = "candle";

    /**
     * The current color temperature (absolute when written, correlated when read) in <a
     * href="https://en.wikipedia.org/wiki/Mired">Mireds</a>.
     *
     * <p>Implementing this property is REQUIRED on color-temperature lights and RECOMMENDED on
     * full-color lights.
     *
     * <p>When this property is written to, the following changes occur:
     *
     * <ul>
     *   <li>The light is configured to emit "white" light at the given color temperature.
     *   <li>{@link #STAT_MODE} (if implemented) changes to {@link #MODE_COLOR_TEMP}
     *   <li>{@link #STAT_WHITEPOINT} (if implemented) changes to this property's value
     *   <li>{@link #STAT_CIE_CS} becomes zero.
     * </ul>
     *
     * <p>If {@link #STAT_MODE} is set to {@link #MODE_COLOR_TEMP}, reading this property will yield
     * the current color temperature of the white light that is being emitted.
     *
     * <p>If {@link #STAT_MODE} is <b>NOT</b> set to {@link #MODE_COLOR_TEMP}, reading this property
     * should yield either the <i>correlated</i> color temperature of the light that is being
     * emitted or {@code null}.
     *
     * <h2>About Mireds</h2>
     *
     * <p>Mireds are a perceptually-uniform way to indicate color temperature. This is an important
     * property to ensure that transitions appear to be smooth and natural.
     *
     * <p>You can easily convert from Kelvin to Mireds using the following formula:
     *
     * <p><b><i>M</i> = 1000000 / <i>K</i></b>
     *
     * <p>Because of the reciprocal relationship between Mireds and Kelvin, the exact same formula
     * also works for converting Mireds to Kelvin.
     *
     * @see #META_MIN_MIREDS
     * @see #META_MAX_MIREDS
     * @see #META_NATIVE_MIREDS
     * @see #miredsFromKelvin(float)
     * @see #kelvinFromMireds(float)
     */
    @Property
    public static final PropertyKey<Float> STAT_MIREDS =
            new PropertyKey<>(Section.STATE, TRAIT_ID, "mire", java.lang.Float.class);

    /**
     * Convenience function for converting a color temperature in Kelvin to <a
     * href="https://en.wikipedia.org/wiki/Mired">Mireds</a>. Note that this is the exact same
     * transform as {@link #kelvinFromMireds(float)}, just named differently to make code more
     * clear.
     *
     * @param x the color temperature in Kelvin
     * @return the color temperature in Mireds
     * @see #STAT_MIREDS
     * @see #kelvinFromMireds(float)
     */
    public static float miredsFromKelvin(float x) {
        return 1000000.0f / x;
    }

    /**
     * Convenience function for converting a color temperature in <a
     * href="https://en.wikipedia.org/wiki/Mired">Mireds</a> to Kelvin. Note that this is the exact
     * same transform as {@link #miredsFromKelvin(float)}, just named differently to make code more
     * clear;
     *
     * @param x the color temperature in Mireds
     * @return the color temperature in Kelvin
     * @see #STAT_MIREDS
     * @see #miredsFromKelvin(float)
     */
    public static float kelvinFromMireds(float x) {
        return 1000000.0f / x;
    }

    /**
     * This property controls the whiteppoint used by the {@link #STAT_SRGB} and {@link
     * #STAT_CIE_HD}/{@link #STAT_CIE_CS} properties, identified by CIE xy coordinates.
     *
     * <p>Implementing this property is RECOMMENDED on full-color lights.
     */
    @Property
    public static final PropertyKey<float[]> STAT_WHITEPOINT =
            new PropertyKey<>(Section.STATE, TRAIT_ID, "whtp", float[].class);

    /**
     * The ‘x’ and ‘y’ CIE chromaticity coordinates for the current color.
     *
     * <p>This property is REQUIRED on full-color lights and OPTIONAL on color-temperature lights.
     *
     * <p>If the values written to this property are out of gamut, then the state when read will
     * reflect an in-gamut approximation.
     *
     * <p>Writing to this property changes {@link #STAT_MODE} to {@link #MODE_CIE_XY}.
     */
    @Property
    public static final PropertyKey<float[]> STAT_CHROMA_XY =
            new PropertyKey<>(Section.STATE, TRAIT_ID, "chro", float[].class);

    /**
     * The sRGB red, green, and blue components, normalized to values between 0.0 and 1.0
     * (inclusive). Uses the real composite sRGB gamma curve. Setting this value will change the
     * color of the light to match the contained values.
     *
     * <p>Implementing this property is RECOMMENDED on full-color lights.
     *
     * <p>If {@link #STAT_WHITEPOINT} is supported, use that as the reference whitepoint, otherwise
     * use D65.
     *
     * <p>The read value is NOT cropped to fit into the gamut of the device, but individual values
     * MAY be limited to the range of 0.0-1.0 or reflect the loss of precision from conversion to
     * the internal representations. If the gamut of the device is larger than the sRGB gamut, then
     * values outside of the range of 0.0-1.0 MAY be allowed.
     *
     * <p>The value read SHOULD read as {@code null} if {@link #STAT_MODE} is not {@link
     * #MODE_SRGB}, otherwise it reports the last value written.
     *
     * <p>Setting a value of all zeros MUST cause {@link OnOffTrait#STAT_VALUE} to become false.
     * Setting a value with any non-zero component MUST cause the {@link OnOffTrait#STAT_VALUE} to
     * become true.
     *
     * <p>Writing to this property changes {@link #STAT_MODE} to {@link #MODE_SRGB}.
     */
    @Property
    public static final PropertyKey<float[]> STAT_SRGB =
            new PropertyKey<>(Section.STATE, TRAIT_ID, "sRGB", float[].class);

    /**
     * A normalized, perceptually uniform value representing lightness. Defined as <em>L*</em> from
     * the CIELAB/CIELCh colorspaces, except normalized between 0 and 1. A value of 0.0 represents
     * zero light output, a value of 1.0 represents full light output, and all values in between
     * represent perceptually uniform levels of lightness. This definition necessitates that this
     * property's functionality encapsulate that of both {@link OnOffTrait#STAT_VALUE} and {@link
     * LevelTrait#STAT_VALUE}.
     *
     * <p>This property is different from that of {@link LevelTrait#STAT_VALUE} because a value of
     * 0.0 in {@link LevelTrait#STAT_VALUE} represents the lowest-possible light output, whereas a
     * value of 0.0 in this property represents zero light output. Both attempt to be somewhat
     * perceptually uniform.
     *
     * <p>There is often a significant discontinuity between the lowest possible light output level
     * and the light's "off" state. As a special case for all lightness levels below {@link
     * #META_MIN_DIM}, the transition from the lowest possible light output to “off” is calculated
     * using {@link #META_MIN_DIM} and is at the <em>perceptual</em> half-way point between no light
     * output and the value for this property associated with a {@link LevelTrait#STAT_VALUE} of
     * 0.0.
     *
     * <p>Writing to this property has no effect on {@link #STAT_MODE}.
     */
    @Property
    public static final PropertyKey<Float> STAT_CIE_LS =
            new PropertyKey<>(Section.STATE, TRAIT_ID, "CIEL", java.lang.Float.class);

    /**
     * 'C*' from CIELCh colorspace, analogous to saturation. Normalized between 0.0 and 1.0. Writing
     * to this property changes ‘mode’ to ‘LCh’. Out-of-gamut values are not reflected: When read or
     * mutated, this value is NOT adjusted to be in-gamut.
     *
     * <p>Writing to this property changes {@link #STAT_MODE} to {@link #MODE_CIE_LCH}.
     */
    @Property
    public static final PropertyKey<Float> STAT_CIE_CS =
            new PropertyKey<>(Section.STATE, TRAIT_ID, "CIEC", java.lang.Float.class);

    /**
     * 'h°' from CIELCh colorspace, analogous to hue. Normalized between 0.0 and 1.0. Writing to
     * this property changes ‘mode’ to ‘LCh’. The value is circular: Incrementing past 1.0
     * automatically loops back around to 0.0. Decrementing below 0.0 automatically loops back
     * around to 1.0. When read or mutated, this value is NOT adjusted to be in-gamut.
     *
     * <p>Writing to this property changes {@link #STAT_MODE} to {@link #MODE_CIE_LCH}.
     */
    @Property
    public static final PropertyKey<Float> STAT_CIE_HD =
            new PropertyKey<>(Section.STATE, TRAIT_ID, "CIEh", java.lang.Float.class);

    /** The maximum lumens that this FE is capable of emitting. */
    @Property(CONSTANT)
    public static final PropertyKey<Float> META_MAX_LUMENS =
            new PropertyKey<>(
                    Section.METADATA, TRAIT_ID, "mxbr", java.lang.Float.class);

    /**
     * This represents the minimum light output achievable by the light without turning completely
     * off. Physically, it is the ratio of lumens emitted at level 0.0 over the lumens emitted at
     * level 1.0. Lower is generally better.
     */
    @Property(CONSTANT)
    public static final PropertyKey<Float> META_MIN_DIM =
            new PropertyKey<>(
                    Section.METADATA, TRAIT_ID, "mdim", java.lang.Float.class);

    /**
     * The native correlated color temperature (in Mireds) of the light.
     *
     * <p>This is typically present on monochromatic lights, but may be present on full-color
     * lights. In the case of full-color lights, this property may be missing entirely, set to an
     * arbitrary value, or set to represent the specific value of {@link #STAT_MIREDS} that would
     * generate the largest possible light output.
     */
    @Property(CONSTANT)
    public static final PropertyKey<Float> META_NATIVE_MIREDS =
            new PropertyKey<>(
                    Section.METADATA, TRAIT_ID, "mire", java.lang.Float.class);

    /**
     * The <b>maximum</b> numerical value for {@link #STAT_MIREDS} on this light.
     *
     * @see #STAT_MIREDS
     */
    @Property(CONSTANT)
    public static final PropertyKey<Float> META_MAX_MIREDS =
            new PropertyKey<>(
                    Section.METADATA, TRAIT_ID, "mxct", java.lang.Float.class);

    /**
     * The <b>minimum</b> numerical value for {@link #STAT_MIREDS} on this light.
     *
     * @see #STAT_MIREDS
     */
    @Property(CONSTANT)
    public static final PropertyKey<Float> META_MIN_MIREDS =
            new PropertyKey<>(
                    Section.METADATA, TRAIT_ID, "mnct", java.lang.Float.class);

    /**
     * An array indicating the special effects that are supported by {@link #STAT_EFFECT} on this
     * light, if any.
     *
     * @see #STAT_EFFECT
     * @see #EFFECT_CANDLE
     * @see #EFFECT_COLORCYCLE
     */
    @Property(CONSTANT)
    public static final PropertyKey<String[]> META_EFFECTS =
            new PropertyKey<>(Section.METADATA, TRAIT_ID, "efct", String[].class);

    /**
     * An array describing the primaries used on the light (up to six) in the CIE xyY colorspace.
     * Each primary is described by a three-element array that contains the x, y, and Y values
     * respectively for the primary. The Y component is normalized to where the maximum brightness
     * of the light is Y=1.0.
     */
    @Property(CONSTANT)
    public static final PropertyKey<float[][]> META_PRIMARIES =
            new PropertyKey<>(Section.METADATA, TRAIT_ID, "prim", float[][].class);

    /**
     * The physical orientation of the light.
     *
     * <p>0: unspecified, 1: omnidirectional, 2: downlight, 3:uplight, 4: sidelight.
     *
     * @see #ORIENTATION_UNSPECIFIED
     * @see #ORIENTATION_OMNIDIRECTIONAL
     * @see #ORIENTATION_DOWNLIGHT
     * @see #ORIENTATION_UPLIGHT
     * @see #ORIENTATION_SIDELIGHT
     */
    @Property(ENUM)
    public static final PropertyKey<Integer> META_ORIENTATION =
            new PropertyKey<>(
                    Section.METADATA, TRAIT_ID, "ornt", java.lang.Integer.class);

    public static final int ORIENTATION_UNSPECIFIED = 0;
    public static final int ORIENTATION_OMNIDIRECTIONAL = 1;
    public static final int ORIENTATION_DOWNLIGHT = 2;
    public static final int ORIENTATION_UPLIGHT = 3;
    public static final int ORIENTATION_SIDELIGHT = 4;

    /**
     * The function of the light.
     *
     * <p>0: unspecified, 1: functional, 2: decorative, 3: informative
     *
     * @see #FUNCTION_UNSPECIFIED
     * @see #FUNCTION_FUNCTIONAL
     * @see #FUNCTION_DECORATIVE
     * @see #FUNCTION_INFORMATIVE
     */
    @Property(ENUM)
    public static final PropertyKey<Integer> META_FUNCTION =
            new PropertyKey<>(
                    Section.METADATA, TRAIT_ID, "func", java.lang.Integer.class);

    public static final int FUNCTION_UNSPECIFIED = 0;
    public static final int FUNCTION_FUNCTIONAL = 1;
    public static final int FUNCTION_DECORATIVE = 2;
    public static final int FUNCTION_INFORMATIVE = 3;

    // Prevent instantiation
    private LightTrait() {}
}
