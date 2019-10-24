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

import com.google.iot.coap.Coap;
import com.google.iot.coap.LocalEndpoint;
import com.google.iot.m2m.base.Thing;
import com.google.iot.m2m.base.Group;
import com.google.iot.m2m.local.LocalThing;
import com.google.iot.m2m.local.LocalPairing;
import com.google.iot.m2m.trait.AutomationPairingTrait;
import com.google.iot.m2m.trait.EnabledDisabledTrait;
import com.google.iot.m2m.trait.OnOffTrait;
import com.google.iot.m2m.util.NestedPersistentStateManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("ConstantConditions")
class SmcpPairingTest extends SmcpTestBase {
    private static final boolean DEBUG = false;
    private static final Logger LOGGER = Logger.getLogger(SmcpPairingTest.class.getCanonicalName());

    SmcpTechnology techHosting = null;
    SmcpTechnology techBacking = null;
    Thing bulbHosted = null;
    Thing bulbLocal = null;
    LocalPairing pairing = null;

    Thing bulbRemote = null;

    @BeforeEach
    public void before() throws Exception {
        super.before();

        techBacking = new SmcpTechnology(mContextA);
        bulbLocal = new MyLightBulbNoTrans();
        pairing = new LocalPairing(techBacking) {
            @Override
            protected Executor getExecutor() {
                return mExecutor;
            }
        };
        techBacking.host(pairing);
        techBacking.host(bulbLocal);

        techHosting = new SmcpTechnology(mContextB);
        bulbHosted = new MyLightBulbNoTrans();

        final String scheme = Coap.SCHEME_UDP;
        LocalEndpoint localHostingEndpoint = techHosting.getLocalEndpointManager().getLocalEndpointForScheme(scheme);
        String host = "localhost";
        int port = -1;
        SocketAddress localSocketAddress = localHostingEndpoint.getLocalSocketAddress();
        if (localSocketAddress instanceof InetSocketAddress) {
            port = ((InetSocketAddress)localSocketAddress).getPort();
        }

        techHosting.getServer().addLocalEndpoint(localHostingEndpoint);
        techHosting.prepareToHost();
        techHosting.host(bulbHosted);

        techHosting.getServer().start();

        bulbRemote = techBacking.getThingForNativeUri(new URI(
                scheme, null, host, port,
                techHosting.getNativeUriForThing(bulbHosted).getPath(), null, null
        ));

        if (DEBUG) {
            pairing.registerPropertyListener(
                    Runnable::run,
                    AutomationPairingTrait.STAT_COUNT,
                    (fe, property, value) -> {
                        LOGGER.info(
                                "Pairing Property changed! Key: " + property + " Value: " + value);
                    });
        }

        assertFalse(bulbHosted.fetchProperty(OnOffTrait.STAT_VALUE).get());
        assertFalse(bulbLocal.fetchProperty(OnOffTrait.STAT_VALUE).get());

        if (DEBUG) {
            LOGGER.info("techHosting.getServer().getLocalEndpoints() = " + techHosting.getServer().getLocalEndpoints());
            LOGGER.info("techBacking.getServer().getLocalEndpoints() = " + techBacking.getServer().getLocalEndpoints());

            LOGGER.info("techHosting.getLocalEndpointManager().getLocalEndpointForScheme(Coap.SCHEME_UDP) = " +
                    techHosting.getLocalEndpointManager().getLocalEndpointForScheme(Coap.SCHEME_UDP));
            LOGGER.info("techBacking.getLocalEndpointManager().getLocalEndpointForScheme(Coap.SCHEME_UDP) = " +
                    techBacking.getLocalEndpointManager().getLocalEndpointForScheme(Coap.SCHEME_UDP));
        }
    }

    @AfterEach
    public void after() throws Exception {
        techHosting.unhost(bulbHosted);
        techHosting.getServer().close();
        techBacking.getServer().close();
        techBacking.unhost(pairing);
        techBacking.unhost(bulbLocal);
        techHosting = null;
        techBacking = null;
        bulbHosted = null;
        bulbLocal = null;
        bulbRemote = null;
        pairing = null;
        super.after();
    }

    void setupOnOffPairing() throws Exception {
        URI source = techBacking.getNativeUriForThing(bulbLocal);
        assertNotNull(source);
        source = source.resolve(OnOffTrait.STAT_VALUE.getName());

        URI destination = techBacking.getNativeUriForThing(bulbRemote);
        assertNotNull(destination);
        destination = destination.resolve(OnOffTrait.STAT_VALUE.getName());

        if (DEBUG) {
            LOGGER.info("source = " + source);
            LOGGER.info("destination = " + destination);
        }

        if (DEBUG) {
            bulbLocal.registerPropertyListener(
                    Runnable::run,
                    OnOffTrait.STAT_VALUE,
                    (fe, property, value) -> {
                        LOGGER.info(
                                "BulbLocal Property changed! Key: " + property + " Value: " + value);
                    });

            bulbHosted.registerPropertyListener(
                    Runnable::run,
                    OnOffTrait.STAT_VALUE,
                    (fe, property, value) -> {
                        LOGGER.info(
                                "BulbHosted Property changed! Key: " + property + " Value: " + value);
                    });
        }

        pairing.setProperty(AutomationPairingTrait.CONF_SOURCE, source).get();
        pairing.setProperty(AutomationPairingTrait.CONF_DESTINATION, destination).get();

        tick(10);
    }

    @Test
    public void pairingPushTest() throws Exception {
        setupOnOffPairing();

        assertTrue(pairing.fetchProperty(EnabledDisabledTrait.STAT_VALUE).get());
        assertTrue(pairing.fetchProperty(AutomationPairingTrait.CONF_PUSH).get());
        assertFalse(pairing.fetchProperty(AutomationPairingTrait.CONF_PULL).get());

        assertFalse(bulbRemote.fetchProperty(OnOffTrait.STAT_VALUE).get());

        int initialCount = pairing.fetchProperty(AutomationPairingTrait.STAT_COUNT).get();

        bulbLocal.setProperty(OnOffTrait.STAT_VALUE, true).get();

        tick(10);

        assertTrue(bulbLocal.fetchProperty(OnOffTrait.STAT_VALUE).get());
        assertTrue(bulbHosted.fetchProperty(OnOffTrait.STAT_VALUE).get());
        assertEquals(initialCount+1, (int)pairing.fetchProperty(AutomationPairingTrait.STAT_COUNT).get());

        bulbLocal.setProperty(OnOffTrait.STAT_VALUE, false).get();

        tick(10);

        assertFalse(bulbLocal.fetchProperty(OnOffTrait.STAT_VALUE).get());
        assertFalse(bulbHosted.fetchProperty(OnOffTrait.STAT_VALUE).get());
        assertEquals(initialCount+2, (int)pairing.fetchProperty(AutomationPairingTrait.STAT_COUNT).get());

        bulbRemote.setProperty(OnOffTrait.STAT_VALUE, true).get();

        tick(10);

        assertFalse(bulbLocal.fetchProperty(OnOffTrait.STAT_VALUE).get());
        assertTrue(bulbHosted.fetchProperty(OnOffTrait.STAT_VALUE).get());
        assertEquals(initialCount+2, (int)pairing.fetchProperty(AutomationPairingTrait.STAT_COUNT).get());
    }

    @Test
    public void pairingPullTest() throws Exception {
        setupOnOffPairing();

        pairing.setProperty(AutomationPairingTrait.CONF_PUSH, false).get();
        pairing.setProperty(AutomationPairingTrait.CONF_PULL, true).get();

        tick(100);

        assertTrue(pairing.fetchProperty(EnabledDisabledTrait.STAT_VALUE).get());
        assertFalse(pairing.fetchProperty(AutomationPairingTrait.CONF_PUSH).get());
        assertTrue(pairing.fetchProperty(AutomationPairingTrait.CONF_PULL).get());
        assertFalse(bulbRemote.fetchProperty(OnOffTrait.STAT_VALUE).get());

        tick(100);

        int initialCount = pairing.fetchProperty(AutomationPairingTrait.STAT_COUNT).get();

        bulbRemote.setProperty(OnOffTrait.STAT_VALUE, true).get();

        tick(100);

        assertTrue(bulbLocal.fetchProperty(OnOffTrait.STAT_VALUE).get());
        assertTrue(bulbHosted.fetchProperty(OnOffTrait.STAT_VALUE).get());
        assertEquals(initialCount+1, (int)pairing.fetchProperty(AutomationPairingTrait.STAT_COUNT).get());

        bulbRemote.setProperty(OnOffTrait.STAT_VALUE, false).get();

        tick(100);

        assertFalse(bulbLocal.fetchProperty(OnOffTrait.STAT_VALUE).get());
        assertFalse(bulbHosted.fetchProperty(OnOffTrait.STAT_VALUE).get());
        assertEquals(initialCount+2, (int)pairing.fetchProperty(AutomationPairingTrait.STAT_COUNT).get());

        bulbLocal.setProperty(OnOffTrait.STAT_VALUE, true).get();

        tick(100);

        assertTrue(bulbLocal.fetchProperty(OnOffTrait.STAT_VALUE).get());
        assertFalse(bulbHosted.fetchProperty(OnOffTrait.STAT_VALUE).get());
        assertEquals(initialCount+2, (int)pairing.fetchProperty(AutomationPairingTrait.STAT_COUNT).get());
    }
}
