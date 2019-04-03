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
package com.google.iot.m2m.local;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.google.iot.m2m.base.ChildListener;
import com.google.iot.m2m.base.FunctionalEndpoint;
import com.google.iot.m2m.base.PropertyKey;
import com.google.iot.m2m.base.PropertyListener;
import com.google.iot.m2m.trait.*;
import com.google.iot.m2m.util.NestedPersistentStateManager;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import org.junit.jupiter.api.*;
import org.mockito.Mock;

public class LocalFunctionalEndpointTest extends TestBase {
    private static final boolean DEBUG = false;
    private static final Logger LOGGER =
            Logger.getLogger(LocalFunctionalEndpointTest.class.getCanonicalName());

    @Mock ChildListener mChildListenerMock;

    @Test
    public void functionalEndpointGetTest() throws Exception {
        FunctionalEndpoint fe = new MyLightBulb();

        Set<PropertyKey<?>> propertyKeys = fe.fetchSupportedPropertyKeys().get();

        if (DEBUG) LOGGER.info("propertyKeys = " + propertyKeys);

        for (PropertyKey<?> key : propertyKeys) {
            Object value = fe.fetchProperty(key).get();

            if (DEBUG) LOGGER.info("Key: " + key + " Value: " + value);
        }

        Map<String, Object> map;

        map = fe.fetchState().get();
        assertNotNull(map);
        if (DEBUG) LOGGER.info("State: " + map);

        map = fe.fetchConfig().get();
        assertNotNull(map);
        if (DEBUG) LOGGER.info("Config: " + map);

        map = fe.fetchMetadata().get();
        assertNotNull(map);
        if (DEBUG) LOGGER.info("Metadata: " + map);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void functionalEndpointSetTest() throws Exception {
        PropertyListener listener =
                (fe, property, value) -> {
                    if (DEBUG)
                        LOGGER.info("Property changed! Key: " + property + " Value: " + value);
                };

        FunctionalEndpoint fe = new MyLightBulb();

        fe.registerPropertyListener(mExecutor, OnOffTrait.STAT_VALUE, listener);

        boolean prevValue = fe.fetchProperty(OnOffTrait.STAT_VALUE).get();

        fe.toggleProperty(OnOffTrait.STAT_VALUE).get();

        assertEquals(!prevValue, fe.fetchProperty(OnOffTrait.STAT_VALUE).get());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void functionalEndpointSetNoTransTest() throws Exception {
        PropertyListener listener =
                (fe, property, value) -> {
                    if (DEBUG)
                        LOGGER.info("Property changed! Key: " + property + " Value: " + value);
                };

        FunctionalEndpoint fe = new MyLightBulbNoTrans();

        fe.registerPropertyListener(mExecutor, OnOffTrait.STAT_VALUE, listener);

        boolean prevValue = fe.fetchProperty(OnOffTrait.STAT_VALUE).get();

        fe.toggleProperty(OnOffTrait.STAT_VALUE).get();

        assertEquals(!prevValue, fe.fetchProperty(OnOffTrait.STAT_VALUE).get());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void functionalEndpointDefaultTransitionTest() throws Exception {
        PropertyListener listener =
                (fe, property, value) -> {
                    if (DEBUG)
                        LOGGER.info("Property changed! Key: " + property + " Value: " + value);
                };

        FunctionalEndpoint fe = new MyLightBulb();

        fe.registerPropertyListener(mExecutor, OnOffTrait.STAT_VALUE, listener);
        fe.registerPropertyListener(mExecutor, LevelTrait.STAT_VALUE, listener);

        fe.setProperty(OnOffTrait.STAT_VALUE, true);
        fe.setProperty(LevelTrait.STAT_VALUE, 1.0f);

        Thread.sleep(200);

        assertNotEquals(1.0f, fe.fetchProperty(LevelTrait.STAT_VALUE).get());
        assertNotEquals(0.0f, fe.fetchProperty(TransitionTrait.STAT_DURATION).get());

        Thread.sleep(500);

        assertEquals(1.0f, (float) fe.fetchProperty(LevelTrait.STAT_VALUE).get());
        assertEquals(0.0f, (float) fe.fetchProperty(TransitionTrait.STAT_DURATION).get());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void functionalEndpointTransitionTest() throws Exception {
        PropertyListener listener =
                (fe, property, value) -> {
                    if (DEBUG)
                        LOGGER.info("Property changed! Key: " + property + " Value: " + value);
                };

        FunctionalEndpoint fe = new MyLightBulb();

        fe.registerPropertyListener(mExecutor, OnOffTrait.STAT_VALUE, listener);
        fe.registerPropertyListener(mExecutor, LevelTrait.STAT_VALUE, listener);
        fe.registerPropertyListener(mExecutor, TransitionTrait.STAT_DURATION, listener);

        Map<String, Object> newState = new HashMap<>();

        OnOffTrait.STAT_VALUE.putInMap(newState, true);
        LevelTrait.STAT_VALUE.putInMap(newState, 1.0f);
        TransitionTrait.STAT_DURATION.putInMap(newState, 1.0f);

        fe.applyProperties(newState);

        Thread.sleep(500);

        assertEquals(true, fe.fetchProperty(OnOffTrait.STAT_VALUE).get());
        assertNotEquals(1.0f, fe.fetchProperty(LevelTrait.STAT_VALUE).get());
        assertNotEquals(0.0f, fe.fetchProperty(TransitionTrait.STAT_DURATION).get());

        Thread.sleep(800);

        assertEquals(true, fe.fetchProperty(OnOffTrait.STAT_VALUE).get());
        assertEquals(1.0f, (float) fe.fetchProperty(LevelTrait.STAT_VALUE).get());
        assertEquals(0.0f, (float) fe.fetchProperty(TransitionTrait.STAT_DURATION).get());

        fe.setProperty(LevelTrait.STAT_VALUE, 0.0f).get();
        fe.setProperty(OnOffTrait.STAT_VALUE, false).get();
        Thread.sleep(500);
        assertEquals(
                false, fe.fetchProperty(OnOffTrait.STAT_VALUE).get(500, TimeUnit.MILLISECONDS));
        assertEquals(
                (Float) 0.0f,
                fe.fetchProperty(LevelTrait.STAT_VALUE).get(500, TimeUnit.MILLISECONDS));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void functionalEndpointTransitionTest2() throws Exception {
        PropertyListener listener =
                (fe, property, value) -> {
                    if (DEBUG)
                        LOGGER.info("Property changed! Key: " + property + " Value: " + value);
                };

        FunctionalEndpoint fe = new MyLightBulb();

        fe.registerPropertyListener(mExecutor, OnOffTrait.STAT_VALUE, listener);
        fe.registerPropertyListener(mExecutor, LevelTrait.STAT_VALUE, listener);
        fe.registerPropertyListener(mExecutor, TransitionTrait.STAT_DURATION, listener);

        assertEquals(
                false, fe.fetchProperty(OnOffTrait.STAT_VALUE).get(500, TimeUnit.MILLISECONDS));
        assertEquals(
                (Float) 0.0f,
                fe.fetchProperty(LevelTrait.STAT_VALUE).get(500, TimeUnit.MILLISECONDS));
        fe.setProperty(OnOffTrait.STAT_VALUE, true).get();
        fe.setProperty(LevelTrait.STAT_VALUE, 100000.0f).get();
        Thread.sleep(500);
        assertEquals(true, fe.fetchProperty(OnOffTrait.STAT_VALUE).get(500, TimeUnit.MILLISECONDS));
        assertEquals(
                (Float) 1.0f,
                fe.fetchProperty(LevelTrait.STAT_VALUE).get(500, TimeUnit.MILLISECONDS));
        fe.setProperty(LevelTrait.STAT_VALUE, 0.0f).get();
        fe.setProperty(OnOffTrait.STAT_VALUE, false).get();
        Thread.sleep(500);
        assertEquals(
                false, fe.fetchProperty(OnOffTrait.STAT_VALUE).get(500, TimeUnit.MILLISECONDS));
        assertEquals(
                (Float) 0.0f,
                fe.fetchProperty(LevelTrait.STAT_VALUE).get(500, TimeUnit.MILLISECONDS));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void functionalEndpointTransitionOnOffLevel() throws Exception {
        PropertyListener listener =
                (fe, property, value) -> {
                    if (DEBUG)
                        LOGGER.info("Property changed! Key: " + property + " Value: " + value);
                };

        FunctionalEndpoint fe = new MyLightBulb();

        fe.registerPropertyListener(mExecutor, OnOffTrait.STAT_VALUE, listener);
        fe.registerPropertyListener(mExecutor, LevelTrait.STAT_VALUE, listener);
        fe.registerPropertyListener(mExecutor, TransitionTrait.STAT_DURATION, listener);

        assertEquals(
                false, fe.fetchProperty(OnOffTrait.STAT_VALUE).get(500, TimeUnit.MILLISECONDS));
        assertEquals(
                (Float) 0.0f,
                fe.fetchProperty(LevelTrait.STAT_VALUE).get(500, TimeUnit.MILLISECONDS));

        fe.setProperty(LevelTrait.STAT_VALUE, 1.0f).get();
        Thread.sleep(500);
        assertEquals(
                (Float) 1.0f,
                fe.fetchProperty(LevelTrait.STAT_VALUE).get(500, TimeUnit.MILLISECONDS));

        fe.setProperty(OnOffTrait.STAT_VALUE, true).get();
        Thread.sleep(100);

        assertNotEquals(
                1.0f, fe.fetchProperty(LevelTrait.STAT_VALUE).get(50, TimeUnit.MILLISECONDS));
        assertNotEquals(
                0.0f, fe.fetchProperty(LevelTrait.STAT_VALUE).get(50, TimeUnit.MILLISECONDS));

        Thread.sleep(500);
        assertEquals(
                (Float) 1.0f,
                fe.fetchProperty(LevelTrait.STAT_VALUE).get(500, TimeUnit.MILLISECONDS));
        assertEquals(true, fe.fetchProperty(OnOffTrait.STAT_VALUE).get(500, TimeUnit.MILLISECONDS));

        fe.setProperty(OnOffTrait.STAT_VALUE, false).get();
        Thread.sleep(100);

        assertNotEquals(
                1.0f, fe.fetchProperty(LevelTrait.STAT_VALUE).get(500, TimeUnit.MILLISECONDS));
        assertNotEquals(
                0.0f, fe.fetchProperty(LevelTrait.STAT_VALUE).get(500, TimeUnit.MILLISECONDS));

        Thread.sleep(500);

        assertEquals(
                (Float) 1.0f,
                fe.fetchProperty(LevelTrait.STAT_VALUE).get(500, TimeUnit.MILLISECONDS));
        assertEquals(
                false, fe.fetchProperty(OnOffTrait.STAT_VALUE).get(500, TimeUnit.MILLISECONDS));

        fe.setProperty(LevelTrait.STAT_VALUE, 0.0f).get();
        Thread.sleep(100);
        assertEquals(
                (Float) 0.0f,
                fe.fetchProperty(LevelTrait.STAT_VALUE).get(500, TimeUnit.MILLISECONDS));
        assertEquals(
                false, fe.fetchProperty(OnOffTrait.STAT_VALUE).get(500, TimeUnit.MILLISECONDS));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void functionalEndpointTransitionIncrementTest() throws Exception {
        PropertyListener listener =
                (fe, property, value) -> {
                    if (DEBUG)
                        LOGGER.info("Property changed! Key: " + property + " Value: " + value);
                };

        FunctionalEndpoint fe = new MyLightBulb();

        fe.registerPropertyListener(mExecutor, OnOffTrait.STAT_VALUE, listener);
        fe.registerPropertyListener(mExecutor, LevelTrait.STAT_VALUE, listener);
        fe.registerPropertyListener(mExecutor, TransitionTrait.STAT_DURATION, listener);

        assertEquals(
                false, fe.fetchProperty(OnOffTrait.STAT_VALUE).get(500, TimeUnit.MILLISECONDS));
        assertEquals(
                (Float) 0.0f,
                fe.fetchProperty(LevelTrait.STAT_VALUE).get(500, TimeUnit.MILLISECONDS));

        fe.setProperty(OnOffTrait.STAT_VALUE, true).get();

        fe.incrementProperty(LevelTrait.STAT_VALUE, 0.1f).get();
        fe.incrementProperty(LevelTrait.STAT_VALUE, 0.1f).get();
        fe.incrementProperty(LevelTrait.STAT_VALUE, 0.1f).get();

        assertNotEquals(
                0.3f, fe.fetchProperty(LevelTrait.STAT_VALUE).get(500, TimeUnit.MILLISECONDS));
        Thread.sleep(500);
        assertEquals(
                (Float) 0.3f,
                fe.fetchProperty(LevelTrait.STAT_VALUE).get(500, TimeUnit.MILLISECONDS));

        fe.incrementProperty(LevelTrait.STAT_VALUE, -0.1f).get();
        fe.incrementProperty(LevelTrait.STAT_VALUE, -0.1f).get();
        fe.incrementProperty(LevelTrait.STAT_VALUE, -0.1f).get();
        fe.incrementProperty(LevelTrait.STAT_VALUE, -0.1f).get();

        assertNotEquals(
                0.0f, fe.fetchProperty(LevelTrait.STAT_VALUE).get(500, TimeUnit.MILLISECONDS));
        Thread.sleep(500);
        assertEquals(
                (Float) 0.0f,
                fe.fetchProperty(LevelTrait.STAT_VALUE).get(500, TimeUnit.MILLISECONDS));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void localSceneTest() throws Exception {
        FunctionalEndpoint fe = new MyLightBulbNoTrans();

        if (DEBUG) {
            PropertyListener listener =
                    (ignored, property, value) -> {
                        LOGGER.info("Property changed! Key: " + property + " Value: " + value);
                    };

            fe.registerPropertyListener(mExecutor, OnOffTrait.STAT_VALUE, listener);
            fe.registerPropertyListener(mExecutor, LevelTrait.STAT_VALUE, listener);
            fe.registerPropertyListener(mExecutor, TransitionTrait.STAT_DURATION, listener);
        }

        fe.registerChildListener(mExecutor, SceneTrait.TRAIT_ID, mChildListenerMock);

        verify(mChildListenerMock, never()).onChildAdded(any(), any(), any());
        verify(mChildListenerMock, never()).onChildRemoved(any(), any(), any());

        fe.setProperty(LevelTrait.STAT_VALUE, 0.0f).get();
        fe.setProperty(OnOffTrait.STAT_VALUE, false).get();
        FunctionalEndpoint offScene =
                fe.invokeMethod(SceneTrait.METHOD_SAVE, SceneTrait.PARAM_SCENE_ID.with("off")).get();

        verify(mChildListenerMock, timeout(100).only())
                .onChildAdded(eq(fe), eq(SceneTrait.TRAIT_ID), eq(offScene));

        clearInvocations(mChildListenerMock);

        fe.setProperty(LevelTrait.STAT_VALUE, 1.0f).get();
        fe.setProperty(OnOffTrait.STAT_VALUE, true).get();

        verify(mChildListenerMock, never()).onChildAdded(any(), any(), any());
        verify(mChildListenerMock, never()).onChildRemoved(any(), any(), any());

        FunctionalEndpoint onScene =
                fe.invokeMethod(SceneTrait.METHOD_SAVE, SceneTrait.PARAM_SCENE_ID.with("on")).get();

        verify(mChildListenerMock, timeout(100).only())
                .onChildAdded(eq(fe), eq(SceneTrait.TRAIT_ID), eq(onScene));

        clearInvocations(mChildListenerMock);

        fe.setProperty(LevelTrait.STAT_VALUE, 0.25f).get();
        FunctionalEndpoint dimScene =
                fe.invokeMethod(SceneTrait.METHOD_SAVE, SceneTrait.PARAM_SCENE_ID.with("dim")).get();

        verify(mChildListenerMock, timeout(100).only())
                .onChildAdded(eq(fe), eq(SceneTrait.TRAIT_ID), eq(dimScene));

        clearInvocations(mChildListenerMock);

        assertEquals(true, fe.fetchProperty(OnOffTrait.STAT_VALUE).get());
        assertEquals((Float) 0.25f, fe.fetchProperty(LevelTrait.STAT_VALUE).get());

        fe.setProperty(SceneTrait.STAT_SCENE_ID, "on").get();
        assertEquals(true, fe.fetchProperty(OnOffTrait.STAT_VALUE).get());
        assertEquals((Float) 1.0f, fe.fetchProperty(LevelTrait.STAT_VALUE).get());
        assertEquals("on", fe.fetchProperty(SceneTrait.STAT_SCENE_ID).get());

        fe.setProperty(SceneTrait.STAT_SCENE_ID, "off").get();
        assertEquals(false, fe.fetchProperty(OnOffTrait.STAT_VALUE).get());
        assertEquals("off", fe.fetchProperty(SceneTrait.STAT_SCENE_ID).get());

        fe.setProperty(SceneTrait.STAT_SCENE_ID, "dim").get();
        assertEquals(true, fe.fetchProperty(OnOffTrait.STAT_VALUE).get());
        assertEquals((Float) 0.25f, fe.fetchProperty(LevelTrait.STAT_VALUE).get());
        assertEquals("dim", fe.fetchProperty(SceneTrait.STAT_SCENE_ID).get());

        dimScene.setProperty(LevelTrait.STAT_VALUE, 0.5f).get();
        assertEquals((Float) 0.25f, fe.fetchProperty(LevelTrait.STAT_VALUE).get());
        fe.setProperty(SceneTrait.STAT_SCENE_ID, "dim").get();
        assertEquals((Float) 0.5f, fe.fetchProperty(LevelTrait.STAT_VALUE).get());

        assertEquals("dim", fe.fetchProperty(SceneTrait.STAT_SCENE_ID).get());
        fe.setProperty(LevelTrait.STAT_VALUE, 0.25f).get();
        assertNotEquals("dim", fe.fetchProperty(SceneTrait.STAT_SCENE_ID).get());

        assertEquals(true, offScene.delete().get());
        fe.setProperty(SceneTrait.STAT_SCENE_ID, "off").get();
        assertNotEquals(false, fe.fetchProperty(OnOffTrait.STAT_VALUE).get());

        verify(mChildListenerMock, timeout(100).only())
                .onChildRemoved(eq(fe), eq(SceneTrait.TRAIT_ID), eq(offScene));
        verify(mChildListenerMock, never()).onChildAdded(any(), any(), any());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void localScenePersistenceTest() throws Exception {
        Map<String, Object> persistentState;

        {
            NestedPersistentStateManager psm = new NestedPersistentStateManager();

            LocalTechnology technology = new LocalTechnology(mExecutor);

            LocalFunctionalEndpoint fe = new MyLightBulbNoTrans();

            psm.startManaging("technology", technology);
            psm.startManaging("bulb1", fe);

            technology.prepareToHost();
            technology.host(fe);

            fe.setProperty(LevelTrait.STAT_VALUE, 0.0f).get();
            fe.setProperty(OnOffTrait.STAT_VALUE, false).get();
            FunctionalEndpoint offScene =
                    fe.invokeMethod(SceneTrait.METHOD_SAVE, SceneTrait.PARAM_SCENE_ID.with("off")).get();

            fe.setProperty(LevelTrait.STAT_VALUE, 1.0f).get();
            fe.setProperty(OnOffTrait.STAT_VALUE, true).get();
            FunctionalEndpoint onScene =
                    fe.invokeMethod(SceneTrait.METHOD_SAVE, SceneTrait.PARAM_SCENE_ID.with("on")).get();

            fe.setProperty(LevelTrait.STAT_VALUE, 0.25f).get();
            FunctionalEndpoint dimScene =
                    fe.invokeMethod(SceneTrait.METHOD_SAVE, SceneTrait.PARAM_SCENE_ID.with("dim")).get();

            persistentState = psm.copyPersistentState();
            psm.close();
        }

        if (DEBUG) {
            LOGGER.info("persistentState = " + persistentState);
        }

        {
            NestedPersistentStateManager psm = new NestedPersistentStateManager();
            psm.initWithPersistentState(persistentState);

            LocalTechnology technology = new LocalTechnology(mExecutor);
            LocalFunctionalEndpoint fe = new MyLightBulbNoTrans();

            psm.startManaging("technology", technology);
            psm.startManaging("bulb1", fe);

            technology.host(fe);

            fe.setProperty(SceneTrait.STAT_SCENE_ID, "on").get();
            assertEquals(true, fe.fetchProperty(OnOffTrait.STAT_VALUE).get());
            assertEquals((Float) 1.0f, fe.fetchProperty(LevelTrait.STAT_VALUE).get());

            fe.setProperty(SceneTrait.STAT_SCENE_ID, "off").get();
            assertEquals(false, fe.fetchProperty(OnOffTrait.STAT_VALUE).get());

            fe.setProperty(SceneTrait.STAT_SCENE_ID, "dim").get();
            assertEquals(true, fe.fetchProperty(OnOffTrait.STAT_VALUE).get());
            assertEquals((Float) 0.25f, fe.fetchProperty(LevelTrait.STAT_VALUE).get());

            fe.registerChildListener(mExecutor, SceneTrait.TRAIT_ID, mChildListenerMock);

            tick(10);
            verify(mChildListenerMock, times(3))
                    .onChildAdded(eq(fe), eq(SceneTrait.TRAIT_ID), any());
            verify(mChildListenerMock, never()).onChildRemoved(any(), any(), any());
        }
    }
}
