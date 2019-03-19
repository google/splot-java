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

import com.google.iot.m2m.base.FunctionalEndpoint;
import com.google.iot.m2m.base.PropertyKey;
import com.google.iot.m2m.base.UnacceptableFunctionalEndpointException;
import com.google.iot.m2m.trait.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class LocalPairingTest extends TestBase {
    private static final boolean DEBUG = false;
    private static final Logger LOGGER =
            Logger.getLogger(LocalFunctionalEndpointTest.class.getCanonicalName());

    LocalTechnology technology = null;
    FunctionalEndpoint bulb1 = null;
    FunctionalEndpoint bulb2 = null;
    LocalPairing pairing = null;

    @BeforeEach
    @Override
    public void before() {
        super.before();

        technology = new LocalTechnology(mExecutor);

        bulb1 = new MyLightBulbNoTrans();
        bulb2 = new MyLightBulbNoTrans();
        pairing = new LocalPairing(technology);

        technology.prepareToHost();

        try {
            technology.host(bulb1);
            technology.host(bulb2);
            technology.host(pairing);
        } catch (UnacceptableFunctionalEndpointException e) {
            throw new AssertionError(e);
        }

        if (DEBUG) {
            pairing.registerPropertyListener(
                    Runnable::run,
                    AutomationPairingTrait.STAT_COUNT,
                    (fe, property, value) -> {
                        LOGGER.info(
                                "Pairing Property changed! Key: " + property + " Value: " + value);
                    });
            pairing.registerPropertyListener(
                    Runnable::run,
                    BaseTrait.STAT_TRAP,
                    (fe, property, value) -> {
                        LOGGER.info(
                                "Pairing Property changed! Key: " + property + " Value: " + value);
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
            pairing.setProperty(EnabledDisabledTrait.STAT_VALUE, false).get();
            technology.unhost(pairing);
            technology.unhost(bulb1);
            technology.unhost(bulb2);
            pairing = null;
            bulb1 = null;
            bulb2 = null;
            technology = null;
        } catch (Exception e) {
            throw new AssertionError(e);
        }

        super.after();
    }

    void setupOnOffPairing() throws Exception {
        URI source = technology.getNativeUriForFunctionalEndpoint(bulb1);
        assertNotNull(source);
        source = source.resolve(OnOffTrait.STAT_VALUE.getName());

        URI destination = technology.getNativeUriForFunctionalEndpoint(bulb2);
        assertNotNull(destination);
        destination = destination.resolve(OnOffTrait.STAT_VALUE.getName());

        if (DEBUG) {
            LOGGER.info("source = " + source);
            LOGGER.info("destination = " + destination);
        }

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
        }

        pairing.setProperty(AutomationPairingTrait.CONF_SOURCE, source).get();
        pairing.setProperty(AutomationPairingTrait.CONF_DESTINATION, destination).get();

        tick(10);
    }

    void setupLevelPairing() throws Exception {
        URI source = technology.getNativeUriForFunctionalEndpoint(bulb1);
        assertNotNull(source);
        source = source.resolve(LevelTrait.STAT_VALUE.getName());

        URI destination = technology.getNativeUriForFunctionalEndpoint(bulb2);
        assertNotNull(destination);
        destination = destination.resolve(LevelTrait.STAT_VALUE.getName());

        if (DEBUG) {
            LOGGER.info("source = " + source);
            LOGGER.info("destination = " + destination);
        }

        if (DEBUG) {
            bulb1.registerPropertyListener(
                    Runnable::run,
                    LevelTrait.STAT_VALUE,
                    (fe, property, value) -> {
                        LOGGER.info(
                                "Bulb 1 Property changed! Key: " + property + " Value: " + value);
                    });

            bulb2.registerPropertyListener(
                    Runnable::run,
                    LevelTrait.STAT_VALUE,
                    (fe, property, value) -> {
                        LOGGER.info(
                                "Bulb 2 Property changed! Key: " + property + " Value: " + value);
                    });
        }

        pairing.setProperty(AutomationPairingTrait.CONF_SOURCE, source).get();
        pairing.setProperty(AutomationPairingTrait.CONF_DESTINATION, destination).get();

        tick(10);
    }

    void setupStatePairing() throws Exception {
        URI source = technology.getNativeUriForFunctionalEndpoint(bulb1);
        assertNotNull(source);
        source = source.resolve(PropertyKey.SECTION_STATE);

        URI destination = technology.getNativeUriForFunctionalEndpoint(bulb2);
        assertNotNull(destination);
        destination = destination.resolve(PropertyKey.SECTION_STATE);

        if (DEBUG) {
            LOGGER.info("source = " + source);
            LOGGER.info("destination = " + destination);
        }

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
            bulb1.registerPropertyListener(
                    Runnable::run,
                    LevelTrait.STAT_VALUE,
                    (fe, property, value) -> {
                        LOGGER.info(
                                "Bulb 1 Property changed! Key: " + property + " Value: " + value);
                    });

            bulb2.registerPropertyListener(
                    Runnable::run,
                    LevelTrait.STAT_VALUE,
                    (fe, property, value) -> {
                        LOGGER.info(
                                "Bulb 2 Property changed! Key: " + property + " Value: " + value);
                    });
        }

        pairing.setProperty(AutomationPairingTrait.CONF_SOURCE, source).get();
        pairing.setProperty(AutomationPairingTrait.CONF_DESTINATION, destination).get();

        tick(10);
    }

    @Test
    public void ForwardTransformTest() throws Exception {
        bulb1.setProperty(LevelTrait.STAT_VALUE, 0.0f).get();
        bulb2.setProperty(LevelTrait.STAT_VALUE, 0.0f).get();

        setupLevelPairing();

        pairing.setProperty(AutomationPairingTrait.CONF_FORWARD_TRANSFORM, "0.5 *").get();

        int initialCount = pairing.fetchProperty(AutomationPairingTrait.STAT_COUNT).get();

        bulb1.setProperty(LevelTrait.STAT_VALUE, 1.0f).get();

        tick(10);

        assertEquals(1.0f, (float)bulb1.fetchProperty(LevelTrait.STAT_VALUE).get());
        assertEquals(0.5f, (float)bulb2.fetchProperty(LevelTrait.STAT_VALUE).get());
        assertEquals(initialCount+1, (int)pairing.fetchProperty(AutomationPairingTrait.STAT_COUNT).get());

        bulb1.setProperty(LevelTrait.STAT_VALUE, 0.5f).get();
        tick(10);

        assertEquals(0.5f, (float)bulb1.fetchProperty(LevelTrait.STAT_VALUE).get());
        assertEquals(0.25f, (float)bulb2.fetchProperty(LevelTrait.STAT_VALUE).get());
        assertEquals(initialCount+2, (int)pairing.fetchProperty(AutomationPairingTrait.STAT_COUNT).get());

        bulb2.setProperty(LevelTrait.STAT_VALUE, 0.5f).get();
        tick(10);

        assertEquals(0.5f, (float)bulb1.fetchProperty(LevelTrait.STAT_VALUE).get());
        assertEquals(0.5f, (float)bulb2.fetchProperty(LevelTrait.STAT_VALUE).get());
        assertEquals(initialCount+2, (int)pairing.fetchProperty(AutomationPairingTrait.STAT_COUNT).get());
    }

    @Test
    public void dropTest() throws Exception {
        bulb1.setProperty(LevelTrait.STAT_VALUE, 0.0f).get();
        bulb2.setProperty(LevelTrait.STAT_VALUE, 0.0f).get();

        setupLevelPairing();

        pairing.setProperty(AutomationPairingTrait.CONF_FORWARD_TRANSFORM, "DUP 0.5 <= IF STOP ENDIF").get();

        int initialCount = pairing.fetchProperty(AutomationPairingTrait.STAT_COUNT).get();

        bulb1.setProperty(LevelTrait.STAT_VALUE, 1.0f).get();

        tick(10);

        assertEquals(1.0f, (float)bulb1.fetchProperty(LevelTrait.STAT_VALUE).get());
        assertEquals(1.0f, (float)bulb2.fetchProperty(LevelTrait.STAT_VALUE).get());
        assertEquals(initialCount+1, (int)pairing.fetchProperty(AutomationPairingTrait.STAT_COUNT).get());

        bulb1.setProperty(LevelTrait.STAT_VALUE, 0.6f).get();
        tick(10);

        assertEquals(0.6f, (float)bulb1.fetchProperty(LevelTrait.STAT_VALUE).get());
        assertEquals(0.6f, (float)bulb2.fetchProperty(LevelTrait.STAT_VALUE).get());
        assertEquals(initialCount+2, (int)pairing.fetchProperty(AutomationPairingTrait.STAT_COUNT).get());

        bulb1.setProperty(LevelTrait.STAT_VALUE, 0.4f).get();
        tick(10);

        assertEquals(0.4f, (float)bulb1.fetchProperty(LevelTrait.STAT_VALUE).get());
        assertEquals(0.6f, (float)bulb2.fetchProperty(LevelTrait.STAT_VALUE).get());
        assertEquals(initialCount+2, (int)pairing.fetchProperty(AutomationPairingTrait.STAT_COUNT).get());

        bulb1.setProperty(LevelTrait.STAT_VALUE, 0.2f).get();
        tick(10);

        assertEquals(0.2f, (float)bulb1.fetchProperty(LevelTrait.STAT_VALUE).get());
        assertEquals(0.6f, (float)bulb2.fetchProperty(LevelTrait.STAT_VALUE).get());
        assertEquals(initialCount+2, (int)pairing.fetchProperty(AutomationPairingTrait.STAT_COUNT).get());

        bulb1.setProperty(LevelTrait.STAT_VALUE, 0.7f).get();
        tick(10);

        assertEquals(0.7f, (float)bulb1.fetchProperty(LevelTrait.STAT_VALUE).get());
        assertEquals(0.7f, (float)bulb2.fetchProperty(LevelTrait.STAT_VALUE).get());
        assertEquals(initialCount+3, (int)pairing.fetchProperty(AutomationPairingTrait.STAT_COUNT).get());
    }

    @Test
    public void ReverseTransformTest() throws Exception {
        bulb1.setProperty(LevelTrait.STAT_VALUE, 0.0f).get();
        bulb2.setProperty(LevelTrait.STAT_VALUE, 0.0f).get();

        setupLevelPairing();

        pairing.setProperty(AutomationPairingTrait.CONF_PUSH, false).get();
        pairing.setProperty(AutomationPairingTrait.CONF_PULL, true).get();
        pairing.setProperty(AutomationPairingTrait.CONF_REVERSE_TRANSFORM, "0.5 *").get();

        int initialCount = pairing.fetchProperty(AutomationPairingTrait.STAT_COUNT).get();

        bulb2.setProperty(LevelTrait.STAT_VALUE, 1.0f).get();

        tick(10);

        assertEquals(1.0f, (float)bulb2.fetchProperty(LevelTrait.STAT_VALUE).get());
        assertEquals(0.5f, (float)bulb1.fetchProperty(LevelTrait.STAT_VALUE).get());
        assertEquals(initialCount+1, (int)pairing.fetchProperty(AutomationPairingTrait.STAT_COUNT).get());

        bulb2.setProperty(LevelTrait.STAT_VALUE, 0.5f).get();
        tick(10);

        assertEquals(0.5f, (float)bulb2.fetchProperty(LevelTrait.STAT_VALUE).get());
        assertEquals(0.25f, (float)bulb1.fetchProperty(LevelTrait.STAT_VALUE).get());
        assertEquals(initialCount+2, (int)pairing.fetchProperty(AutomationPairingTrait.STAT_COUNT).get());

        bulb1.setProperty(LevelTrait.STAT_VALUE, 0.5f).get();
        tick(10);

        assertEquals(0.5f, (float)bulb2.fetchProperty(LevelTrait.STAT_VALUE).get());
        assertEquals(0.5f, (float)bulb1.fetchProperty(LevelTrait.STAT_VALUE).get());
        assertEquals(initialCount+2, (int)pairing.fetchProperty(AutomationPairingTrait.STAT_COUNT).get());
    }

    @Test
    public void FullTransformTest() throws Exception {
        bulb1.setProperty(LevelTrait.STAT_VALUE, 0.0f).get();
        bulb2.setProperty(LevelTrait.STAT_VALUE, 0.0f).get();

        setupLevelPairing();

        pairing.setProperty(AutomationPairingTrait.CONF_PUSH, true).get();
        pairing.setProperty(AutomationPairingTrait.CONF_PULL, true).get();
        pairing.setProperty(AutomationPairingTrait.CONF_FORWARD_TRANSFORM, "0.5 *").get();
        pairing.setProperty(AutomationPairingTrait.CONF_REVERSE_TRANSFORM, "2.0 *").get();

        int initialCount = pairing.fetchProperty(AutomationPairingTrait.STAT_COUNT).get();

        bulb1.setProperty(LevelTrait.STAT_VALUE, 1.0f).get();

        tick(10);
        assertEquals(1.0f, (float)bulb1.fetchProperty(LevelTrait.STAT_VALUE).get());
        assertEquals(0.5f, (float)bulb2.fetchProperty(LevelTrait.STAT_VALUE).get());
        assertEquals(initialCount+1, (int)pairing.fetchProperty(AutomationPairingTrait.STAT_COUNT).get());

        bulb1.setProperty(LevelTrait.STAT_VALUE, 0.5f).get();
        tick(10);

        assertEquals(0.5f, (float)bulb1.fetchProperty(LevelTrait.STAT_VALUE).get());
        assertEquals(0.25f, (float)bulb2.fetchProperty(LevelTrait.STAT_VALUE).get());
        assertEquals(initialCount+2, (int)pairing.fetchProperty(AutomationPairingTrait.STAT_COUNT).get());

        bulb2.setProperty(LevelTrait.STAT_VALUE, 0.5f).get();
        tick(10);

        assertEquals(1.0f, (float)bulb1.fetchProperty(LevelTrait.STAT_VALUE).get());
        assertEquals(0.5f, (float)bulb2.fetchProperty(LevelTrait.STAT_VALUE).get());
        assertEquals(initialCount+3, (int)pairing.fetchProperty(AutomationPairingTrait.STAT_COUNT).get());
    }

    @Test
    public void statePushTest() throws Exception {
        setupStatePairing();

        assertTrue(pairing.fetchProperty(EnabledDisabledTrait.STAT_VALUE).get());
        assertTrue(pairing.fetchProperty(AutomationPairingTrait.CONF_PUSH).get());
        assertFalse(pairing.fetchProperty(AutomationPairingTrait.CONF_PULL).get());

        int initialCount = pairing.fetchProperty(AutomationPairingTrait.STAT_COUNT).get();

        bulb1.setProperty(OnOffTrait.STAT_VALUE, true).get();

        tick(10);

        assertTrue(bulb1.fetchProperty(OnOffTrait.STAT_VALUE).get());
        assertTrue(bulb2.fetchProperty(OnOffTrait.STAT_VALUE).get());
        assertEquals(initialCount+1, (int)pairing.fetchProperty(AutomationPairingTrait.STAT_COUNT).get());

        bulb1.setProperty(OnOffTrait.STAT_VALUE, false).get();

        tick(10);

        assertFalse(bulb1.fetchProperty(OnOffTrait.STAT_VALUE).get());
        assertFalse(bulb2.fetchProperty(OnOffTrait.STAT_VALUE).get());
        assertEquals(initialCount+2, (int)pairing.fetchProperty(AutomationPairingTrait.STAT_COUNT).get());

        bulb2.setProperty(OnOffTrait.STAT_VALUE, true).get();

        tick(10);

        assertFalse(bulb1.fetchProperty(OnOffTrait.STAT_VALUE).get());
        assertTrue(bulb2.fetchProperty(OnOffTrait.STAT_VALUE).get());
        assertEquals(initialCount+2, (int)pairing.fetchProperty(AutomationPairingTrait.STAT_COUNT).get());

        bulb1.setProperty(LevelTrait.STAT_VALUE, 1.0f).get();

        tick(10);

        assertEquals(1.0f, (float)bulb1.fetchProperty(LevelTrait.STAT_VALUE).get());
        assertEquals(1.0f, (float)bulb2.fetchProperty(LevelTrait.STAT_VALUE).get());
        assertFalse(bulb1.fetchProperty(OnOffTrait.STAT_VALUE).get());
        assertFalse(bulb2.fetchProperty(OnOffTrait.STAT_VALUE).get());
        assertEquals(initialCount+3, (int)pairing.fetchProperty(AutomationPairingTrait.STAT_COUNT).get());

        bulb1.setProperty(LevelTrait.STAT_VALUE, 0.5f).get();

        tick(10);

        assertEquals(0.5f, (float)bulb1.fetchProperty(LevelTrait.STAT_VALUE).get());
        assertEquals(0.5f, (float)bulb2.fetchProperty(LevelTrait.STAT_VALUE).get());
        assertEquals(initialCount+4, (int)pairing.fetchProperty(AutomationPairingTrait.STAT_COUNT).get());

        bulb2.setProperty(LevelTrait.STAT_VALUE, 0.25f).get();

        tick(10);

        assertEquals(0.5f, (float)bulb1.fetchProperty(LevelTrait.STAT_VALUE).get());
        assertEquals(0.25f, (float)bulb2.fetchProperty(LevelTrait.STAT_VALUE).get());
        assertEquals(initialCount+4, (int)pairing.fetchProperty(AutomationPairingTrait.STAT_COUNT).get());
    }

    @Test
    public void simplePushTest() throws Exception {
        setupOnOffPairing();

        assertTrue(pairing.fetchProperty(EnabledDisabledTrait.STAT_VALUE).get());
        assertTrue(pairing.fetchProperty(AutomationPairingTrait.CONF_PUSH).get());
        assertFalse(pairing.fetchProperty(AutomationPairingTrait.CONF_PULL).get());

        int initialCount = pairing.fetchProperty(AutomationPairingTrait.STAT_COUNT).get();

        bulb1.setProperty(OnOffTrait.STAT_VALUE, true).get();

        tick(10);

        assertTrue(bulb1.fetchProperty(OnOffTrait.STAT_VALUE).get());
        assertTrue(bulb2.fetchProperty(OnOffTrait.STAT_VALUE).get());
        assertEquals(initialCount+1, (int)pairing.fetchProperty(AutomationPairingTrait.STAT_COUNT).get());

        bulb1.setProperty(OnOffTrait.STAT_VALUE, false).get();

        tick(10);

        assertFalse(bulb1.fetchProperty(OnOffTrait.STAT_VALUE).get());
        assertFalse(bulb2.fetchProperty(OnOffTrait.STAT_VALUE).get());
        assertEquals(initialCount+2, (int)pairing.fetchProperty(AutomationPairingTrait.STAT_COUNT).get());

        bulb2.setProperty(OnOffTrait.STAT_VALUE, true).get();

        tick(10);

        assertFalse(bulb1.fetchProperty(OnOffTrait.STAT_VALUE).get());
        assertTrue(bulb2.fetchProperty(OnOffTrait.STAT_VALUE).get());
        assertEquals(initialCount+2, (int)pairing.fetchProperty(AutomationPairingTrait.STAT_COUNT).get());
    }

    @Test
    public void simplePullTest() throws Exception {
        setupOnOffPairing();

        pairing.setProperty(AutomationPairingTrait.CONF_PUSH, false).get();
        pairing.setProperty(AutomationPairingTrait.CONF_PULL, true).get();

        tick(10);

        assertTrue(pairing.fetchProperty(EnabledDisabledTrait.STAT_VALUE).get());
        assertFalse(pairing.fetchProperty(AutomationPairingTrait.CONF_PUSH).get());
        assertTrue(pairing.fetchProperty(AutomationPairingTrait.CONF_PULL).get());

        int initialCount = pairing.fetchProperty(AutomationPairingTrait.STAT_COUNT).get();

        bulb2.setProperty(OnOffTrait.STAT_VALUE, true).get();

        tick(10);

        assertTrue(bulb1.fetchProperty(OnOffTrait.STAT_VALUE).get());
        assertTrue(bulb2.fetchProperty(OnOffTrait.STAT_VALUE).get());
        assertEquals(initialCount + 1, (int)pairing.fetchProperty(AutomationPairingTrait.STAT_COUNT).get());

        bulb2.setProperty(OnOffTrait.STAT_VALUE, false).get();

        tick(10);

        assertFalse(bulb1.fetchProperty(OnOffTrait.STAT_VALUE).get());
        assertFalse(bulb2.fetchProperty(OnOffTrait.STAT_VALUE).get());
        assertEquals(initialCount + 2, (int)pairing.fetchProperty(AutomationPairingTrait.STAT_COUNT).get());

        bulb1.setProperty(OnOffTrait.STAT_VALUE, true).get();

        tick(10);

        assertTrue(bulb1.fetchProperty(OnOffTrait.STAT_VALUE).get());
        assertFalse(bulb2.fetchProperty(OnOffTrait.STAT_VALUE).get());
        assertEquals(initialCount + 2, (int)pairing.fetchProperty(AutomationPairingTrait.STAT_COUNT).get());
    }


    @Test
    public void simpleSyncTest() throws Exception {
        setupOnOffPairing();

        pairing.setProperty(AutomationPairingTrait.CONF_PUSH, true).get();
        pairing.setProperty(AutomationPairingTrait.CONF_PULL, true).get();

        tick(10);

        assertTrue(pairing.fetchProperty(EnabledDisabledTrait.STAT_VALUE).get());
        assertTrue(pairing.fetchProperty(AutomationPairingTrait.CONF_PUSH).get());
        assertTrue(pairing.fetchProperty(AutomationPairingTrait.CONF_PULL).get());

        int initialCount = pairing.fetchProperty(AutomationPairingTrait.STAT_COUNT).get();

        bulb1.setProperty(OnOffTrait.STAT_VALUE, true).get();

        tick(10);

        assertTrue(bulb1.fetchProperty(OnOffTrait.STAT_VALUE).get());
        assertTrue(bulb2.fetchProperty(OnOffTrait.STAT_VALUE).get());
        assertEquals(initialCount+1, (int)pairing.fetchProperty(AutomationPairingTrait.STAT_COUNT).get());

        bulb1.setProperty(OnOffTrait.STAT_VALUE, false).get();

        tick(10);

        assertFalse(bulb1.fetchProperty(OnOffTrait.STAT_VALUE).get());
        assertFalse(bulb2.fetchProperty(OnOffTrait.STAT_VALUE).get());
        assertEquals(initialCount+2, (int)pairing.fetchProperty(AutomationPairingTrait.STAT_COUNT).get());

        bulb2.setProperty(OnOffTrait.STAT_VALUE, true).get();

        tick(10);

        assertTrue(bulb1.fetchProperty(OnOffTrait.STAT_VALUE).get());
        assertTrue(bulb2.fetchProperty(OnOffTrait.STAT_VALUE).get());
        assertEquals(initialCount+3, (int)pairing.fetchProperty(AutomationPairingTrait.STAT_COUNT).get());

        bulb2.setProperty(OnOffTrait.STAT_VALUE, false).get();

        tick(10);

        assertFalse(bulb1.fetchProperty(OnOffTrait.STAT_VALUE).get());
        assertFalse(bulb2.fetchProperty(OnOffTrait.STAT_VALUE).get());
        assertEquals(initialCount+4, (int)pairing.fetchProperty(AutomationPairingTrait.STAT_COUNT).get());
    }


    @Test
    public void simpleEnabledDisabledTest() throws Exception {
        setupOnOffPairing();

        assertTrue(pairing.fetchProperty(EnabledDisabledTrait.STAT_VALUE).get());
        assertTrue(pairing.fetchProperty(AutomationPairingTrait.CONF_PUSH).get());
        assertFalse(pairing.fetchProperty(AutomationPairingTrait.CONF_PULL).get());

        int initialCount = pairing.fetchProperty(AutomationPairingTrait.STAT_COUNT).get();

        bulb1.setProperty(OnOffTrait.STAT_VALUE, true).get();

        tick(10);

        assertTrue(bulb1.fetchProperty(OnOffTrait.STAT_VALUE).get());
        assertTrue(bulb2.fetchProperty(OnOffTrait.STAT_VALUE).get());
        assertEquals(initialCount+1, (int)pairing.fetchProperty(AutomationPairingTrait.STAT_COUNT).get());

        pairing.setProperty(EnabledDisabledTrait.STAT_VALUE, false).get();

        bulb1.setProperty(OnOffTrait.STAT_VALUE, false).get();

        tick(10);

        assertFalse(bulb1.fetchProperty(OnOffTrait.STAT_VALUE).get());
        assertTrue(bulb2.fetchProperty(OnOffTrait.STAT_VALUE).get());
        assertEquals(initialCount+1, (int)pairing.fetchProperty(AutomationPairingTrait.STAT_COUNT).get());

        bulb1.setProperty(OnOffTrait.STAT_VALUE, true).get();
        pairing.setProperty(EnabledDisabledTrait.STAT_VALUE, true).get();
        bulb1.setProperty(OnOffTrait.STAT_VALUE, false).get();

        tick(10);

        assertFalse(bulb1.fetchProperty(OnOffTrait.STAT_VALUE).get());
        assertFalse(bulb2.fetchProperty(OnOffTrait.STAT_VALUE).get());
        assertEquals(initialCount+3, (int)pairing.fetchProperty(AutomationPairingTrait.STAT_COUNT).get());
    }
}