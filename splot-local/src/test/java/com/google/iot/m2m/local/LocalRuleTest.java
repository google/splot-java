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
import com.google.iot.m2m.base.UnacceptableFunctionalEndpointException;
import com.google.iot.m2m.trait.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class LocalRuleTest extends TestBase {
    private static final boolean DEBUG = false;
    private static final Logger LOGGER =
            Logger.getLogger(LocalFunctionalEndpointTest.class.getCanonicalName());

    LocalTechnology technology = null;
    FunctionalEndpoint bulb1 = null;
    FunctionalEndpoint bulb2 = null;
    LocalRule rule = null;

    @BeforeEach
    @Override
    public void before() {
        super.before();

        technology = new LocalTechnology(mExecutor);

        bulb1 = new MyLightBulbNoTrans();
        bulb2 = new MyLightBulbNoTrans();
        rule = new LocalRule(technology);

        technology.prepareToHost();

        try {
            technology.host(bulb1);
            technology.host(bulb2);
            technology.host(rule);
        } catch (UnacceptableFunctionalEndpointException e) {
            throw new AssertionError(e);
        }

        if (DEBUG) {
            rule.registerPropertyListener(
                    Runnable::run,
                    ActionsTrait.STAT_COUNT,
                    (fe, property, value) -> {
                        LOGGER.info(
                                "Rule Property changed! Key: " + property + " Value: " + value);
                    });
            rule.registerPropertyListener(
                    Runnable::run,
                    BaseTrait.STAT_TRAP,
                    (fe, property, value) -> {
                        LOGGER.info(
                                "Rule Property changed! Key: " + property + " Value: " + value);
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
            rule.setProperty(EnabledDisabledTrait.STAT_VALUE, false).get();
            technology.unhost(rule);
            technology.unhost(bulb1);
            technology.unhost(bulb2);
            rule = null;
            bulb1 = null;
            bulb2 = null;
            technology = null;
        } catch (Exception e) {
            throw new AssertionError(e);
        }

        super.after();
    }

    @Test
    public void simpleConditionTest() throws Exception {

        assertEquals(0.0f, (float)bulb2.fetchProperty(LevelTrait.STAT_VALUE).get());

        Map<String,Object> action = new HashMap<>();

        ActionsTrait.PARAM_ACTION_PATH.putInMap(action,
                technology.getNativeUriForProperty(bulb2, LevelTrait.STAT_VALUE));
        ActionsTrait.PARAM_ACTION_BODY.putInMap(action, 0.2);

        rule.insertValueIntoProperty(ActionsTrait.CONF_ACTIONS, action).get();

        Map<String,Object> condition = new HashMap<>();

        AutomationRuleTrait.PARAM_COND_PATH.putInMap(condition,
                technology.getNativeUriForProperty(bulb1, LevelTrait.STAT_VALUE));
        AutomationRuleTrait.PARAM_COND_EXPR.putInMap(condition,
                "v 0.5 >");

        rule.insertValueIntoProperty(AutomationRuleTrait.CONF_CONDITIONS, condition).get();
        rule.setProperty(EnabledDisabledTrait.STAT_VALUE, true).get();

        tick(10);

        assertEquals(0.0f, (float)bulb2.fetchProperty(LevelTrait.STAT_VALUE).get());

        bulb1.setProperty(LevelTrait.STAT_VALUE, 0.2f).get();

        tick(10);

        assertEquals(0.0f, (float)bulb2.fetchProperty(LevelTrait.STAT_VALUE).get());

        bulb1.setProperty(LevelTrait.STAT_VALUE, 0.6f).get();

        tick(10);

        assertEquals(0.2f, (float)bulb2.fetchProperty(LevelTrait.STAT_VALUE).get());
    }

    @Test
    public void multipleConditionTest() throws Exception {

        assertEquals(0.0f, (float)bulb2.fetchProperty(LevelTrait.STAT_VALUE).get());

        Map<String,Object> action = new HashMap<>();

        ActionsTrait.PARAM_ACTION_PATH.putInMap(action,
                technology.getNativeUriForProperty(bulb2, LevelTrait.STAT_VALUE));
        ActionsTrait.PARAM_ACTION_BODY.putInMap(action, 0.2);

        rule.insertValueIntoProperty(ActionsTrait.CONF_ACTIONS, action).get();

        Map<String,Object> condition = new HashMap<>();
        AutomationRuleTrait.PARAM_COND_PATH.putInMap(condition,
                technology.getNativeUriForProperty(bulb1, LevelTrait.STAT_VALUE));
        AutomationRuleTrait.PARAM_COND_EXPR.putInMap(condition,
                "v 0.5 >");
        rule.insertValueIntoProperty(AutomationRuleTrait.CONF_CONDITIONS, condition).get();

        condition = new HashMap<>();
        AutomationRuleTrait.PARAM_COND_PATH.putInMap(condition,
                technology.getNativeUriForProperty(bulb1, OnOffTrait.STAT_VALUE));
        AutomationRuleTrait.PARAM_COND_EXPR.putInMap(condition,
                "! !");
        rule.insertValueIntoProperty(AutomationRuleTrait.CONF_CONDITIONS, condition).get();

        rule.setProperty(EnabledDisabledTrait.STAT_VALUE, true).get();

        tick(10);

        assertEquals(0.0f, (float)bulb2.fetchProperty(LevelTrait.STAT_VALUE).get());

        bulb1.setProperty(LevelTrait.STAT_VALUE, 0.2f).get();

        tick(10);

        assertEquals(0.0f, (float)bulb2.fetchProperty(LevelTrait.STAT_VALUE).get());

        bulb1.setProperty(LevelTrait.STAT_VALUE, 0.6f).get();

        tick(10);

        assertEquals(0.0f, (float)bulb2.fetchProperty(LevelTrait.STAT_VALUE).get());

        bulb1.setProperty(OnOffTrait.STAT_VALUE, true).get();

        tick(10);

        assertEquals(0.2f, (float)bulb2.fetchProperty(LevelTrait.STAT_VALUE).get());

        bulb2.setProperty(LevelTrait.STAT_VALUE, 0.0f).get();
        bulb1.setProperty(OnOffTrait.STAT_VALUE, false).get();

        tick(10);

        assertEquals(0.0f, (float)bulb2.fetchProperty(LevelTrait.STAT_VALUE).get());

        bulb1.setProperty(LevelTrait.STAT_VALUE, 0.0f).get();

        tick(10);

        assertEquals(0.0f, (float)bulb2.fetchProperty(LevelTrait.STAT_VALUE).get());

        bulb1.setProperty(OnOffTrait.STAT_VALUE, true).get();

        tick(10);

        assertEquals(0.0f, (float)bulb2.fetchProperty(LevelTrait.STAT_VALUE).get());

        bulb1.setProperty(LevelTrait.STAT_VALUE, 0.6f).get();

        tick(10);

        assertEquals(0.2f, (float)bulb2.fetchProperty(LevelTrait.STAT_VALUE).get());
    }
}