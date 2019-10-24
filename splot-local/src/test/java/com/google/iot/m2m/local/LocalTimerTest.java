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

import com.google.iot.m2m.base.Thing;
import com.google.iot.m2m.base.Modifier;
import com.google.iot.m2m.base.Operation;
import com.google.iot.m2m.base.UnacceptableThingException;
import com.google.iot.m2m.trait.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class LocalTimerTest extends TestBase {
    private static final boolean DEBUG = false;
    private static final Logger LOGGER =
            Logger.getLogger(LocalThingTest.class.getCanonicalName());

    LocalTechnology technology = null;
    Thing bulb1 = null;
    Thing bulb2 = null;
    LocalTimer timer = null;

    @BeforeEach
    @Override
    public void before() {
        super.before();

        technology = new LocalTechnology(mExecutor);

        bulb1 = new MyLightBulbNoTrans();
        bulb2 = new MyLightBulbNoTrans();
        timer = new LocalTimer(technology);

        technology.prepareToHost();

        try {
            technology.host(bulb1);
            technology.host(bulb2);
            technology.host(timer);
        } catch (UnacceptableThingException e) {
            throw new AssertionError(e);
        }

        if (DEBUG) {
            timer.registerPropertyListener(
                    Runnable::run,
                    ActionsTrait.STAT_COUNT,
                    (fe, property, value) -> {
                        LOGGER.info(
                                "Timer Property changed! Key: " + property + " Value: " + value);
                    });
            timer.registerPropertyListener(
                    Runnable::run,
                    AutomationTimerTrait.STAT_RUNNING,
                    (fe, property, value) -> {
                        LOGGER.info(
                                "Timer Property changed! Key: " + property + " Value: " + value);
                    });
            timer.registerPropertyListener(
                    Runnable::run,
                    BaseTrait.STAT_TRAP,
                    (fe, property, value) -> {
                        LOGGER.info(
                                "Timer Property changed! Key: " + property + " Value: " + value);
                    });
            bulb1.registerPropertyListener(
                    Runnable::run,
                    LevelTrait.STAT_VALUE,
                    (fe, property, value) -> {
                        LOGGER.info(
                                "bulb1 Property changed! Key: " + property + " Value: " + value);
                    });
        }

        try {
            assertFalse(bulb1.fetchProperty(OnOffTrait.STAT_VALUE).get());
            assertFalse(bulb2.fetchProperty(OnOffTrait.STAT_VALUE).get());
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    @AfterEach
    @Override
    public void after() {
        try {
            timer.setProperty(EnabledDisabledTrait.STAT_VALUE, false).get();
            technology.unhost(timer);
            technology.unhost(bulb1);
            technology.unhost(bulb2);
            timer = null;
            bulb1 = null;
            bulb2 = null;
            technology = null;
        } catch (Exception e) {
            throw new AssertionError(e);
        }

        super.after();
    }

    @Test
    public void simpleTimerTest() throws Exception {
        Map<String,Object> action = new HashMap<>();

        assertEquals(0.0f, (float)bulb1.fetchProperty(LevelTrait.STAT_VALUE).get());
        assertFalse(timer.fetchProperty(AutomationTimerTrait.STAT_RUNNING).get());

        ActionsTrait.PARAM_ACTION_PATH.putInMap(action,
                technology.getNativeUriForProperty(bulb1, LevelTrait.STAT_VALUE));

        ActionsTrait.PARAM_ACTION_BODY.putInMap(action, 0.2);

        timer.insertValueIntoProperty(ActionsTrait.CONF_ACTIONS, action).get();
        timer.setProperty(AutomationTimerTrait.CONF_SCHEDULE_PROGRAM, "0.2").get();
        //timer.setProperty(AutomationTimerTrait.CONF_AUTO_RESET, true).get();
        timer.setProperty(EnabledDisabledTrait.STAT_VALUE, true).get();
        timer.invokeMethod(AutomationTimerTrait.METHOD_RESET);

        tick(100);

        assertTrue(timer.fetchProperty(AutomationTimerTrait.STAT_RUNNING).get());

        assertEquals(0.0f, (float)bulb1.fetchProperty(LevelTrait.STAT_VALUE).get());

        tick(150);

        assertEquals(0.2f, (float)bulb1.fetchProperty(LevelTrait.STAT_VALUE).get());

        tick(300);

        assertEquals(0.2f, (float)bulb1.fetchProperty(LevelTrait.STAT_VALUE).get());

        assertFalse(timer.fetchProperty(AutomationTimerTrait.STAT_RUNNING).get());
    }

    @Test
    public void autoResetTimerTest() throws Exception {
        Map<String,Object> action = new HashMap<>();

        assertEquals(0.0f, (float)bulb1.fetchProperty(LevelTrait.STAT_VALUE).get());
        assertFalse(timer.fetchProperty(AutomationTimerTrait.STAT_RUNNING).get());

        ActionsTrait.PARAM_ACTION_PATH.putInMap(action,
                technology.getNativeUriForProperty(bulb1, LevelTrait.STAT_VALUE, Operation.INCREMENT));

        ActionsTrait.PARAM_ACTION_BODY.putInMap(action, 0.2);

        timer.insertValueIntoProperty(ActionsTrait.CONF_ACTIONS, action).get();
        timer.setProperty(AutomationTimerTrait.CONF_SCHEDULE_PROGRAM, "0.2").get();
        timer.setProperty(AutomationTimerTrait.CONF_AUTO_RESET, true).get();
        timer.setProperty(EnabledDisabledTrait.STAT_VALUE, true).get();

        tick(100);

        assertEquals(0.0f, (float)bulb1.fetchProperty(LevelTrait.STAT_VALUE).get());

        tick(150);

        assertEquals(0.2f, (float)bulb1.fetchProperty(LevelTrait.STAT_VALUE).get());
        assertTrue(timer.fetchProperty(AutomationTimerTrait.STAT_RUNNING).get());

        tick(200);

        assertEquals(0.4f, (float)bulb1.fetchProperty(LevelTrait.STAT_VALUE).get());
        assertTrue(timer.fetchProperty(AutomationTimerTrait.STAT_RUNNING).get());

        timer.setProperty(AutomationTimerTrait.STAT_RUNNING, false).get();

        assertFalse(timer.fetchProperty(AutomationTimerTrait.STAT_RUNNING).get());

        tick(300);

        assertEquals(0.4f, (float)bulb1.fetchProperty(LevelTrait.STAT_VALUE).get());
    }

    @Test
    public void autoResetTimerScheduleCancelTest() throws Exception {
        Map<String,Object> action = new HashMap<>();

        assertEquals(0.0f, (float)bulb1.fetchProperty(LevelTrait.STAT_VALUE).get());
        assertFalse(timer.fetchProperty(AutomationTimerTrait.STAT_RUNNING).get());

        ActionsTrait.PARAM_ACTION_PATH.putInMap(action,
                technology.getNativeUriForProperty(bulb1, LevelTrait.STAT_VALUE, Operation.INCREMENT));

        ActionsTrait.PARAM_ACTION_BODY.putInMap(action, 0.2);

        timer.insertValueIntoProperty(ActionsTrait.CONF_ACTIONS, action).get();

        // This schedule program will stop the timer after two runs.
        timer.setProperty(AutomationTimerTrait.CONF_SCHEDULE_PROGRAM, "c 2 < IF 0.2 ENDIF").get();

        timer.setProperty(AutomationTimerTrait.CONF_AUTO_RESET, true).get();
        timer.setProperty(EnabledDisabledTrait.STAT_VALUE, true).get();

        tick(100);

        assertEquals(0.0f, (float)bulb1.fetchProperty(LevelTrait.STAT_VALUE).get());

        tick(150);

        assertEquals(0.2f, (float)bulb1.fetchProperty(LevelTrait.STAT_VALUE).get());
        assertTrue(timer.fetchProperty(AutomationTimerTrait.STAT_RUNNING).get());

        tick(200);

        assertEquals(0.4f, (float)bulb1.fetchProperty(LevelTrait.STAT_VALUE).get());
        assertFalse(timer.fetchProperty(AutomationTimerTrait.STAT_RUNNING).get());

        tick(200);

        assertEquals(0.4f, (float)bulb1.fetchProperty(LevelTrait.STAT_VALUE).get());
        assertFalse(timer.fetchProperty(AutomationTimerTrait.STAT_RUNNING).get());
    }
}
