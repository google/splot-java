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
package com.google.iot.m2m.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation describing the characteristics of a <i>property</i> in a trait. This annotation is
 * applied to all {@link com.google.iot.m2m.base.PropertyKey} fields designated as {@code public
 * static final} in a trait class. The value of this annotation is a bit-mask describing the
 * characteristics of the property.
 *
 * <h2>Example</h2>
 *
 * <pre><code>
 *     &#64;Property(READ_WRITE | GET_REQUIRED)
 *     public static final PropertyKey<Float> STAT_VALUE =
 *             new PropertyKey<>(
 *                     Section.STATE,
 *                     TRAIT_ID,
 *                     "v",
 *                     Float.class);
 *
 *     &#64;Property(CONSTANT)
 *     public static final PropertyKey<Float> META_NATIVE_MIREDS =
 *             new PropertyKey<>(
 *                     Section.METADATA,
 *                     TRAIT_ID,
 *                     "mire",
 *                     Float.class);
 * </code></pre>
 *
 * @see Trait
 * @see Method
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.CLASS)
public @interface Property {

    /**
     * Indicates that the property may have a getter.
     *
     * @see #GET_REQUIRED
     * @see #REQUIRED
     */
    int GET = (1 << 0);

    /**
     * Indicates that the property may have a setter.
     *
     * @see #SET_REQUIRED
     * @see #REQUIRED
     */
    int SET = (1 << 1);

    /**
     * Set if the property has the ability to send notifications about changes to its value.
     *
     * @see #READ_WRITE
     */
    int CHANGE = (1 << 2);

    /**
     * Indicates that a getter is REQUIRED, but only if {@link #GET} is also present.
     *
     * @see #GET
     * @see #REQUIRED
     */
    int GET_REQUIRED = (1 << 3);

    /**
     * Indicates that a setter is REQUIRED, but only if {@link #SET} is also present.
     *
     * @see #SET
     * @see #REQUIRED
     */
    int SET_REQUIRED = (1 << 4);

    /**
     * Indicates that this property can be "reset" by passing 'null' to the setter. This implies
     * that the property has a setter, but unless {@link #SET} is also present, writing other values
     * does nothing.
     */
    int RESET = (1 << 5);

    /**
     * Indicates that the value of this property should not be included when saving state. This only
     * has an effect when {@link #SET} is also present.
     */
    int NO_SAVE = (1 << 6);

    /**
     * Indicates that the value of this state property should not be smoothly transitioned. This
     * only has an effect when both {@link #GET} and {@link #SET} are also present.
     */
    int NO_TRANSITION = (1 << 7);

    /**
     * Indicates that this property should explicitly not allow the increment action to be performed
     * on it.
     */
    int NO_INCREMENT = (1 << 8);

    /**
     * Indicates that the full implementation of this property is required. This is a more general
     * version of {@link #GET_REQUIRED}/{@link #SET_REQUIRED}.
     *
     * @see #GET_REQUIRED
     * @see #SET_REQUIRED
     */
    int REQUIRED = GET_REQUIRED | SET_REQUIRED;

    /** A combination of {@link #GET}, {@link #SET}, and {@link #CHANGE}. This is the default. */
    int READ_WRITE = GET | SET | CHANGE;

    /** A combination of {@link #GET}, {@link #CHANGE}, and {@link #NO_SAVE}. */
    int READ_ONLY = GET | CHANGE | NO_SAVE;

    /** A combination of {@link #SET} and {@link #NO_SAVE}. */
    int WRITE_ONLY = SET | NO_SAVE;

    /** A combination of {@link #GET}, {@link #NO_SAVE}, and {@link #NO_TRANSITION}. */
    int CONSTANT = GET | NO_SAVE | NO_TRANSITION;

    /** A combination of {@link #NO_INCREMENT} and {@link #NO_TRANSITION}. */
    int ENUM = NO_INCREMENT | NO_TRANSITION;

    int value() default READ_WRITE;
}
