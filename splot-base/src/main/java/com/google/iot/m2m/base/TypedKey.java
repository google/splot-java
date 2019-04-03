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

import java.util.*;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Contains the shared implementation of {@link PropertyKey}, {@link MethodKey}, and {@link
 * ParamKey}.
 *
 * @param <T> the type associated with this key
 */
public abstract class TypedKey<T> {
    private final TypeConverter<T> mTypeConverter;

    // Package-private constructor to prevent external subclassing.
    TypedKey(Class<T> type) {
        mTypeConverter = new TypeConverter<>(type);
    }

    /**
     * Casts the given object to the type of property, coercing the value if necessary.
     *
     * @param obj The object to cast/coerce to {@code type}
     * @param type The desired class of the returned object
     * @return Object instance of type {@code type}
     * @throws InvalidValueException if there was no way to coerce {@code obj} into an instance of
     *     {@code type}
     * @see #coerce(Object)
     */
    @Nullable
    static <T> T coerce(@Nullable Object obj, Class<T> type) throws InvalidValueException {
        return new TypeConverter<>(type).coerce(obj);
    }

    /** Gets the name of this key. */
    public abstract String getName();

    /** Gets the class associated with this key. */
    public Class<T> getType() {
        return mTypeConverter.getType();
    }

    public TypedKeyValue<T> with(T value) {
        return new TypedKeyValue<>(this, value);
    }

    /** Puts a specific value into a string-keyed map in a type-safe way. */
    public void putInMap(Map<String, Object> map, @Nullable T value) {
        map.put(getName(), value);
    }

    /**
     * Retrieves a the value associated with this key from a string-keyed map in a strongly-typed,
     * type-safe way. Does not attempt to coerce the retrieved value to be of type {@code T}: will
     * throw an exception if the retrieved object cannot cleanly cast to {@code T}.
     *
     * @throws InvalidValueException if the value could not be cast to type {@code T}.
     * @see #getFromMapNoThrow(Map)
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public T getFromMap(Map<String, ?> map) throws InvalidValueException {
        Object ret = map.get(getName());
        if (ret == null || getType().isInstance(ret)) {
            return (T) ret;
        }
        throw new InvalidValueException("Expected " + getType() + ", got " + ret.getClass());
    }

    /**
     * Same as {@link #getFromMap(Map)}, except that it will return null upon failure instead of
     * throwing {@link InvalidValueException}.
     *
     * @see #getFromMap(Map)
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public T getFromMapNoThrow(Map<String, ?> map) {
        Object ret = map.get(getName());
        if (getType().isInstance(ret)) {
            return (T) ret;
        }
        return null;
    }

    /**
     * A weak-typed variant of {@link #getFromMap} that will attempt to coerce the retrieved value
     * to the correct type.
     *
     * @throws InvalidValueException if the value could not be coerced to type {@code T}.
     * @see #coerce(Object)
     */
    @Nullable
    public T coerceFromMap(Map<String, ?> map) throws InvalidValueException {
        return coerce(map.get(getName()));
    }

    /**
     * Same as {@link #coerceFromMap(Map)}, except that it will return null upon failure instead of
     * throwing {@link InvalidValueException}.
     */
    @Nullable
    public T coerceFromMapNoThrow(Map<String, ?> map) {
        try {
            return coerceFromMap(map);
        } catch (InvalidValueException ignore) {
            return null;
        }
    }

    /** Removes any value that might be associated with this property key from the given map. */
    public void removeFromMap(Map<String, ?> map) {
        map.remove(getName());
    }

    /** Determines if there is any value associated with this property key in the given map. */
    public boolean isInMap(Map<String, ?> map) {
        return map.containsKey(getName());
    }

    /**
     * Casts the given object to the type associated with this key. No type massaging is performed.
     *
     * @param obj The object to cast to type {@code T}
     * @return Object instance of type {@code T}
     * @throws ClassCastException if the cast was invalid
     * @see #coerce
     */
    @Nullable
    public T cast(@Nullable Object obj) {
        return getType().cast(obj);
    }

    /**
     * Casts the given object to the type of property, massaging the type if necessary.
     *
     * @param value The object to cast/coerce to type {@code T}
     * @return Object instance of type {@code T}
     * @throws InvalidValueException if there was no way to coerce {@code value} into an instance of
     *     {@code T}
     * @see #cast
     * @see #coerce(Object, Class)
     */
    @Nullable
    public T coerce(@Nullable Object value) throws InvalidValueException {
        return mTypeConverter.coerce(value);
    }

    @Override
    public int hashCode() {
        return getName().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (!(obj instanceof TypedKey)) {
            return false;
        }

        final TypedKey<?> rhs = (TypedKey) obj;
        final Class<T> lhsType = getType();
        final Class<?> rhsType = rhs.getType();

        return getName().equals(rhs.getName())
                && (lhsType.isAssignableFrom(rhsType) || rhsType.isAssignableFrom(lhsType));
    }

    public final String toString() {
        return getName();
    }
}
