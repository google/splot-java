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
package com.google.iot.m2m.base;

import com.google.common.base.Preconditions;

/**
 * A generic class for facilitating type-safety on individual properties while remaining convenient
 * to use.
 *
 * @see com.google.iot.m2m.annotation.Method
 */
public final class PropertyKey<T> extends TypedKey<T> {
    private final String mName;

    /**
     * Constructs a property key object. Note that {@link #PropertyKey(Section, String,
     * String, Class)} is the preferred constructor to use.
     *
     * @param fullName the full name of the property, in the form <code>
     *     &lt;SECTION&gt;/&lt;TRAIT-SHORT-ID&gt;/&lt;PROP-SHORT-ID&gt;</code>.
     * @param type the class for the value that will be associated with this property.
     */
    public PropertyKey(String fullName, Class<T> type) {
        super(type);
        Preconditions.checkNotNull(fullName, "fullName cannot be null");

        // Verify that the section is legal.
        try {
            @SuppressWarnings("unused")
            Section ignore = Section.fromId(fullName.substring(0, fullName.indexOf('/')));
        } catch (InvalidSectionException|StringIndexOutOfBoundsException e) {
            throw new IllegalArgumentException("Illegal property name: " + fullName, e);
        }

        mName = fullName;
    }

    /**
     * Preferred constructor for PropertyKey objects.
     *
     * @param section the section this property is in. Can be one of {@link Section#STATE},
     *                {@link Section#CONFIG}, or {@link Section#METADATA}.
     * @param trait the short name of the trait that owns this property
     * @param shortName the short name of the property
     * @param type the class for the value that will be associated with this property.
     */
    public PropertyKey(Section section, String trait, String shortName, Class<T> type) {
        this(section.id + "/" + trait + "/" + shortName, type);
    }

    /**
     * Returns the name of this property, in the form <code>
     * &lt;SECTION&gt;/&lt;TRAIT-ID&gt;/&lt;PROP-ID&gt;</code>.
     *
     * <ul>
     *   <li>{@code SECTION}: either {@link Splot#SECTION_STATE}, {@link Splot#SECTION_CONFIG}, or {@link
     *       Splot#SECTION_METADATA}
     *   <li>{@code TRAIT-ID}: the short identifier of the trait that this property belongs to
     *   <li>{@code PROP-ID}: the short identifier of the property
     * </ul>
     */
    @Override
    public final String getName() {
        return mName;
    }

    /**
     * Method for determining if this property is in a given section.
     *
     * @return true if the property is in the state section, false otherwise.
     * @see Section#containsPath(String)
     */
    public boolean isInSection(Section section) {
        return section.containsPath(mName);
    }

    /**
     * Returns the {@link Section} enumeration that this property key falls under.
     * @return a {@link Section} enum value
     */
    public Section getSection() {
        try {
            return Section.fromId(mName.substring(0, mName.indexOf('/')));
        } catch (InvalidSectionException e) {
            // This shouldn't happen, we should catch these cases in the constructor.
            throw new IllegalStateException(e);
        }
    }
}
