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

import com.google.iot.m2m.base.FunctionalEndpoint;
import com.google.iot.m2m.base.Group;
import com.google.iot.m2m.trait.*;
import com.google.iot.m2m.util.NestedPersistentStateManager;
import java.util.Map;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;

@SuppressWarnings("ConstantConditions")
public class LocalGroupTest extends TestBase {
    private static final boolean DEBUG = false;
    private static final Logger LOGGER = Logger.getLogger(LocalGroupTest.class.getCanonicalName());

    @Test
    public void localGroupTest() throws Exception {
        LocalTechnology technology = new LocalTechnology(mExecutor);

        FunctionalEndpoint bulb1 = new MyLightBulbNoTrans();
        FunctionalEndpoint bulb2 = new MyLightBulbNoTrans();
        FunctionalEndpoint bulb3 = new MyLightBulbNoTrans();

        technology.prepareToHost();

        technology.host(bulb1);
        technology.host(bulb2);
        technology.host(bulb3);

        if (DEBUG) {
            bulb1.registerPropertyListener(
                    Runnable::run,
                    OnOffTrait.STAT_VALUE,
                    (fe, property, value) -> {
                        LOGGER.info(
                                "Bulb 1 Property changed! Key: " + property + " Value: " + value);
                    });

            bulb2.registerPropertyListener(
                    Runnable::run,
                    OnOffTrait.STAT_VALUE,
                    (fe, property, value) -> {
                        LOGGER.info(
                                "Bulb 2 Property changed! Key: " + property + " Value: " + value);
                    });

            bulb3.registerPropertyListener(
                    Runnable::run,
                    OnOffTrait.STAT_VALUE,
                    (fe, property, value) -> {
                        LOGGER.info(
                                "Bulb 3 Property changed! Key: " + property + " Value: " + value);
                    });
        }

        assertFalse(bulb1.fetchProperty(OnOffTrait.STAT_VALUE).get());
        assertFalse(bulb2.fetchProperty(OnOffTrait.STAT_VALUE).get());
        assertFalse(bulb3.fetchProperty(OnOffTrait.STAT_VALUE).get());

        Group livingRoom = technology.createNewGroup().get();

        technology.host(livingRoom);

        livingRoom.addMember(bulb1).get();
        livingRoom.addMember(bulb2).get();

        livingRoom.setProperty(OnOffTrait.STAT_VALUE, true).get();

        assertTrue(bulb1.fetchProperty(OnOffTrait.STAT_VALUE).get());
        assertTrue(bulb2.fetchProperty(OnOffTrait.STAT_VALUE).get());
        assertFalse(bulb3.fetchProperty(OnOffTrait.STAT_VALUE).get());

        livingRoom.setProperty(OnOffTrait.STAT_VALUE, false).get();

        assertFalse(bulb1.fetchProperty(OnOffTrait.STAT_VALUE).get());
        assertFalse(bulb2.fetchProperty(OnOffTrait.STAT_VALUE).get());
        assertFalse(bulb3.fetchProperty(OnOffTrait.STAT_VALUE).get());

        livingRoom.toggleProperty(OnOffTrait.STAT_VALUE).get();

        assertTrue(bulb1.fetchProperty(OnOffTrait.STAT_VALUE).get());
        assertTrue(bulb2.fetchProperty(OnOffTrait.STAT_VALUE).get());
        assertFalse(bulb3.fetchProperty(OnOffTrait.STAT_VALUE).get());

        bulb1.toggleProperty(OnOffTrait.STAT_VALUE).get();

        assertFalse(bulb1.fetchProperty(OnOffTrait.STAT_VALUE).get());
        assertTrue(bulb2.fetchProperty(OnOffTrait.STAT_VALUE).get());
        assertFalse(bulb3.fetchProperty(OnOffTrait.STAT_VALUE).get());

        livingRoom.toggleProperty(OnOffTrait.STAT_VALUE).get();

        assertTrue(bulb1.fetchProperty(OnOffTrait.STAT_VALUE).get());
        assertFalse(bulb2.fetchProperty(OnOffTrait.STAT_VALUE).get());
        assertFalse(bulb3.fetchProperty(OnOffTrait.STAT_VALUE).get());
    }

    @Test
    public void localGroupSceneTest() throws Exception {
        LocalTechnology technology = new LocalTechnology(mExecutor);

        FunctionalEndpoint bulb1 = new MyLightBulbNoTrans();
        FunctionalEndpoint bulb2 = new MyLightBulbNoTrans();
        FunctionalEndpoint bulb3 = new MyLightBulbNoTrans();

        technology.prepareToHost();

        technology.host(bulb1);
        technology.host(bulb2);
        technology.host(bulb3);

        if (DEBUG) {
            bulb1.registerPropertyListener(
                    Runnable::run,
                    OnOffTrait.STAT_VALUE,
                    (fe, property, value) -> {
                        LOGGER.info(
                                "Bulb 1 Property changed! Key: " + property + " Value: " + value);
                    });

            bulb2.registerPropertyListener(
                    Runnable::run,
                    OnOffTrait.STAT_VALUE,
                    (fe, property, value) -> {
                        LOGGER.info(
                                "Bulb 2 Property changed! Key: " + property + " Value: " + value);
                    });

            bulb3.registerPropertyListener(
                    Runnable::run,
                    OnOffTrait.STAT_VALUE,
                    (fe, property, value) -> {
                        LOGGER.info(
                                "Bulb 3 Property changed! Key: " + property + " Value: " + value);
                    });
        }

        assertFalse(bulb1.fetchProperty(OnOffTrait.STAT_VALUE).get());
        assertFalse(bulb2.fetchProperty(OnOffTrait.STAT_VALUE).get());
        assertFalse(bulb3.fetchProperty(OnOffTrait.STAT_VALUE).get());

        Group livingRoom = technology.createNewGroup().get();

        technology.host(livingRoom);

        livingRoom.addMember(bulb1).get();
        livingRoom.addMember(bulb2).get();
        livingRoom.addMember(bulb3).get();

        livingRoom.setProperty(OnOffTrait.STAT_VALUE, false).get();
        FunctionalEndpoint offScene =
                livingRoom
                        .invokeMethod(SceneTrait.METHOD_SAVE, SceneTrait.PARAM_SCENE_ID, "off")
                        .get();

        livingRoom.setProperty(LevelTrait.STAT_VALUE, 1.0f).get();
        livingRoom.setProperty(OnOffTrait.STAT_VALUE, true).get();
        FunctionalEndpoint onScene =
                livingRoom
                        .invokeMethod(SceneTrait.METHOD_SAVE, SceneTrait.PARAM_SCENE_ID, "on")
                        .get();

        bulb1.setProperty(LevelTrait.STAT_VALUE, 0.25f).get();
        bulb2.setProperty(LevelTrait.STAT_VALUE, 0.5f).get();
        bulb3.setProperty(OnOffTrait.STAT_VALUE, false).get();
        FunctionalEndpoint dimScene =
                livingRoom
                        .invokeMethod(SceneTrait.METHOD_SAVE, SceneTrait.PARAM_SCENE_ID, "dim")
                        .get();

        assertNull(bulb1.fetchProperty(SceneTrait.STAT_GROUP_ID).get());

        livingRoom.setProperty(SceneTrait.STAT_SCENE_ID, "off").get();
        assertFalse(bulb1.fetchProperty(OnOffTrait.STAT_VALUE).get());
        assertFalse(bulb2.fetchProperty(OnOffTrait.STAT_VALUE).get());
        assertFalse(bulb3.fetchProperty(OnOffTrait.STAT_VALUE).get());
        assertEquals(livingRoom.getGroupId(), bulb1.fetchProperty(SceneTrait.STAT_GROUP_ID).get());

        livingRoom.setProperty(SceneTrait.STAT_SCENE_ID, "on").get();
        assertTrue(bulb1.fetchProperty(OnOffTrait.STAT_VALUE).get());
        assertTrue(bulb2.fetchProperty(OnOffTrait.STAT_VALUE).get());
        assertTrue(bulb3.fetchProperty(OnOffTrait.STAT_VALUE).get());

        livingRoom.setProperty(SceneTrait.STAT_SCENE_ID, "dim").get();
        assertTrue(bulb1.fetchProperty(OnOffTrait.STAT_VALUE).get());
        assertTrue(bulb2.fetchProperty(OnOffTrait.STAT_VALUE).get());
        assertFalse(bulb3.fetchProperty(OnOffTrait.STAT_VALUE).get());
    }

    @Test
    public void localGroupInBandTest() throws Exception {
        LocalTechnology technology = new LocalTechnology(mExecutor);

        FunctionalEndpoint bulb1 = new MyLightBulbNoTrans();
        FunctionalEndpoint bulb2 = new MyLightBulbNoTrans();
        FunctionalEndpoint bulb3 = new MyLightBulbNoTrans();

        technology.prepareToHost();

        technology.host(bulb1);
        technology.host(bulb2);
        technology.host(bulb3);

        if (DEBUG) {
            bulb1.registerPropertyListener(
                    Runnable::run,
                    OnOffTrait.STAT_VALUE,
                    (fe, property, value) -> {
                        LOGGER.info(
                                "Bulb 1 Property changed! Key: " + property + " Value: " + value);
                    });

            bulb2.registerPropertyListener(
                    Runnable::run,
                    OnOffTrait.STAT_VALUE,
                    (fe, property, value) -> {
                        LOGGER.info(
                                "Bulb 2 Property changed! Key: " + property + " Value: " + value);
                    });

            bulb3.registerPropertyListener(
                    Runnable::run,
                    OnOffTrait.STAT_VALUE,
                    (fe, property, value) -> {
                        LOGGER.info(
                                "Bulb 3 Property changed! Key: " + property + " Value: " + value);
                    });
        }

        assertFalse(bulb1.fetchProperty(OnOffTrait.STAT_VALUE).get());
        assertFalse(bulb2.fetchProperty(OnOffTrait.STAT_VALUE).get());
        assertFalse(bulb3.fetchProperty(OnOffTrait.STAT_VALUE).get());

        Group livingRoom = technology.createNewGroup().get();

        technology.host(livingRoom);

        livingRoom.addMember(bulb1).get();
        livingRoom.addMember(bulb2).get();
        livingRoom
                .addValueToProperty(
                        GroupTrait.CONF_LOCAL_MEMBERS, bulb3.getCachedProperty(BaseTrait.META_UID))
                .get();

        livingRoom.setProperty(OnOffTrait.STAT_VALUE, true).get();

        assertTrue(bulb1.fetchProperty(OnOffTrait.STAT_VALUE).get());
        assertTrue(bulb2.fetchProperty(OnOffTrait.STAT_VALUE).get());
        assertTrue(bulb3.fetchProperty(OnOffTrait.STAT_VALUE).get());

        livingRoom
                .removeValueFromProperty(
                        GroupTrait.CONF_LOCAL_MEMBERS, bulb1.getCachedProperty(BaseTrait.META_UID))
                .get();

        livingRoom.setProperty(OnOffTrait.STAT_VALUE, false).get();

        assertTrue(bulb1.fetchProperty(OnOffTrait.STAT_VALUE).get());
        assertFalse(bulb2.fetchProperty(OnOffTrait.STAT_VALUE).get());
        assertFalse(bulb3.fetchProperty(OnOffTrait.STAT_VALUE).get());
    }

    @Test
    public void localGroupPersistenceTest() throws Exception {
        String livingRoomGroupId;
        Map<String, Object> persistentState;

        {
            NestedPersistentStateManager psm = new NestedPersistentStateManager();

            LocalTechnology technology = new LocalTechnology(mExecutor);
            LocalFunctionalEndpoint bulb1 = new MyLightBulbNoTrans();
            LocalFunctionalEndpoint bulb2 = new MyLightBulbNoTrans();
            LocalFunctionalEndpoint bulb3 = new MyLightBulbNoTrans();

            psm.startManaging("technology", technology);
            psm.startManaging("bulb1", bulb1);
            psm.startManaging("bulb2", bulb2);
            psm.startManaging("bulb3", bulb3);

            technology.prepareToHost();

            technology.host(bulb1);
            technology.host(bulb2);
            technology.host(bulb3);

            Group livingRoom = technology.createNewGroup().get();

            technology.host(livingRoom);

            livingRoomGroupId = livingRoom.getGroupId();

            livingRoom.addMember(bulb1).get();
            livingRoom.addMember(bulb2).get();

            persistentState = psm.copyPersistentState();
            psm.close();
        }

        if (DEBUG) {
            LOGGER.info("livingRoomGroupId = " + livingRoomGroupId);
            LOGGER.info("persistentState = " + persistentState);
        }

        {
            NestedPersistentStateManager psm = new NestedPersistentStateManager();
            psm.initWithPersistentState(persistentState);

            LocalTechnology technology = new LocalTechnology(mExecutor);
            LocalFunctionalEndpoint bulb1 = new MyLightBulbNoTrans();
            LocalFunctionalEndpoint bulb2 = new MyLightBulbNoTrans();
            LocalFunctionalEndpoint bulb3 = new MyLightBulbNoTrans();

            psm.startManaging("technology", technology);
            psm.startManaging("bulb1", bulb1);
            psm.startManaging("bulb2", bulb2);
            psm.startManaging("bulb3", bulb3);

            technology.host(bulb1);
            technology.host(bulb2);
            technology.host(bulb3);

            Group livingRoom = technology.fetchOrCreateGroupWithId(livingRoomGroupId).get();

            assertNotNull(livingRoom);

            livingRoom.setProperty(OnOffTrait.STAT_VALUE, true).get();

            assertTrue(bulb1.fetchProperty(OnOffTrait.STAT_VALUE).get());
            assertTrue(bulb2.fetchProperty(OnOffTrait.STAT_VALUE).get());
            assertFalse(bulb3.fetchProperty(OnOffTrait.STAT_VALUE).get());

            livingRoom.setProperty(OnOffTrait.STAT_VALUE, false).get();

            assertFalse(bulb1.fetchProperty(OnOffTrait.STAT_VALUE).get());
            assertFalse(bulb2.fetchProperty(OnOffTrait.STAT_VALUE).get());
            assertFalse(bulb3.fetchProperty(OnOffTrait.STAT_VALUE).get());

            livingRoom.toggleProperty(OnOffTrait.STAT_VALUE).get();

            assertTrue(bulb1.fetchProperty(OnOffTrait.STAT_VALUE).get());
            assertTrue(bulb2.fetchProperty(OnOffTrait.STAT_VALUE).get());
            assertFalse(bulb3.fetchProperty(OnOffTrait.STAT_VALUE).get());

            bulb1.toggleProperty(OnOffTrait.STAT_VALUE).get();

            assertFalse(bulb1.fetchProperty(OnOffTrait.STAT_VALUE).get());
            assertTrue(bulb2.fetchProperty(OnOffTrait.STAT_VALUE).get());
            assertFalse(bulb3.fetchProperty(OnOffTrait.STAT_VALUE).get());

            livingRoom.toggleProperty(OnOffTrait.STAT_VALUE).get();

            assertTrue(bulb1.fetchProperty(OnOffTrait.STAT_VALUE).get());
            assertFalse(bulb2.fetchProperty(OnOffTrait.STAT_VALUE).get());
            assertFalse(bulb3.fetchProperty(OnOffTrait.STAT_VALUE).get());
        }
    }
}
