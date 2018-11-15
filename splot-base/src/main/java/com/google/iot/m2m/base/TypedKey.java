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

import com.google.common.collect.Lists;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Shorts;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Contains the shared implementation of {@link PropertyKey}, {@link MethodKey}, and {@link
 * ParamKey}.
 *
 * @param <T> the type associated with this key
 */
public abstract class TypedKey<T> {
    // Package-private constructor to prevent external subclassing.
    TypedKey() {}

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
        if (obj == null) {
            return null;
        }

        if (!type.isInstance(obj)) {
            // Ok, the given type doesn't match the class of obj.
            // In the case where the type is a simple java array and
            // obj is a Collection, then we can coerce the object
            // to make it work. Otherwise we will need to throw an error.

            try {
                if (obj instanceof Collection<?>) {
                    if (type.isAssignableFrom(String[].class)) {
                        @SuppressWarnings(
                                "unchecked") // If wrong, we catch ClassCastException later
                        Collection<String> collection = (Collection<String>) obj;
                        obj = collection.toArray(new String[0]);

                    } else if (type.isAssignableFrom(String[].class)) {
                        @SuppressWarnings(
                                "unchecked") // If wrong, we catch ClassCastException later
                        Collection<Map> collection = (Collection<Map>) obj;
                        obj = collection.toArray(new Map[0]);

                    } else if (type.isAssignableFrom(int[].class)) {
                        @SuppressWarnings(
                                "unchecked") // If wrong, we catch ClassCastException later
                        Collection<Number> collection = (Collection<Number>) obj;
                        int[] array = new int[collection.size()];
                        Iterator<Number> iter = collection.iterator();
                        for (int i = 0; i < collection.size(); i++) {
                            array[i] = Ints.checkedCast(iter.next().longValue());
                        }
                        obj = array;

                    } else if (type.isAssignableFrom(short[].class)) {
                        @SuppressWarnings(
                                "unchecked") // If wrong, we catch ClassCastException later
                        Collection<Number> collection = (Collection<Number>) obj;
                        short[] array = new short[collection.size()];
                        Iterator<Number> iter = collection.iterator();
                        for (int i = 0; i < collection.size(); i++) {
                            array[i] = Shorts.checkedCast(iter.next().longValue());
                        }
                        obj = array;

                    } else if (type.isAssignableFrom(float[].class)) {
                        @SuppressWarnings(
                                "unchecked") // If wrong, we catch ClassCastException later
                        Collection<Number> collection = (Collection<Number>) obj;
                        float[] array = new float[collection.size()];
                        Iterator<Number> iter = collection.iterator();
                        for (int i = 0; i < collection.size(); i++) {
                            array[i] = iter.next().floatValue();
                        }
                        obj = array;

                    } else if (type.isAssignableFrom(double[].class)) {
                        @SuppressWarnings(
                                "unchecked") // If wrong, we catch ClassCastException later
                        Collection<Number> collection = (Collection<Number>) obj;
                        double[] array = new double[collection.size()];
                        Iterator<Number> iter = collection.iterator();
                        for (int i = 0; i < collection.size(); i++) {
                            array[i] = iter.next().doubleValue();
                        }
                        obj = array;
                    }

                } else if (obj instanceof Number) {
                    Number number = (Number) obj;

                    if (type.isAssignableFrom(Double.class)) {
                        obj = number.doubleValue();

                    } else if (type.isAssignableFrom(Float.class)) {
                        obj = number.floatValue();

                    } else if (type.isAssignableFrom(Integer.class)) {
                        long v = number.longValue();
                        if (v > Integer.MAX_VALUE) {
                            throw new InvalidValueException("Value too big");
                        } else if (v < Integer.MIN_VALUE) {
                            throw new InvalidValueException("Value too small");
                        }
                        obj = (int) v;

                    } else if (type.isAssignableFrom(Long.class)) {
                        obj = number.longValue();

                    } else if (type.isAssignableFrom(Short.class)) {
                        long v = number.longValue();
                        if (v > Short.MAX_VALUE) {
                            throw new InvalidValueException("Value too big");
                        } else if (v < Short.MIN_VALUE) {
                            throw new InvalidValueException("Value too small");
                        }
                        obj = (short) v;

                    } else if (type.isAssignableFrom(Byte.class)) {
                        long v = number.longValue();
                        if (v > Byte.MAX_VALUE) {
                            throw new InvalidValueException("Value too big");
                        } else if (v < Byte.MIN_VALUE) {
                            throw new InvalidValueException("Value too small");
                        }
                        obj = (byte) v;

                    } else if (type.isAssignableFrom(String.class)) {
                        obj = number.toString();

                    } else if (type.isAssignableFrom(Boolean.class)) {
                        // Note that this is a very Splot-specific transform.
                        obj = number.doubleValue() >= 0.5;

                    } else if (type.isAssignableFrom(double[].class)) {
                        obj = new double[] {number.doubleValue()};

                    } else if (type.isAssignableFrom(float[].class)) {
                        obj = new float[] {number.floatValue()};

                    } else if (type.isAssignableFrom(int[].class)) {
                        obj = new int[] {number.intValue()};
                    }

                } else if (obj instanceof Boolean) {
                    boolean boolVal = (Boolean) obj;

                    if (type.isAssignableFrom(Integer.class)) {
                        obj = boolVal ? 1 : 0;

                    } else if (type.isAssignableFrom(Long.class)) {
                        obj = boolVal ? 1L : 0L;

                    } else if (type.isAssignableFrom(Short.class)) {
                        obj = boolVal ? (short) 1 : (short) 0;

                    } else if (type.isAssignableFrom(Float.class)) {
                        obj = boolVal ? 1.0f : 0.0f;

                    } else if (type.isAssignableFrom(Double.class)) {
                        obj = boolVal ? 1.0 : 0.0;
                    }

                } else if (obj instanceof String[]) {
                    if (type.isAssignableFrom(ArrayList.class)) {
                        obj = Lists.newArrayList((String[]) obj);
                    }

                } else if (obj instanceof Object[]) {
                    if (type.isAssignableFrom(String[].class)) {
                        String[] array = new String[((Object[]) obj).length];
                        System.arraycopy((Object[]) obj, 0, array, 0, array.length);
                        obj = array;
                    }

                } else if (obj instanceof URI && type.isAssignableFrom(String.class)) {
                    obj = ((URI) obj).toASCIIString();

                } else if (obj instanceof String && type.isAssignableFrom(URI.class)) {
                    obj = new URI((String) obj);
                }

            } catch (URISyntaxException | IllegalArgumentException x) {
                throw new InvalidValueException(
                        "Value type "
                                + obj.getClass()
                                + " is coercible to "
                                + type
                                + ", but value rejected",
                        x);

            } catch (ClassCastException x) {
                throw new InvalidValueException(
                        "Fundamental value type mismatch: values of type "
                                + obj.getClass()
                                + " cannot be converted to an instance of "
                                + type,
                        x);
            }

            // Final check.
            if (!type.isInstance(obj)) {
                throw new InvalidValueException(
                        "Fundamental value type mismatch: values of type "
                                + obj.getClass()
                                + " cannot be converted to an instance of "
                                + type);
            }
        }

        return type.cast(obj);
    }

    /** Gets the name of this key. */
    public abstract String getName();

    /** Gets the class associated with this key. */
    public abstract Class<T> getType();

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
        throw new InvalidValueException();
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
        return coerce(value, getType());
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
