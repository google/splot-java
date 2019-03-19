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
import org.checkerframework.checker.nullness.qual.Nullable;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

/**
 * Class for performing Splot's implicit value conversions.
 */
public class TypeConverter<T> {
    private final Class<T> mType;

    public static final TypeConverter<Double> DOUBLE = new TypeConverter<>(Double.class);
    public static final TypeConverter<Float> FLOAT = new TypeConverter<>(Float.class);
    public static final TypeConverter<Integer> INTEGER = new TypeConverter<>(Integer.class);
    public static final TypeConverter<Boolean> BOOLEAN = new TypeConverter<>(Boolean.class);
    public static final TypeConverter<String> STRING = new TypeConverter<>(String.class);
    public static final TypeConverter<URI> URI = new TypeConverter<>(URI.class);

    // Package-private constructor to prevent external subclassing.
    public TypeConverter(Class<T> type) {
        mType = type;
    }

    public Class<T> getType() {
        return mType;
    }

    @Nullable
    public T cast(@Nullable Object obj) {
        return getType().cast(obj);
    }

    /**
     * Casts or coerces the given object to the target type.
     *
     * @param obj The object to cast/coerce to {@code type}
     * @return Object instance of type {@code type}, or {@code null} if obj is {@code null}
     * @throws InvalidValueException if there was no way to coerce {@code obj} into an instance of
     *     {@code type}
     * @see #coerce(Object)
     */
    @Nullable
    public T coerce(@Nullable Object obj) throws InvalidValueException {
        if (obj == null) {
            return null;
        }

        if (!mType.isInstance(obj)) {
            // Ok, the given type doesn't match the class of obj.
            // In the case where the type is a simple java array and
            // obj is a Collection, then we can coerce the object
            // to make it work. Otherwise we will need to throw an error.

            try {
                if (obj instanceof Collection<?>) {
                    if (mType.isAssignableFrom(String[].class)) {
                        @SuppressWarnings(
                                "unchecked") // If wrong, we catch ClassCastException later
                                Collection<String> collection = (Collection<String>) obj;
                        obj = collection.toArray(new String[0]);

                    } else if (mType.isAssignableFrom(Map[].class)) {
                        @SuppressWarnings(
                                "unchecked") // If wrong, we catch ClassCastException later
                                Collection<Map> collection = (Collection<Map>) obj;
                        obj = collection.toArray(new Map[0]);

                    } else if (mType.isAssignableFrom(int[].class)) {
                        @SuppressWarnings(
                                "unchecked") // If wrong, we catch ClassCastException later
                                Collection<Number> collection = (Collection<Number>) obj;
                        int[] array = new int[collection.size()];
                        Iterator<Number> iter = collection.iterator();
                        for (int i = 0; i < collection.size(); i++) {
                            array[i] = Ints.checkedCast(iter.next().longValue());
                        }
                        obj = array;

                    } else if (mType.isAssignableFrom(short[].class)) {
                        @SuppressWarnings(
                                "unchecked") // If wrong, we catch ClassCastException later
                                Collection<Number> collection = (Collection<Number>) obj;
                        short[] array = new short[collection.size()];
                        Iterator<Number> iter = collection.iterator();
                        for (int i = 0; i < collection.size(); i++) {
                            array[i] = Shorts.checkedCast(iter.next().longValue());
                        }
                        obj = array;

                    } else if (mType.isAssignableFrom(float[].class)) {
                        @SuppressWarnings(
                                "unchecked") // If wrong, we catch ClassCastException later
                                Collection<Number> collection = (Collection<Number>) obj;
                        float[] array = new float[collection.size()];
                        Iterator<Number> iter = collection.iterator();
                        for (int i = 0; i < collection.size(); i++) {
                            array[i] = iter.next().floatValue();
                        }
                        obj = array;

                    } else if (mType.isAssignableFrom(double[].class)) {
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

                    if (mType.isAssignableFrom(Double.class)) {
                        obj = number.doubleValue();

                    } else if (mType.isAssignableFrom(Float.class)) {
                        obj = number.floatValue();

                    } else if (mType.isAssignableFrom(Integer.class)) {
                        long v = number.longValue();
                        if (v > Integer.MAX_VALUE) {
                            throw new InvalidValueException("Value too big");
                        } else if (v < Integer.MIN_VALUE) {
                            throw new InvalidValueException("Value too small");
                        }
                        obj = (int) v;

                    } else if (mType.isAssignableFrom(Long.class)) {
                        obj = number.longValue();

                    } else if (mType.isAssignableFrom(Short.class)) {
                        long v = number.longValue();
                        if (v > Short.MAX_VALUE) {
                            throw new InvalidValueException("Value too big");
                        } else if (v < Short.MIN_VALUE) {
                            throw new InvalidValueException("Value too small");
                        }
                        obj = (short) v;

                    } else if (mType.isAssignableFrom(Byte.class)) {
                        long v = number.longValue();
                        if (v > Byte.MAX_VALUE) {
                            throw new InvalidValueException("Value too big");
                        } else if (v < Byte.MIN_VALUE) {
                            throw new InvalidValueException("Value too small");
                        }
                        obj = (byte) v;

                    } else if (mType.isAssignableFrom(String.class)) {
                        obj = number.toString();

                    } else if (mType.isAssignableFrom(Boolean.class)) {
                        // Note that this is a very Splot-specific transform.
                        obj = number.doubleValue() >= 0.5;

                    } else if (mType.isAssignableFrom(double[].class)) {
                        obj = new double[] {number.doubleValue()};

                    } else if (mType.isAssignableFrom(float[].class)) {
                        obj = new float[] {number.floatValue()};

                    } else if (mType.isAssignableFrom(int[].class)) {
                        obj = new int[] {number.intValue()};
                    }

                } else if (obj instanceof Boolean) {
                    boolean boolVal = (Boolean) obj;

                    if (mType.isAssignableFrom(Integer.class)) {
                        obj = boolVal ? 1 : 0;

                    } else if (mType.isAssignableFrom(Long.class)) {
                        obj = boolVal ? 1L : 0L;

                    } else if (mType.isAssignableFrom(Short.class)) {
                        obj = boolVal ? (short) 1 : (short) 0;

                    } else if (mType.isAssignableFrom(Float.class)) {
                        obj = boolVal ? 1.0f : 0.0f;

                    } else if (mType.isAssignableFrom(Double.class)) {
                        obj = boolVal ? 1.0 : 0.0;
                    }

                } else if (obj instanceof String[]) {
                    if (mType.isAssignableFrom(ArrayList.class)) {
                        obj = Lists.newArrayList((String[]) obj);
                    }

                } else if (obj instanceof Object[]) {
                    if (mType.isAssignableFrom(String[].class)) {
                        String[] array = new String[((Object[]) obj).length];
                        System.arraycopy((Object[]) obj, 0, array, 0, array.length);
                        obj = array;
                    }

                } else if (obj instanceof URI && mType.isAssignableFrom(String.class)) {
                    obj = ((URI) obj).toASCIIString();

                } else if (obj instanceof String && mType.isAssignableFrom(URI.class)) {
                    obj = new URI((String) obj);
                }

            } catch (URISyntaxException | IllegalArgumentException x) {
                throw new InvalidValueException(
                        "Value type "
                                + obj.getClass()
                                + " is coercible to "
                                + mType
                                + ", but value rejected",
                        x);

            } catch (ClassCastException x) {
                throw new InvalidValueException(
                        "Fundamental value type mismatch: values of type "
                                + obj.getClass()
                                + " cannot be converted to an instance of "
                                + mType + " (1)",
                        x);
            }

            // Final check.
            if (!mType.isInstance(obj)) {
                throw new InvalidValueException(
                        "Fundamental value type mismatch: values of type "
                                + obj.getClass()
                                + " cannot be converted to an instance of "
                                + mType + " (2)");
            }
        }

        return mType.cast(obj);
    }

    public T coerceNonNull(@Nullable Object value) throws InvalidValueException {
        T ret = coerce(value);
        if (ret == null) {
            throw new InvalidValueException("null value prohibited");
        }
        return ret;
    }

    public final String toString() {
        return "TypeConverter<" + mType + ">";
    }
}
