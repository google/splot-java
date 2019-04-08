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

/** A generic class for identifying and facilitating improved type-safety for method parameters. */
public final class ParamKey<T> extends TypedKey<T> {
    private final String mName;

    /**
     * Constructs a parameter key object.
     *
     * @param name the name of the parameter
     * @param type the class for the value that will be associated with this parameter
     */
    public ParamKey(String name, Class<T> type) {
        super(type);
        Preconditions.checkNotNull(name, "name cannot be null");
        Preconditions.checkNotNull(type, "type cannot be null");

        mName = name;
    }

    /** Returns the name of this parameter. */
    public final String getName() {
        return mName;
    }
}
