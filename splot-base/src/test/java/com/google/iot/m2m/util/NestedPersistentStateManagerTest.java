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

package com.google.iot.m2m.util;

import static org.junit.jupiter.api.Assertions.*;

import com.google.iot.m2m.base.CorruptPersistentStateException;
import com.google.iot.m2m.base.ParamKey;
import com.google.iot.m2m.base.PersistentStateInterface;
import com.google.iot.m2m.base.PersistentStateListener;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.jupiter.api.Test;

@SuppressWarnings("UnusedAssignment")
class NestedPersistentStateManagerTest {

    final ParamKey<String> PARAM_STRING_KEY = new ParamKey<>("PARAM_STRING_KEY", String.class);
    final ParamKey<Integer> PARAM_INTEGER_KEY = new ParamKey<>("PARAM_INTEGER_KEY", Integer.class);
    final ParamKey<String> PARAM_WHOAMI_KEY = new ParamKey<>("PARAM_WHOAMI_KEY", String.class);

    class TestIface implements PersistentStateInterface {
        PersistentStateListener mListener = null;
        final Map<String, Object> mMap = new HashMap<>();

        @Override
        public Map<String, Object> copyPersistentState() {
            return new HashMap<>(mMap);
        }

        void trigger(boolean shouldBlock) throws IOException {
            if (mListener != null) {
                mListener.onSavePersistentState(this);
            }
        }

        void incrementNoSignal() throws Exception {
            Integer integer = PARAM_INTEGER_KEY.getFromMap(mMap);
            integer += 1;
            PARAM_INTEGER_KEY.putInMap(mMap, integer);
        }

        void increment() throws Exception {
            Integer integer = PARAM_INTEGER_KEY.getFromMap(mMap);
            integer += 1;
            PARAM_INTEGER_KEY.putInMap(mMap, integer);
            try {
                trigger(false);
            } catch (IOException e) {
                throw new AssertionError(e);
            }
        }

        void incrementBlock() throws Exception {
            Integer integer = PARAM_INTEGER_KEY.getFromMap(mMap);
            integer += 1;
            PARAM_INTEGER_KEY.putInMap(mMap, integer);
            try {
                trigger(true);
            } catch (IOException e) {
                throw new AssertionError(e);
            }
        }

        @Override
        public void initWithPersistentState(@Nullable Map<String, Object> persistentState) {
            if (persistentState == null || persistentState.isEmpty()) {
                PARAM_STRING_KEY.putInMap(mMap, "Fresh");
                PARAM_WHOAMI_KEY.putInMap(mMap, this.toString());
                PARAM_INTEGER_KEY.putInMap(mMap, 1);
                return;
            }

            mMap.putAll(persistentState);

            PARAM_STRING_KEY.putInMap(mMap, "Not fresh");
        }

        @Override
        public void setPersistentStateListener(@Nullable PersistentStateListener listener) {
            mListener = listener;
        }
    }

    @Test
    void basicBehavior() throws Exception {
        Map<String, Object> state = new HashMap<>();
        String obj1String;
        String obj2String;

        {
            TestIface obj1 = new TestIface();
            TestIface obj2 = new TestIface();

            NestedPersistentStateManager persistentStateManager =
                    new NestedPersistentStateManager();
            persistentStateManager.initWithPersistentState(state);

            persistentStateManager.startManaging("obj1", obj1);
            persistentStateManager.startManaging("obj2", obj2);

            obj1String = PARAM_WHOAMI_KEY.getFromMap(obj1.mMap);
            obj2String = PARAM_WHOAMI_KEY.getFromMap(obj2.mMap);
            assertEquals("Fresh", PARAM_STRING_KEY.getFromMap(obj1.mMap), obj1.mMap.toString());
            assertEquals("Fresh", PARAM_STRING_KEY.getFromMap(obj2.mMap), obj1.mMap.toString());

            state = persistentStateManager.copyPersistentState();
            persistentStateManager.close();
        }

        {
            TestIface obj1 = new TestIface();
            TestIface obj2 = new TestIface();

            NestedPersistentStateManager persistentStateManager =
                    new NestedPersistentStateManager();
            persistentStateManager.initWithPersistentState(state);

            persistentStateManager.startManaging("obj1", obj1);
            persistentStateManager.startManaging("obj2", obj2);

            assertEquals(obj1String, PARAM_WHOAMI_KEY.getFromMap(obj1.mMap));
            assertEquals(obj2String, PARAM_WHOAMI_KEY.getFromMap(obj2.mMap));
            assertEquals("Not fresh", PARAM_STRING_KEY.getFromMap(obj1.mMap));
            assertEquals("Not fresh", PARAM_STRING_KEY.getFromMap(obj2.mMap));

            state = persistentStateManager.copyPersistentState();
            persistentStateManager.close();
        }
    }

    @Test
    void updateBehavior() throws Exception {
        Map<String, Object> state = new HashMap<>();
        String obj1String;
        String obj2String;

        {
            TestIface obj1 = new TestIface();
            TestIface obj2 = new TestIface();

            NestedPersistentStateManager persistentStateManager =
                    new NestedPersistentStateManager();
            persistentStateManager.initWithPersistentState(state);

            persistentStateManager.startManaging("obj1", obj1);
            persistentStateManager.startManaging("obj2", obj2);

            obj1String = PARAM_WHOAMI_KEY.getFromMap(obj1.mMap);
            obj2String = PARAM_WHOAMI_KEY.getFromMap(obj2.mMap);

            assertEquals("Fresh", PARAM_STRING_KEY.getFromMap(obj1.mMap));
            assertEquals("Fresh", PARAM_STRING_KEY.getFromMap(obj2.mMap));

            state = persistentStateManager.copyPersistentState();
            persistentStateManager.close();
        }

        {
            TestIface obj1 = new TestIface();
            TestIface obj2 = new TestIface();

            NestedPersistentStateManager persistentStateManager =
                    new NestedPersistentStateManager();
            persistentStateManager.initWithPersistentState(state);

            persistentStateManager.startManaging("obj1", obj1);
            persistentStateManager.startManaging("obj2", obj2);

            assertEquals(obj1String, PARAM_WHOAMI_KEY.getFromMap(obj1.mMap));
            assertEquals(obj2String, PARAM_WHOAMI_KEY.getFromMap(obj2.mMap));

            assertEquals("Not fresh", PARAM_STRING_KEY.getFromMap(obj1.mMap));
            assertEquals("Not fresh", PARAM_STRING_KEY.getFromMap(obj2.mMap));

            obj2.increment();

            state = persistentStateManager.copyPersistentState();
            persistentStateManager.close();
        }

        {
            TestIface obj1 = new TestIface();
            TestIface obj2 = new TestIface();

            NestedPersistentStateManager persistentStateManager =
                    new NestedPersistentStateManager();
            persistentStateManager.initWithPersistentState(state);

            persistentStateManager.startManaging("obj1", obj1);
            persistentStateManager.startManaging("obj2", obj2);

            assertEquals(obj1String, PARAM_WHOAMI_KEY.getFromMap(obj1.mMap));
            assertEquals(obj2String, PARAM_WHOAMI_KEY.getFromMap(obj2.mMap));
            assertEquals("Not fresh", PARAM_STRING_KEY.getFromMap(obj1.mMap));
            assertEquals("Not fresh", PARAM_STRING_KEY.getFromMap(obj2.mMap));
            assertEquals((Object) 2, PARAM_INTEGER_KEY.getFromMap(obj2.mMap));

            state = persistentStateManager.copyPersistentState();
            persistentStateManager.close();
        }
    }

    @Test
    void stopManaging() throws Exception {
        Map<String, Object> state = new HashMap<>();
        String obj1String;
        String obj2String;

        {
            TestIface obj1 = new TestIface();
            TestIface obj2 = new TestIface();

            NestedPersistentStateManager persistentStateManager =
                    new NestedPersistentStateManager();
            persistentStateManager.initWithPersistentState(state);

            persistentStateManager.startManaging("obj1", obj1);
            persistentStateManager.startManaging("obj2", obj2);

            obj1String = PARAM_WHOAMI_KEY.getFromMap(obj1.mMap);
            obj2String = PARAM_WHOAMI_KEY.getFromMap(obj2.mMap);

            assertEquals("Fresh", PARAM_STRING_KEY.getFromMap(obj1.mMap));
            assertEquals("Fresh", PARAM_STRING_KEY.getFromMap(obj2.mMap));

            state = persistentStateManager.copyPersistentState();
            persistentStateManager.close();
        }

        {
            TestIface obj1 = new TestIface();
            TestIface obj2 = new TestIface();

            NestedPersistentStateManager persistentStateManager =
                    new NestedPersistentStateManager();
            persistentStateManager.initWithPersistentState(state);

            persistentStateManager.startManaging("obj1", obj1);
            persistentStateManager.startManaging("obj2", obj2);

            assertEquals(obj1String, PARAM_WHOAMI_KEY.getFromMap(obj1.mMap));
            assertEquals(obj2String, PARAM_WHOAMI_KEY.getFromMap(obj2.mMap));

            assertEquals("Not fresh", PARAM_STRING_KEY.getFromMap(obj1.mMap));
            assertEquals("Not fresh", PARAM_STRING_KEY.getFromMap(obj2.mMap));

            persistentStateManager.stopManaging("obj2");

            obj2.increment();

            state = persistentStateManager.copyPersistentState();
            persistentStateManager.close();
        }

        {
            TestIface obj1 = new TestIface();
            TestIface obj2 = new TestIface();

            NestedPersistentStateManager persistentStateManager =
                    new NestedPersistentStateManager();
            persistentStateManager.initWithPersistentState(state);

            persistentStateManager.startManaging("obj1", obj1);
            persistentStateManager.startManaging("obj2", obj2);

            assertEquals(obj1String, PARAM_WHOAMI_KEY.getFromMap(obj1.mMap));
            assertEquals(obj2String, PARAM_WHOAMI_KEY.getFromMap(obj2.mMap));
            assertEquals("Not fresh", PARAM_STRING_KEY.getFromMap(obj1.mMap));
            assertEquals("Not fresh", PARAM_STRING_KEY.getFromMap(obj2.mMap));
            assertEquals((Object) 1, PARAM_INTEGER_KEY.getFromMap(obj2.mMap));

            state = persistentStateManager.copyPersistentState();
            persistentStateManager.close();
        }
    }

    @Test
    void refresh() throws Exception {
        Map<String, Object> state = new HashMap<>();
        String obj1String;
        String obj2String;

        {
            TestIface obj1 = new TestIface();
            TestIface obj2 = new TestIface();

            NestedPersistentStateManager persistentStateManager =
                    new NestedPersistentStateManager();
            persistentStateManager.initWithPersistentState(state);

            persistentStateManager.startManaging("obj1", obj1);
            persistentStateManager.startManaging("obj2", obj2);

            obj1String = PARAM_WHOAMI_KEY.getFromMap(obj1.mMap);
            obj2String = PARAM_WHOAMI_KEY.getFromMap(obj2.mMap);

            assertEquals("Fresh", PARAM_STRING_KEY.getFromMap(obj1.mMap));
            assertEquals("Fresh", PARAM_STRING_KEY.getFromMap(obj2.mMap));

            state = persistentStateManager.copyPersistentState();
            persistentStateManager.close();
        }

        {
            TestIface obj1 = new TestIface();
            TestIface obj2 = new TestIface();

            NestedPersistentStateManager persistentStateManager =
                    new NestedPersistentStateManager();
            persistentStateManager.initWithPersistentState(state);

            persistentStateManager.startManaging("obj1", obj1);
            persistentStateManager.startManaging("obj2", obj2);

            assertEquals(obj1String, PARAM_WHOAMI_KEY.getFromMap(obj1.mMap));
            assertEquals(obj2String, PARAM_WHOAMI_KEY.getFromMap(obj2.mMap));

            assertEquals("Not fresh", PARAM_STRING_KEY.getFromMap(obj1.mMap));
            assertEquals("Not fresh", PARAM_STRING_KEY.getFromMap(obj2.mMap));

            obj2.incrementNoSignal();

            state = persistentStateManager.copyPersistentState();
            persistentStateManager.close();
        }

        {
            TestIface obj1 = new TestIface();
            TestIface obj2 = new TestIface();

            NestedPersistentStateManager persistentStateManager =
                    new NestedPersistentStateManager();
            persistentStateManager.initWithPersistentState(state);

            persistentStateManager.startManaging("obj1", obj1);
            persistentStateManager.startManaging("obj2", obj2);

            assertEquals(obj1String, PARAM_WHOAMI_KEY.getFromMap(obj1.mMap));
            assertEquals(obj2String, PARAM_WHOAMI_KEY.getFromMap(obj2.mMap));
            assertEquals("Not fresh", PARAM_STRING_KEY.getFromMap(obj1.mMap));
            assertEquals("Not fresh", PARAM_STRING_KEY.getFromMap(obj2.mMap));
            assertEquals((Object) 1, PARAM_INTEGER_KEY.getFromMap(obj2.mMap));

            obj2.incrementNoSignal();

            persistentStateManager.refresh("obj1");

            state = persistentStateManager.copyPersistentState();
            persistentStateManager.close();
        }

        {
            TestIface obj1 = new TestIface();
            TestIface obj2 = new TestIface();

            NestedPersistentStateManager persistentStateManager =
                    new NestedPersistentStateManager();
            persistentStateManager.initWithPersistentState(state);

            persistentStateManager.startManaging("obj1", obj1);
            persistentStateManager.startManaging("obj2", obj2);

            assertEquals(obj1String, PARAM_WHOAMI_KEY.getFromMap(obj1.mMap));
            assertEquals(obj2String, PARAM_WHOAMI_KEY.getFromMap(obj2.mMap));
            assertEquals("Not fresh", PARAM_STRING_KEY.getFromMap(obj1.mMap));
            assertEquals("Not fresh", PARAM_STRING_KEY.getFromMap(obj2.mMap));
            assertEquals((Object) 1, PARAM_INTEGER_KEY.getFromMap(obj2.mMap));

            obj2.incrementNoSignal();

            persistentStateManager.refresh("obj2");

            state = persistentStateManager.copyPersistentState();
            persistentStateManager.close();
        }

        {
            TestIface obj1 = new TestIface();
            TestIface obj2 = new TestIface();

            NestedPersistentStateManager persistentStateManager =
                    new NestedPersistentStateManager();
            persistentStateManager.initWithPersistentState(state);

            persistentStateManager.startManaging("obj1", obj1);
            persistentStateManager.startManaging("obj2", obj2);

            assertEquals(obj1String, PARAM_WHOAMI_KEY.getFromMap(obj1.mMap));
            assertEquals(obj2String, PARAM_WHOAMI_KEY.getFromMap(obj2.mMap));
            assertEquals("Not fresh", PARAM_STRING_KEY.getFromMap(obj1.mMap));
            assertEquals("Not fresh", PARAM_STRING_KEY.getFromMap(obj2.mMap));
            assertEquals((Object) 2, PARAM_INTEGER_KEY.getFromMap(obj2.mMap));

            obj1.incrementNoSignal();
            obj2.incrementNoSignal();

            persistentStateManager.refresh();

            state = persistentStateManager.copyPersistentState();
            persistentStateManager.close();
        }

        {
            TestIface obj1 = new TestIface();
            TestIface obj2 = new TestIface();

            NestedPersistentStateManager persistentStateManager =
                    new NestedPersistentStateManager();
            persistentStateManager.initWithPersistentState(state);

            persistentStateManager.startManaging("obj1", obj1);
            persistentStateManager.startManaging("obj2", obj2);

            assertEquals(obj1String, PARAM_WHOAMI_KEY.getFromMap(obj1.mMap));
            assertEquals(obj2String, PARAM_WHOAMI_KEY.getFromMap(obj2.mMap));
            assertEquals("Not fresh", PARAM_STRING_KEY.getFromMap(obj1.mMap));
            assertEquals("Not fresh", PARAM_STRING_KEY.getFromMap(obj2.mMap));
            assertEquals((Object) 2, PARAM_INTEGER_KEY.getFromMap(obj1.mMap));
            assertEquals((Object) 3, PARAM_INTEGER_KEY.getFromMap(obj2.mMap));

            obj1.incrementNoSignal();
            obj2.increment();

            state = persistentStateManager.copyPersistentState();
            persistentStateManager.close();
        }

        {
            TestIface obj1 = new TestIface();
            TestIface obj2 = new TestIface();

            NestedPersistentStateManager persistentStateManager =
                    new NestedPersistentStateManager();
            persistentStateManager.initWithPersistentState(state);

            persistentStateManager.startManaging("obj1", obj1);
            persistentStateManager.startManaging("obj2", obj2);

            assertEquals(obj1String, PARAM_WHOAMI_KEY.getFromMap(obj1.mMap));
            assertEquals(obj2String, PARAM_WHOAMI_KEY.getFromMap(obj2.mMap));
            assertEquals("Not fresh", PARAM_STRING_KEY.getFromMap(obj1.mMap));
            assertEquals("Not fresh", PARAM_STRING_KEY.getFromMap(obj2.mMap));
            assertEquals((Object) 2, PARAM_INTEGER_KEY.getFromMap(obj1.mMap));
            assertEquals((Object) 4, PARAM_INTEGER_KEY.getFromMap(obj2.mMap));

            state = persistentStateManager.copyPersistentState();
            persistentStateManager.close();
        }
    }

    @Test
    void reset() throws Exception {
        Map<String, Object> state = new HashMap<>();
        String obj1String;
        String obj2String;

        {
            TestIface obj1 = new TestIface();
            TestIface obj2 = new TestIface();

            NestedPersistentStateManager persistentStateManager =
                    new NestedPersistentStateManager();
            persistentStateManager.initWithPersistentState(state);

            persistentStateManager.startManaging("obj1", obj1);
            persistentStateManager.startManaging("obj2", obj2);

            obj1String = PARAM_WHOAMI_KEY.getFromMap(obj1.mMap);
            obj2String = PARAM_WHOAMI_KEY.getFromMap(obj2.mMap);

            assertEquals("Fresh", PARAM_STRING_KEY.getFromMap(obj1.mMap));
            assertEquals("Fresh", PARAM_STRING_KEY.getFromMap(obj2.mMap));

            state = persistentStateManager.copyPersistentState();
            persistentStateManager.close();
        }

        {
            TestIface obj1 = new TestIface();
            TestIface obj2 = new TestIface();

            NestedPersistentStateManager persistentStateManager =
                    new NestedPersistentStateManager();
            persistentStateManager.initWithPersistentState(state);

            persistentStateManager.reset("obj1");

            persistentStateManager.startManaging("obj1", obj1);
            persistentStateManager.startManaging("obj2", obj2);

            assertNotEquals(obj1String, PARAM_WHOAMI_KEY.getFromMap(obj1.mMap));
            assertEquals(obj2String, PARAM_WHOAMI_KEY.getFromMap(obj2.mMap));

            assertEquals("Fresh", PARAM_STRING_KEY.getFromMap(obj1.mMap));
            assertEquals("Not fresh", PARAM_STRING_KEY.getFromMap(obj2.mMap));

            state = persistentStateManager.copyPersistentState();
            persistentStateManager.close();
        }

        {
            TestIface obj1 = new TestIface();
            TestIface obj2 = new TestIface();

            NestedPersistentStateManager persistentStateManager =
                    new NestedPersistentStateManager();
            persistentStateManager.initWithPersistentState(state);

            persistentStateManager.reset();

            persistentStateManager.startManaging("obj1", obj1);
            persistentStateManager.startManaging("obj2", obj2);

            assertNotEquals(obj1String, PARAM_WHOAMI_KEY.getFromMap(obj1.mMap));
            assertNotEquals(obj2String, PARAM_WHOAMI_KEY.getFromMap(obj2.mMap));

            assertEquals("Fresh", PARAM_STRING_KEY.getFromMap(obj1.mMap));
            assertEquals("Fresh", PARAM_STRING_KEY.getFromMap(obj2.mMap));

            persistentStateManager.reset();

            state = persistentStateManager.copyPersistentState();
            persistentStateManager.close();
        }

        {
            TestIface obj1 = new TestIface();
            TestIface obj2 = new TestIface();

            NestedPersistentStateManager persistentStateManager =
                    new NestedPersistentStateManager();
            persistentStateManager.initWithPersistentState(state);

            persistentStateManager.startManaging("obj1", obj1);
            persistentStateManager.startManaging("obj2", obj2);

            assertNotEquals(obj1String, PARAM_WHOAMI_KEY.getFromMap(obj1.mMap));
            assertNotEquals(obj2String, PARAM_WHOAMI_KEY.getFromMap(obj2.mMap));

            assertEquals("Fresh", PARAM_STRING_KEY.getFromMap(obj1.mMap));
            assertEquals("Fresh", PARAM_STRING_KEY.getFromMap(obj2.mMap));

            state = persistentStateManager.copyPersistentState();
            persistentStateManager.close();
        }
    }

    @Test
    void close() throws IOException, CorruptPersistentStateException {
        Map<String, Object> state = new HashMap<>();

        NestedPersistentStateManager persistentStateManager = new NestedPersistentStateManager();
        persistentStateManager.initWithPersistentState(state);

        state = persistentStateManager.copyPersistentState();
        persistentStateManager.close();
    }
}
