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

import static org.junit.jupiter.api.Assertions.*;

import com.google.common.collect.Lists;
import java.util.List;
import org.junit.jupiter.api.Test;

class TypedKeyTest {

    @Test
    void coerceFloatInteger() throws Exception {
        assertEquals((Integer) 0, TypedKey.coerce(0.1f, Integer.class));
        assertEquals((Integer) 0, TypedKey.coerce(0.4f, Integer.class));
        assertEquals((Integer) 0, TypedKey.coerce(0.8f, Integer.class));
        assertEquals((Integer) 1, TypedKey.coerce(1.0f, Integer.class));
        assertEquals((Float) 1.0f, TypedKey.coerce(1, Float.class));
        assertEquals((Float) 0.0f, TypedKey.coerce(0, Float.class));
        assertEquals((Float) (-1.0f), TypedKey.coerce(-1, Float.class));
    }

    @Test
    void coerceDoubleInteger() throws Exception {
        assertEquals((Double) 1.0, TypedKey.coerce(1, Double.class));
    }

    @Test
    void coerceFloatBoolean() throws Exception {
        assertEquals(Boolean.FALSE, TypedKey.coerce(0.1f, Boolean.class));
        assertEquals(Boolean.TRUE, TypedKey.coerce(0.5f, Boolean.class));
    }

    @Test
    void coerceStringInteger() throws Exception {
        assertEquals("1", TypedKey.coerce(1, String.class));
        assertThrows(InvalidValueException.class, () -> TypedKey.coerce("1", Integer.class));
    }

    @Test
    void coerceLongInteger() throws Exception {
        assertThrows(
                InvalidValueException.class, () -> TypedKey.coerce(Long.MAX_VALUE, Integer.class));
        assertEquals((Integer) 1, TypedKey.coerce(1L, Integer.class));
    }

    @Test
    void coerceStringArray() throws Exception {
        String[] array = new String[] {"one", "two", "three"};
        List<String> list = Lists.newArrayList(array);

        assertArrayEquals(array, TypedKey.coerce(list, array.getClass()));
        assertEquals(list, TypedKey.coerce(array, list.getClass()));
    }

    @Test
    void coerceIntegerArray() throws Exception {
        int[] array = new int[] {1, 2, 3};
        List<Integer> list = Lists.newArrayList(1, 2, 3);

        assertArrayEquals(array, TypedKey.coerce(list, array.getClass()));
    }

    @Test
    void coerceFloatArray() throws Exception {
        float[] array = new float[] {1.0f, 2.0f, 3.0f};
        List<Float> list = Lists.newArrayList(1.0f, 2.0f, 3.0f);

        assertArrayEquals(array, TypedKey.coerce(list, array.getClass()));
        // assertEquals(list, TypedKey.coerce(array, list.getClass()));
    }

    @Test
    void coerceDoubleArray() throws Exception {
        double[] array = new double[] {1.0, 2.0, 3.0};
        List<Double> list = Lists.newArrayList(1.0, 2.0, 3.0);

        assertArrayEquals(array, TypedKey.coerce(list, array.getClass()));
        // assertEquals(list, TypedKey.coerce(array, list.getClass()));
    }
}
