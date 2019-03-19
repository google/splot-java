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
import org.mockito.Mock;

@SuppressWarnings("ConstantConditions")
class SmcpFunctionalEndpointTest extends SmcpTestBase {
    private static final boolean DEBUG = false;
    private static final Logger LOGGER =
            Logger.getLogger(SmcpFunctionalEndpointTest.class.getCanonicalName());

    @Mock ChildListener mChildListenerMock;

    @Test
    void fetchPropertyTestCoap() throws Exception {
        try {
            SmcpTechnology techHosting = new SmcpTechnology(mContextA);
            SmcpTechnology techBacking = new SmcpTechnology(mContextB);

            MyLightBulb localFe = new MyLightBulb();

            techHosting.prepareToHost();
            techHosting.host(localFe);

            LocalEndpoint localHostingEndpoint = techHosting.getLocalEndpointManager().getLocalEndpointForScheme(Coap.SCHEME_UDP);
            String host = "localhost";
            int port = -1;
            SocketAddress localSocketAddress = localHostingEndpoint.getLocalSocketAddress();
            if (localSocketAddress instanceof InetSocketAddress) {
                port = ((InetSocketAddress)localSocketAddress).getPort();
            }

            techHosting.getServer().addLocalEndpoint(localHostingEndpoint);
            techHosting.getServer().start();

            FunctionalEndpoint remoteFe = techBacking.getFunctionalEndpointForNativeUri(new URI(
                    Coap.SCHEME_UDP, null, host, port,
                    "/1/", null, null
            ));

            assertNotNull(remoteFe);

            for (int i = 0; i < 10; i++) {
                for (PropertyKey<?> key : localFe.fetchSupportedPropertyKeys().get()) {
                    assertEquals(
                            localFe.fetchProperty(key).get(500, TimeUnit.MILLISECONDS),
                            remoteFe.fetchProperty(key).get(500, TimeUnit.MILLISECONDS),
                            "for key " + key);
                }
            }
        } catch (Throwable x) {
            dumpLogs();
            throw x;
        }
    }

    @Test
    void fetchPropertyTestLoopback() throws Exception {
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

            FunctionalEndpoint remoteFe =
                    techBacking.getFunctionalEndpointForNativeUri(
                            URI.create("loop://localhost/1/"));

            assertNotNull(remoteFe);

            for (int i = 0; i < 10; i++) {
                for (PropertyKey<?> key : localFe.fetchSupportedPropertyKeys().get()) {
                    assertEquals(
                            localFe.fetchProperty(key).get(500, TimeUnit.MILLISECONDS),
                            remoteFe.fetchProperty(key).get(500, TimeUnit.MILLISECONDS),
                            "for key " + key);
                }
            }
        } catch (Throwable x) {
            dumpLogs();
            throw x;
        }
    }

    @Test
    void fetchSectionTest() throws Exception {
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

            FunctionalEndpoint remoteFe =
                    techBacking.getFunctionalEndpointForNativeUri(
                            URI.create("loop://localhost/1/"));

            assertEquals(
                    localFe.fetchState().get(500, TimeUnit.MILLISECONDS),
                    remoteFe.fetchState().get(500, TimeUnit.MILLISECONDS));
            assertEquals(
                    localFe.fetchMetadata().get(500, TimeUnit.MILLISECONDS),
                    remoteFe.fetchMetadata().get(500, TimeUnit.MILLISECONDS));
            assertEquals(
                    localFe.fetchConfig().get(500, TimeUnit.MILLISECONDS),
                    remoteFe.fetchConfig().get(500, TimeUnit.MILLISECONDS));

            assertEquals(
                    localFe.fetchState().get(500, TimeUnit.MILLISECONDS),
                    remoteFe.fetchState().get(500, TimeUnit.MILLISECONDS));
            assertEquals(
                    localFe.fetchMetadata().get(500, TimeUnit.MILLISECONDS),
                    remoteFe.fetchMetadata().get(500, TimeUnit.MILLISECONDS));
            assertEquals(
                    localFe.fetchConfig().get(500, TimeUnit.MILLISECONDS),
                    remoteFe.fetchConfig().get(500, TimeUnit.MILLISECONDS));

            assertEquals(
                    localFe.fetchState().get(500, TimeUnit.MILLISECONDS),
                    remoteFe.fetchState().get(500, TimeUnit.MILLISECONDS));
            assertEquals(
                    localFe.fetchMetadata().get(500, TimeUnit.MILLISECONDS),
                    remoteFe.fetchMetadata().get(500, TimeUnit.MILLISECONDS));
            assertEquals(
                    localFe.fetchConfig().get(500, TimeUnit.MILLISECONDS),
                    remoteFe.fetchConfig().get(500, TimeUnit.MILLISECONDS));
        } catch (Throwable x) {
            dumpLogs();
            throw x;
        }
    }

    @Test
    void setPropertyTest() throws Exception {
        SmcpTechnology techHosting = new SmcpTechnology(mContextA);
        SmcpTechnology techBacking = new SmcpTechnology(mContextA);

        MyLightBulb localFe = new MyLightBulb();

        techHosting
                .getServer()
                .addLocalEndpoint(
                        techHosting.getLocalEndpointManager().getLocalEndpointForScheme("loop"));
        techHosting.host(localFe);

        techHosting.getServer().start();

        FunctionalEndpoint remoteFe =
                techBacking.getFunctionalEndpointForNativeUri(URI.create("loop://localhost/1/"));

        assertEquals(
                false,
                remoteFe.fetchProperty(OnOffTrait.STAT_VALUE).get(500, TimeUnit.MILLISECONDS));
        assertEquals(
                0.0f,
                (float)
                        remoteFe.fetchProperty(LevelTrait.STAT_VALUE)
                                .get(500, TimeUnit.MILLISECONDS));

        remoteFe.setProperty(OnOffTrait.STAT_VALUE, true).get();

        tick(500);

        assertEquals(
                true,
                remoteFe.fetchProperty(OnOffTrait.STAT_VALUE).get(500, TimeUnit.MILLISECONDS));

        remoteFe.setProperty(LevelTrait.STAT_VALUE, 1.0f).get();

        tick(500);

        assertEquals(
                (Float) 1.0f,
                remoteFe.fetchProperty(LevelTrait.STAT_VALUE).get(500, TimeUnit.MILLISECONDS));

        remoteFe.setProperty(LevelTrait.STAT_VALUE, 0.0f).get();
        remoteFe.setProperty(OnOffTrait.STAT_VALUE, false).get();

        tick(500);

        assertEquals(
                false,
                remoteFe.fetchProperty(OnOffTrait.STAT_VALUE).get(500, TimeUnit.MILLISECONDS));
        assertEquals(
                (Float) 0.0f,
                remoteFe.fetchProperty(LevelTrait.STAT_VALUE).get(500, TimeUnit.MILLISECONDS));
    }

    @Test
    void setPropertiesTest() throws Exception {
        SmcpTechnology techHosting = new SmcpTechnology(mContextA);
        SmcpTechnology techBacking = new SmcpTechnology(mContextA);

        MyLightBulb localFe = new MyLightBulb();

        techHosting
                .getServer()
                .addLocalEndpoint(
                        techHosting.getLocalEndpointManager().getLocalEndpointForScheme("loop"));
        techHosting.host(localFe);

        techHosting.getServer().start();

        FunctionalEndpoint remoteFe =
                techBacking.getFunctionalEndpointForNativeUri(URI.create("loop://localhost/1/"));

        Map<String, Object> targetState = new HashMap<>();

        OnOffTrait.STAT_VALUE.putInMap(targetState, true);
        LevelTrait.STAT_VALUE.putInMap(targetState, 1.0f);
        TransitionTrait.STAT_DURATION.putInMap(targetState, 0.0f);

        remoteFe.applyProperties(targetState).get();

        remoteFe.setProperty(OnOffTrait.STAT_VALUE, true).get();

        assertEquals(
                true,
                remoteFe.fetchProperty(OnOffTrait.STAT_VALUE).get(500, TimeUnit.MILLISECONDS));
        assertEquals(
                1.0f,
                (float)
                        remoteFe.fetchProperty(LevelTrait.STAT_VALUE)
                                .get(500, TimeUnit.MILLISECONDS));
    }

    @Test
    void childFetchTest() throws Exception {
        try {
            SmcpTechnology techHosting = new SmcpTechnology(mContextA);
            SmcpTechnology techBacking = new SmcpTechnology(mContextA);

            MyLightBulbNoTrans localFe = new MyLightBulbNoTrans();

            techHosting
                    .getServer()
                    .addLocalEndpoint(
                            techHosting
                                    .getLocalEndpointManager()
                                    .getLocalEndpointForScheme("loop"));
            techHosting.host(localFe);

            techHosting.getServer().start();

            FunctionalEndpoint testScene =
                    localFe.invokeMethod(
                                    SceneTrait.METHOD_SAVE,
                                    SceneTrait.PARAM_SCENE_ID,
                                    "childFetchTest")
                            .get();
            assertNotNull(testScene);

            tick(10);

            FunctionalEndpoint remoteFe =
                    techBacking.getFunctionalEndpointForNativeUri(
                            URI.create("loop://localhost/1/"));

            assertNotNull(remoteFe);

            Set<String> childIdSet = new HashSet<>();
            Collection<FunctionalEndpoint> children =
                    remoteFe.fetchChildrenForTrait(SceneTrait.TRAIT_ID).get();

            assertNotNull(children);

            assertNotEquals(0, children.size());

            for (FunctionalEndpoint child : children) {
                String childId = remoteFe.getIdForChild(child);
                if (DEBUG) {
                    LOGGER.info(child.toString() + " -> " + childId);
                }
                childIdSet.add(childId);
            }

            assertTrue(childIdSet.contains("childFetchTest"), childIdSet.toString());
        } catch (Throwable x) {
            dumpLogs();
            throw x;
        }
    }

    @Test
    void childAddedRemovedTest() throws Exception {
        try {
            URI uri = URI.create("loop://localhost/1/");
            SmcpTechnology techHosting = new SmcpTechnology(mContextA);
            SmcpTechnology techBacking = new SmcpTechnology(mContextA);

            MyLightBulbNoTrans localFe = new MyLightBulbNoTrans();

            techHosting
                    .getServer()
                    .addLocalEndpoint(
                            techHosting
                                    .getLocalEndpointManager()
                                    .getLocalEndpointForScheme("loop"));
            techHosting.host(localFe);

            techHosting.getServer().start();

            FunctionalEndpoint remoteFe = techBacking.getFunctionalEndpointForNativeUri(uri);

            assertNotNull(remoteFe);

            Collection<FunctionalEndpoint> children;

            remoteFe.registerChildListener(mExecutor, SceneTrait.TRAIT_ID, mChildListenerMock);

            children = remoteFe.fetchChildrenForTrait(SceneTrait.TRAIT_ID).get();
            assertNotNull(children);
            assertEquals(0, children.size());

            verify(mChildListenerMock, never()).onChildAdded(any(), any(), any());
            verify(mChildListenerMock, never()).onChildRemoved(any(), any(), any());

            FunctionalEndpoint testScene1 =
                    localFe.invokeMethod(
                                    SceneTrait.METHOD_SAVE,
                                    SceneTrait.PARAM_SCENE_ID,
                                    "childAddedTest-1")
                            .get();
            assertNotNull(testScene1);

            verify(mChildListenerMock, timeout(200).only())
                    .onChildAdded(eq(remoteFe), eq(SceneTrait.TRAIT_ID), any());

            children = remoteFe.fetchChildrenForTrait(SceneTrait.TRAIT_ID).get();
            assertNotNull(children);
            assertEquals(1, children.size());

            verify(mChildListenerMock, timeout(200).only())
                    .onChildAdded(
                            eq(remoteFe), eq(SceneTrait.TRAIT_ID), eq(children.iterator().next()));

            clearInvocations(mChildListenerMock);

            verify(mChildListenerMock, timeout(100).times(0)).onChildAdded(any(), any(), any());
            verify(mChildListenerMock, never()).onChildRemoved(any(), any(), any());

            FunctionalEndpoint testScene2 =
                    remoteFe.invokeMethod(
                                    SceneTrait.METHOD_SAVE,
                                    SceneTrait.PARAM_SCENE_ID,
                                    "childAddedTest-2")
                            .get();
            assertNotNull(testScene2);

            verify(mChildListenerMock, timeout(200).only())
                    .onChildAdded(eq(remoteFe), eq(SceneTrait.TRAIT_ID), any());

            children = remoteFe.fetchChildrenForTrait(SceneTrait.TRAIT_ID).get();
            assertNotNull(children);
            assertEquals(2, children.size());

            clearInvocations(mChildListenerMock);

            assertTrue(testScene1.delete().get());

            verify(mChildListenerMock, timeout(100).only())
                    .onChildRemoved(eq(remoteFe), eq(SceneTrait.TRAIT_ID), any());

            clearInvocations(mChildListenerMock);

            assertTrue(testScene2.delete().get());

            verify(mChildListenerMock, timeout(100).only())
                    .onChildRemoved(eq(remoteFe), eq(SceneTrait.TRAIT_ID), any());
        } catch (Throwable x) {
            dumpLogs();
            throw x;
        }
    }

    @Test
    void sceneEndToEndTest() throws Exception {
        SmcpTechnology techHosting = new SmcpTechnology(mContextA);
        SmcpTechnology techBacking = new SmcpTechnology(mContextA);

        MyLightBulbNoTrans localFe = new MyLightBulbNoTrans();

        techHosting
                .getServer()
                .addLocalEndpoint(
                        techHosting.getLocalEndpointManager().getLocalEndpointForScheme("loop"));
        techHosting.host(localFe);

        techHosting.getServer().start();

        FunctionalEndpoint remoteFe =
                techBacking.getFunctionalEndpointForNativeUri(URI.create("loop://localhost/1/"));

        assertNotNull(remoteFe);

        remoteFe.setProperty(LevelTrait.STAT_VALUE, 0.0f).get();
        remoteFe.setProperty(OnOffTrait.STAT_VALUE, false).get();
        FunctionalEndpoint offScene =
                remoteFe.invokeMethod(SceneTrait.METHOD_SAVE, SceneTrait.PARAM_SCENE_ID, "off")
                        .get();
        assertNotNull(offScene);

        remoteFe.setProperty(LevelTrait.STAT_VALUE, 1.0f).get();
        remoteFe.setProperty(OnOffTrait.STAT_VALUE, true).get();
        FunctionalEndpoint onScene =
                remoteFe.invokeMethod(SceneTrait.METHOD_SAVE, SceneTrait.PARAM_SCENE_ID, "on")
                        .get();
        assertNotNull(onScene);

        remoteFe.setProperty(LevelTrait.STAT_VALUE, 0.25f).get();
        FunctionalEndpoint dimScene =
                remoteFe.invokeMethod(SceneTrait.METHOD_SAVE, SceneTrait.PARAM_SCENE_ID, "dim")
                        .get();
        assertNotNull(dimScene);

        assertEquals(true, remoteFe.fetchProperty(OnOffTrait.STAT_VALUE).get());
        assertEquals((Float) 0.25f, remoteFe.fetchProperty(LevelTrait.STAT_VALUE).get());

        remoteFe.setProperty(SceneTrait.STAT_SCENE_ID, "on").get();
        assertEquals(true, remoteFe.fetchProperty(OnOffTrait.STAT_VALUE).get());
        assertEquals((Float) 1.0f, remoteFe.fetchProperty(LevelTrait.STAT_VALUE).get());
        assertEquals("on", remoteFe.fetchProperty(SceneTrait.STAT_SCENE_ID).get());

        remoteFe.setProperty(SceneTrait.STAT_SCENE_ID, "off").get();
        assertEquals(false, remoteFe.fetchProperty(OnOffTrait.STAT_VALUE).get());
        assertEquals("off", remoteFe.fetchProperty(SceneTrait.STAT_SCENE_ID).get());

        remoteFe.setProperty(SceneTrait.STAT_SCENE_ID, "dim").get();
        assertEquals(true, remoteFe.fetchProperty(OnOffTrait.STAT_VALUE).get());
        assertEquals((Float) 0.25f, remoteFe.fetchProperty(LevelTrait.STAT_VALUE).get());
        assertEquals("dim", remoteFe.fetchProperty(SceneTrait.STAT_SCENE_ID).get());

        dimScene.setProperty(LevelTrait.STAT_VALUE, 0.5f).get();
        assertEquals((Float) 0.25f, remoteFe.fetchProperty(LevelTrait.STAT_VALUE).get());
        remoteFe.setProperty(SceneTrait.STAT_SCENE_ID, "dim").get();
        assertEquals((Float) 0.5f, remoteFe.fetchProperty(LevelTrait.STAT_VALUE).get());

        assertEquals("dim", remoteFe.fetchProperty(SceneTrait.STAT_SCENE_ID).get());
        remoteFe.setProperty(LevelTrait.STAT_VALUE, 0.25f).get();
        assertNotEquals("dim", remoteFe.fetchProperty(SceneTrait.STAT_SCENE_ID).get());

        assertEquals(true, offScene.delete().get());
        remoteFe.setProperty(SceneTrait.STAT_SCENE_ID, "off").get();
        assertNotEquals(false, remoteFe.fetchProperty(OnOffTrait.STAT_VALUE).get());
    }

    @Test
    void togglePropertyTest() throws Exception {
        SmcpTechnology techHosting = new SmcpTechnology(mContextA);
        SmcpTechnology techBacking = new SmcpTechnology(mContextA);

        MyLightBulb localFe = new MyLightBulb();

        techHosting
                .getServer()
                .addLocalEndpoint(
                        techHosting.getLocalEndpointManager().getLocalEndpointForScheme("loop"));
        techHosting.host(localFe);

        techHosting.getServer().start();

        FunctionalEndpoint remoteFe =
                techBacking.getFunctionalEndpointForNativeUri(URI.create("loop://localhost/1/"));

        assertNotNull(remoteFe);

        assertEquals(
                false,
                remoteFe.fetchProperty(OnOffTrait.STAT_VALUE).get(500, TimeUnit.MILLISECONDS));

        remoteFe.toggleProperty(OnOffTrait.STAT_VALUE).get();

        assertEquals(
                true,
                remoteFe.fetchProperty(OnOffTrait.STAT_VALUE).get(500, TimeUnit.MILLISECONDS));

        remoteFe.toggleProperty(OnOffTrait.STAT_VALUE).get();

        tick(500);

        assertEquals(
                false,
                remoteFe.fetchProperty(OnOffTrait.STAT_VALUE).get(500, TimeUnit.MILLISECONDS));
    }

    @Test
    void incrementPropertyTest() throws Exception {
        SmcpTechnology techHosting = new SmcpTechnology(mContextA);
        SmcpTechnology techBacking = new SmcpTechnology(mContextA);

        MyLightBulb localFe = new MyLightBulb();

        techHosting
                .getServer()
                .addLocalEndpoint(
                        techHosting.getLocalEndpointManager().getLocalEndpointForScheme("loop"));
        techHosting.host(localFe);

        techHosting.getServer().start();

        FunctionalEndpoint remoteFe =
                techBacking.getFunctionalEndpointForNativeUri(URI.create("loop://localhost/1/"));

        assertNotNull(remoteFe);

        assertEquals(
                (Float) 0.0f,
                remoteFe.fetchProperty(LevelTrait.STAT_VALUE).get(500, TimeUnit.MILLISECONDS));

        remoteFe.setProperty(OnOffTrait.STAT_VALUE, true).get();
        remoteFe.incrementProperty(LevelTrait.STAT_VALUE, 0.1f).get();

        assertNotEquals(
                0.1f,
                remoteFe.fetchProperty(LevelTrait.STAT_VALUE).get(500, TimeUnit.MILLISECONDS));
        tick(500);
        assertEquals(
                (Float) 0.1f,
                remoteFe.fetchProperty(LevelTrait.STAT_VALUE).get(500, TimeUnit.MILLISECONDS));

        remoteFe.incrementProperty(LevelTrait.STAT_VALUE, -0.1f).get();

        assertNotEquals(
                0.0f,
                remoteFe.fetchProperty(LevelTrait.STAT_VALUE).get(500, TimeUnit.MILLISECONDS));
        tick(500);
        assertEquals(
                (Float) 0.0f,
                remoteFe.fetchProperty(LevelTrait.STAT_VALUE).get(500, TimeUnit.MILLISECONDS));
    }

    @Test
    @SuppressWarnings("unchecked")
    void observeRemotePropertyTest() throws Exception {
        SmcpTechnology techHosting = new SmcpTechnology(mContextA);
        SmcpTechnology techBacking = new SmcpTechnology(mContextA);

        MyLightBulb localFe = new MyLightBulb();

        techHosting
                .getServer()
                .addLocalEndpoint(
                        techHosting.getLocalEndpointManager().getLocalEndpointForScheme("loop"));
        techHosting.host(localFe);

        techHosting.getServer().start();

        FunctionalEndpoint remoteFe =
                techBacking.getFunctionalEndpointForNativeUri(URI.create("loop://localhost/1/"));

        assertNotNull(remoteFe);

        assertEquals(
                "Light Bulb",
                remoteFe.fetchProperty(BaseTrait.META_NAME).get(500, TimeUnit.MILLISECONDS));

        PropertyListener<String> propertyListener = mock(PropertyListener.class);

        remoteFe.registerPropertyListener(mExecutor, BaseTrait.META_NAME, propertyListener);

        // Multiple invocations with the same listener shouldn't cause problems.
        remoteFe.registerPropertyListener(mExecutor, BaseTrait.META_NAME, propertyListener);

        tick(10);

        // Sometimes we might get an initial copy of the property value. Ignore it for this test.
        clearInvocations(propertyListener);

        localFe.setProperty(BaseTrait.META_NAME, "FooBar").get();

        tick(10);

        verify(propertyListener, times(1))
                .onPropertyChanged(remoteFe, BaseTrait.META_NAME, "FooBar");
        clearInvocations(propertyListener);

        // Make sure unregister works properly.
        remoteFe.unregisterPropertyListener(BaseTrait.META_NAME, propertyListener);
        tick(10);
        localFe.setProperty(BaseTrait.META_NAME, "BarFoo").get();
        tick(10);
        verify(propertyListener, never())
                .onPropertyChanged(remoteFe, BaseTrait.META_NAME, "BarFoo");
        tick(10);
    }

    @Test
    void observeRemoteStateTest() throws Exception {
        SmcpTechnology techHosting = new SmcpTechnology(mContextA);
        SmcpTechnology techBacking = new SmcpTechnology(mContextA);

        MyLightBulb localFe = new MyLightBulb();

        techHosting
                .getServer()
                .addLocalEndpoint(
                        techHosting.getLocalEndpointManager().getLocalEndpointForScheme("loop"));
        techHosting.host(localFe);

        techHosting.getServer().start();

        FunctionalEndpoint remoteFe =
                techBacking.getFunctionalEndpointForNativeUri(URI.create("loop://localhost/1/"));

        assertNotNull(remoteFe);

        StateListener stateListener = mock(StateListener.class);

        remoteFe.registerStateListener(mExecutor, stateListener);

        // Multiple invocations with the same listener shouldn't cause problems.
        remoteFe.registerStateListener(mExecutor, stateListener);

        tick(10);

        // Sometimes we might get an initial copy of the property value. Ignore it for this test.
        clearInvocations(stateListener);

        localFe.setProperty(OnOffTrait.STAT_VALUE, true).get();

        tick(500);

        verify(stateListener, times(1)).onStateChanged(remoteFe, localFe.copyCachedState());
        clearInvocations(stateListener);

        remoteFe.unregisterStateListener(stateListener);

        tick(10);

        localFe.setProperty(OnOffTrait.STAT_VALUE, false).get();

        tick(500);

        verify(stateListener, never()).onStateChanged(remoteFe, localFe.copyCachedState());

        tick(10);
    }

    @Test
    void observeRemoteMetadataTest() throws Exception {
        SmcpTechnology techHosting = new SmcpTechnology(mContextA);
        SmcpTechnology techBacking = new SmcpTechnology(mContextA);

        MyLightBulb localFe = new MyLightBulb();

        techHosting
                .getServer()
                .addLocalEndpoint(
                        techHosting.getLocalEndpointManager().getLocalEndpointForScheme("loop"));
        techHosting.host(localFe);

        techHosting.getServer().start();

        FunctionalEndpoint remoteFe =
                techBacking.getFunctionalEndpointForNativeUri(URI.create("loop://localhost/1/"));

        assertNotNull(remoteFe);

        MetadataListener metadataListener = mock(MetadataListener.class);

        remoteFe.registerMetadataListener(mExecutor, metadataListener);

        // Multiple invocations with the same listener shouldn't cause problems.
        remoteFe.registerMetadataListener(mExecutor, metadataListener);

        tick(10);

        // Sometimes we might get an initial copy of the property value. Ignore it for this test.
        clearInvocations(metadataListener);

        localFe.setProperty(BaseTrait.META_NAME, "FooBar").get(10, TimeUnit.MILLISECONDS);

        tick(10);

        verify(metadataListener, times(1))
                .onMetadataChanged(remoteFe, localFe.copyCachedMetadata());
        clearInvocations(metadataListener);

        remoteFe.unregisterMetadataListener(metadataListener);

        tick(10);

        localFe.setProperty(BaseTrait.META_NAME, "BarFoo").get(10, TimeUnit.MILLISECONDS);

        tick(10);

        verify(metadataListener, never()).onMetadataChanged(remoteFe, localFe.copyCachedMetadata());

        tick(10);
    }

    @Test
    void observeRemoteConfigTest() throws Exception {
        SmcpTechnology techHosting = new SmcpTechnology(mContextA);
        SmcpTechnology techBacking = new SmcpTechnology(mContextA);

        MyLightBulb localFe = new MyLightBulb();

        techHosting
                .getServer()
                .addLocalEndpoint(
                        techHosting.getLocalEndpointManager().getLocalEndpointForScheme("loop"));
        techHosting.host(localFe);

        techHosting.getServer().start();

        FunctionalEndpoint remoteFe =
                techBacking.getFunctionalEndpointForNativeUri(URI.create("loop://localhost/1/"));

        assertNotNull(remoteFe);

        ConfigListener configListener = mock(ConfigListener.class);

        remoteFe.registerConfigListener(mExecutor, configListener);

        // Multiple invocations with the same listener shouldn't cause problems.
        remoteFe.registerConfigListener(mExecutor, configListener);

        final PropertyKey<Float> propertyKey = OnOffTrait.CONF_DURATION_ON;

        tick(10);

        // Sometimes we might get an initial copy of the property value. Ignore it for this test.
        clearInvocations(configListener);

        localFe.setProperty(propertyKey, 1.0f).get(10, TimeUnit.MILLISECONDS);

        tick(10);

        verify(configListener, times(1)).onConfigChanged(remoteFe, localFe.copyCachedConfig());
        clearInvocations(configListener);

        remoteFe.unregisterConfigListener(configListener);

        tick(10);

        localFe.setProperty(propertyKey, 0.0f).get(10, TimeUnit.MILLISECONDS);

        tick(10);

        verify(configListener, never()).onConfigChanged(any(), any());

        tick(10);
    }
}
