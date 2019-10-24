/*
 * Copyright (C) 2019 Google Inc.
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

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Convenience class representing a value tagged with a {@link TypedKey}.
 *
 * <p>This class makes it easier to pass typed parameters are arguments, specifically
 * {@link Thing#invokeMethod(MethodKey, TypedKeyValue[])}.
 *
 * @see TypedKey#with(Object)
 * @see Thing#invokeMethod(MethodKey, TypedKeyValue[])
 */
public class TypedKeyValue<T> {
    private final TypedKey<T> mKey;
    private final @Nullable T mValue;

    public static Map<String, Object> asMap(TypedKeyValue<?> ... entries) {
        Map<String, Object> ret = new HashMap<>();

        for (TypedKeyValue entry : entries) {
            ret.put(entry.getKey().getName(), entry.getValue());
        }

        return ret;
    }

    /**
     * Package-private constructor. Use {@link TypedKey#with(Object)} instead.
     */
    TypedKeyValue(TypedKey<T> key, @Nullable T value) {
        mKey = key;
        mValue = value;
    }

    public TypedKey<T> getKey() {
        return mKey;
    }

    public T getValue() {
        return mValue;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(mKey) * 1337 + Objects.hashCode(mValue);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof TypedKeyValue)) {
            return false;
        }

        TypedKeyValue rhs = (TypedKeyValue)obj;

        return Objects.equals(getKey(), rhs.getKey()) && Objects.equals(getValue(), rhs.getValue());
    }

    @Override
    public String toString() {
        if (mValue == null) {
            return getKey().toString();
        }

        return mKey + "=" + mValue;
    }
}
