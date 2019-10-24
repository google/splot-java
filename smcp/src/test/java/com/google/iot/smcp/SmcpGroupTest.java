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
package com.google.iot.smcp;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.google.iot.coap.*;
import com.google.iot.m2m.base.*;
import com.google.iot.m2m.local.LocalThing;
import com.google.iot.m2m.trait.*;
import com.google.iot.m2m.util.NestedPersistentStateManager;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;

@SuppressWarnings("ConstantConditions")
class SmcpGroupTest extends SmcpTestBase {
    private static final boolean DEBUG = false;
    private static final Logger LOGGER = Logger.getLogger(SmcpGroupTest.class.getCanonicalName());

    @Test
    public void groupPersistenceTest() throws Exception {
        String livingRoomGroupId;
        Map<String, Object> persistentState;

        {
            NestedPersistentStateManager psm = new NestedPersistentStateManager();

            SmcpTechnology technology = new SmcpTechnology(mContextA);
            LocalThing bulb1 = new MyLightBulbNoTrans();
            LocalThing bulb2 = new MyLightBulbNoTrans();
            LocalThing bulb3 = new MyLightBulbNoTrans();

            psm.startManaging("technology", technology);
            psm.startManaging("bulbHosted", bulb1);
            psm.startManaging("bulbLocal", bulb2);
            psm.startManaging("bulb3", bulb3);

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

            SmcpTechnology technology = new SmcpTechnology(mContextA);
            LocalThing bulb1 = new MyLightBulbNoTrans();
            LocalThing bulb2 = new MyLightBulbNoTrans();
            LocalThing bulb3 = new MyLightBulbNoTrans();

            psm.startManaging("technology", technology);
            psm.startManaging("bulbHosted", bulb1);
            psm.startManaging("bulbLocal", bulb2);
            psm.startManaging("bulb3", bulb3);

            technology.host(bulb1);
            technology.host(bulb2);
            technology.host(bulb3);

            Group livingRoom = technology.fetchOrCreateGroupWithId(livingRoomGroupId).get();

            assertNotNull(livingRoom);

            livingRoom.setProperty(OnOffTrait.STAT_VALUE, true).get(10, TimeUnit.MILLISECONDS);

            assertTrue(bulb1.fetchProperty(OnOffTrait.STAT_VALUE).get());
            assertTrue(bulb2.fetchProperty(OnOffTrait.STAT_VALUE).get());
            assertFalse(bulb3.fetchProperty(OnOffTrait.STAT_VALUE).get());

            livingRoom.setProperty(OnOffTrait.STAT_VALUE, false).get(10, TimeUnit.MILLISECONDS);

            assertFalse(bulb1.fetchProperty(OnOffTrait.STAT_VALUE).get());
            assertFalse(bulb2.fetchProperty(OnOffTrait.STAT_VALUE).get());
            assertFalse(bulb3.fetchProperty(OnOffTrait.STAT_VALUE).get());

            livingRoom.toggleProperty(OnOffTrait.STAT_VALUE).get(10, TimeUnit.MILLISECONDS);

            assertTrue(bulb1.fetchProperty(OnOffTrait.STAT_VALUE).get());
            assertTrue(bulb2.fetchProperty(OnOffTrait.STAT_VALUE).get());
            assertFalse(bulb3.fetchProperty(OnOffTrait.STAT_VALUE).get());

            bulb1.toggleProperty(OnOffTrait.STAT_VALUE).get();

            assertFalse(bulb1.fetchProperty(OnOffTrait.STAT_VALUE).get());
            assertTrue(bulb2.fetchProperty(OnOffTrait.STAT_VALUE).get());
            assertFalse(bulb3.fetchProperty(OnOffTrait.STAT_VALUE).get());

            livingRoom.toggleProperty(OnOffTrait.STAT_VALUE).get(10, TimeUnit.MILLISECONDS);

            assertTrue(bulb1.fetchProperty(OnOffTrait.STAT_VALUE).get());
            assertFalse(bulb2.fetchProperty(OnOffTrait.STAT_VALUE).get());
            assertFalse(bulb3.fetchProperty(OnOffTrait.STAT_VALUE).get());
        }
    }
}
