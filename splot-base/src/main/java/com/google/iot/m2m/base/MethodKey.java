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
 * A generic class for identifying and facilitating return-type-safety for methods.
 *
 * @see com.google.iot.m2m.annotation.Method
 */
public final class MethodKey<T> extends TypedKey<T> {

    private final String mName;

    /**
     * Constructs a method key object.
     *
     * @param fullName the name of the property, in the form {@code
     *     f/&lt;TRAIT-SHORT-ID&gt;?&lt;METHOD-NAME&gt;}.
     * @param type the class for the value that will be associated with this property.
     */
    private MethodKey(String fullName, Class<T> type) {
        super(type);
        Preconditions.checkNotNull(fullName, "fullName cannot be null");
        Preconditions.checkNotNull(type, "type cannot be null");

        mName = fullName;
    }

    /**
     * Preferred constructor for MethodKey objects.
     *
     * @param trait the short name of the trait that owns this method
     * @param shortName the short name of the method
     * @param type the class for the value that will be associated with this property.
     */
    public MethodKey(String trait, String shortName, Class<T> type) {
        this(Splot.SECTION_FUNC + "/" + trait + "?" + shortName, type);
    }

    /**
     * Returns the full name of this property, in the form {@code
     * f/&lt;TRAIT-SHORT-ID&gt;?&lt;METHOD-NAME&gt;}.
     */
    public final String getName() {
        return mName;
    }
}
