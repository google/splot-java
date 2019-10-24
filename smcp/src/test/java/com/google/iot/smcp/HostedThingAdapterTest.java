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

import com.google.iot.coap.Client;
import com.google.iot.coap.Code;
import com.google.iot.coap.Message;
import com.google.iot.coap.Transaction;
import com.google.iot.m2m.base.*;
import com.google.iot.m2m.trait.*;
import java.net.URI;
import java.util.*;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;

@SuppressWarnings("ConstantConditions")
class HostedThingAdapterTest extends SmcpTestBase {
    private static final boolean DEBUG = false;
    private static final Logger LOGGER =
            Logger.getLogger(HostedThingAdapterTest.class.getCanonicalName());

    @Test
    void hostedAdapterCombinedTest() throws Exception {
        SmcpTechnology techHosting = new SmcpTechnology(mContextA);
        SmcpTechnology techBacking = new SmcpTechnology(mContextA);

        MyLightBulb localFe = new MyLightBulb();

        techHosting
                .getServer()
                .addLocalEndpoint(
                        techHosting.getLocalEndpointManager().getLocalEndpointForScheme("loop"));
        techHosting.host(localFe);

        techHosting.getServer().start();

        Thing remoteFe =
                techBacking.getThingForNativeUri(URI.create("loop://localhost/1/"));

        Client client =
                new Client(
                        techBacking.getLocalEndpointManager(), URI.create("loop://localhost/1/"));

        Transaction transaction =
                client.newRequestBuilder()
                        .changePath(SceneTrait.METHOD_SAVE.getName() + "&sid=test")
                        .setCode(Code.METHOD_POST)
                        .send();
        Message response = transaction.getResponse();

        if (DEBUG) LOGGER.info(response.toString());

        URI location = response.getOptionSet().getLocation();
        assertEquals(Code.RESPONSE_CREATED, response.getCode(), Code.toString(response.getCode()));
        assertEquals(
                "/1/" + Splot.SECTION_FUNC + "/" + SceneTrait.TRAIT_ID + "/test/",
                location.toASCIIString());

        // Verify that the created resource is listed
        transaction =
                client.newRequestBuilder()
                        .changePath(
                                "/1/" + Splot.SECTION_FUNC + "/" + SceneTrait.TRAIT_ID + "/")
                        .setCode(Code.METHOD_GET)
                        .send();
        response = transaction.getResponse();

        if (DEBUG) LOGGER.info(response.toString());

        assertEquals(Code.RESPONSE_CONTENT, response.getCode(), Code.toString(response.getCode()));
        // TODO: Actually verify that the created resource is listed

        // Verify that the created resource is accessible.
        transaction =
                client.newRequestBuilder()
                        .changePath(location.toASCIIString() + Splot.SECTION_STATE + "/")
                        .setCode(Code.METHOD_GET)
                        .send();
        response = transaction.getResponse();

        if (DEBUG) LOGGER.info(response.toString());

        assertEquals(Code.RESPONSE_CONTENT, response.getCode(), Code.toString(response.getCode()));
    }

    @Test
    void hostedAdapterMethodCallTest() throws Exception {
        SmcpTechnology techHosting = new SmcpTechnology(mContextA);
        SmcpTechnology techBacking = new SmcpTechnology(mContextA);

        MyLightBulb localFe = new MyLightBulb();

        techHosting
                .getServer()
                .addLocalEndpoint(
                        techHosting.getLocalEndpointManager().getLocalEndpointForScheme("loop"));
        techHosting.host(localFe);

        techHosting.getServer().start();

        Thing remoteFe =
                techBacking.getThingForNativeUri(URI.create("loop://localhost/1/"));

        Client client =
                new Client(
                        techBacking.getLocalEndpointManager(), URI.create("loop://localhost/1/"));

        Transaction transaction =
                client.newRequestBuilder()
                        .changePath(SceneTrait.METHOD_SAVE.getName() + "&sid=test")
                        .setCode(Code.METHOD_POST)
                        .send();
        Message response = transaction.getResponse();

        if (DEBUG) LOGGER.info(response.toString());

        URI location = response.getOptionSet().getLocation();
        assertEquals(Code.RESPONSE_CREATED, response.getCode(), Code.toString(response.getCode()));
        assertEquals(
                "/1/" + Splot.SECTION_FUNC + "/" + SceneTrait.TRAIT_ID + "/test/",
                location.toASCIIString());

        // Verify that the resource was indeed created
        Set<String> childIdSet = new HashSet<>();
        for (Thing child : localFe.fetchChildrenForTrait(SceneTrait.TRAIT_ID).get()) {
            childIdSet.add(localFe.getIdForChild(child));
        }
        assertTrue(childIdSet.contains("test"));
    }

    @Test
    void hostedAdapterChildTest() throws Exception {
        SmcpTechnology techHosting = new SmcpTechnology(mContextA);
        SmcpTechnology techBacking = new SmcpTechnology(mContextA);

        MyLightBulb localFe = new MyLightBulb();

        techHosting
                .getServer()
                .addLocalEndpoint(
                        techHosting.getLocalEndpointManager().getLocalEndpointForScheme("loop"));
        techHosting.host(localFe);

        techHosting.getServer().start();

        Thing remoteFe =
                techBacking.getThingForNativeUri(URI.create("loop://localhost/1/"));

        Client client =
                new Client(
                        techBacking.getLocalEndpointManager(), URI.create("loop://localhost/1/"));

        Thing testScene =
                remoteFe.invokeMethod(
                                SceneTrait.METHOD_SAVE,
                                SceneTrait.PARAM_SCENE_ID.with("hostedAdapterChildTest"))
                        .get();

        assertNotNull(testScene);

        // Verify that the created resource is listed
        Transaction transaction =
                client.newRequestBuilder()
                        .changePath(
                                "/1/" + Splot.SECTION_FUNC + "/" + SceneTrait.TRAIT_ID + "/")
                        .setCode(Code.METHOD_GET)
                        .send();
        Message response = transaction.getResponse();

        if (DEBUG) LOGGER.info(response.toString());

        assertEquals(Code.RESPONSE_CONTENT, response.getCode(), Code.toString(response.getCode()));
        // TODO: Actually verify that the created resource is listed

        // Verify that the created resource is accessible.
        transaction =
                client.newRequestBuilder()
                        .changePath(
                                "/1/"
                                        + Splot.SECTION_FUNC
                                        + "/"
                                        + SceneTrait.TRAIT_ID
                                        + "/hostedAdapterChildTest/"
                                        + Splot.SECTION_STATE
                                        + "/")
                        .setCode(Code.METHOD_GET)
                        .send();
        response = transaction.getResponse();

        if (DEBUG) LOGGER.info(response.toString());

        assertEquals(Code.RESPONSE_CONTENT, response.getCode(), Code.toString(response.getCode()));
    }
}
