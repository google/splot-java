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

import com.google.iot.coap.Coap;
import com.google.iot.coap.LocalEndpoint;
import com.google.iot.m2m.base.*;
import com.google.iot.m2m.trait.*;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;

@SuppressWarnings("ConstantConditions")
class SmcpDiscoveryQueryTest extends SmcpTestBase {
    private static final boolean DEBUG = false;
    private static final Logger LOGGER =
            Logger.getLogger(SmcpDiscoveryQueryTest.class.getCanonicalName());

    @Test
    void normalDiscoveryLoopTest() throws Exception {
        try {
            SmcpTechnology techHosting = new SmcpTechnology(mContextA);
            SmcpTechnology techBacking = new SmcpTechnology(mContextA);

            MyLightBulb localFe = new MyLightBulb();

            techHosting
                    .getServer()
                    .addLocalEndpoint(
                            techHosting
                                    .getLocalEndpointManager()
                                    .getLocalEndpointForScheme("loop"));
            techHosting.host(localFe);

            techHosting.getServer().start();

            DiscoveryQuery query =
                    techBacking
                            .createDiscoveryQueryBuilder()
                            .setLocalEndpoint(
                                    techBacking
                                            .getLocalEndpointManager()
                                            .getLocalEndpointForScheme("loop"))
                            .setTimeout(100, TimeUnit.MILLISECONDS)
                            .buildAndRun();

            tick(1);

            Set<FunctionalEndpoint> results = query.get();

            assertFalse(results.isEmpty());
            assertEquals(1, results.size());

            FunctionalEndpoint remoteFe = results.iterator().next();

            assertEquals(
                    localFe.getCachedProperty(BaseTrait.META_UID),
                    remoteFe.getCachedProperty(BaseTrait.META_UID));
        } catch (Throwable x) {
            dumpLogs();
            throw x;
        }
    }

    @Test
    void normalDiscoveryCoapUdpTest() throws Exception {
        try {
            SmcpTechnology techHosting = new SmcpTechnology(mContextB);
            SmcpTechnology techBacking = new SmcpTechnology(mContextA);

            MyLightBulb localFe = new MyLightBulb();

            LocalEndpoint hostingEndpoint = mContextA.getLocalEndpointForScheme(Coap.SCHEME_UDP);

            techHosting.getServer().addLocalEndpoint(hostingEndpoint);
            techHosting.prepareToHost();
            techHosting.host(localFe);

            techHosting.getServer().start();

            int port = 0;
            SocketAddress hostingSockaddr = hostingEndpoint.getLocalSocketAddress();
            if (hostingSockaddr instanceof InetSocketAddress) {
                port = ((InetSocketAddress)hostingSockaddr).getPort();
            }

            DiscoveryQuery query =
                    techBacking
                            .createDiscoveryQueryBuilder()
                            .setTimeout(100, TimeUnit.MILLISECONDS)
                            .setBaseUri(URI.create("coap://" + Coap.ALL_NODES_MCAST_IP4 + ":" + port + "/"))
                            .buildAndRun();

            Set<FunctionalEndpoint> results = query.get();

            assertFalse(results.isEmpty());
            assertEquals(1, results.size());

            FunctionalEndpoint remoteFe = results.iterator().next();

            assertEquals(
                    localFe.getCachedProperty(BaseTrait.META_UID),
                    remoteFe.getCachedProperty(BaseTrait.META_UID));
        } catch (Throwable x) {
            dumpLogs();
            throw x;
        }
    }
}
