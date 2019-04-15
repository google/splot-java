/*
 * Copyright (C) 2019 Google Inc.
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
package com.example.smcp.server;

import com.google.iot.coap.LocalEndpointManager;
import com.google.iot.coap.LoggingInterceptorFactory;
import com.google.iot.m2m.base.CorruptPersistentStateException;
import com.google.iot.m2m.base.TechnologyException;
import com.google.iot.m2m.local.LocalAutomationManager;
import com.google.iot.m2m.util.FilePersistentStateManager;
import com.google.iot.smcp.SmcpTechnology;

import java.io.File;
import java.io.IOError;
import java.io.IOException;

/**
 * Example SMCP Server, hosting an automation manager, a light bulb, and system info.
 */
public class App
{
    private static final boolean LOG_PACKETS = true;

    // Local endpoint manager, for managing our CoAP endpoints.
    // We will only have one endpoint, but we need the manager
    // to pass to the SmcpTechnology constructor.
    private final LocalEndpointManager mLocalEndpointManager = new LocalEndpointManager();

    // Some settings and values we will want to persist across
    // startups, so we use a persistent state manager to handle
    // the saving of persistent data to disk. In this case, we
    // are saving it to a file named "demoState.cbor".
    private final File mStateFile = new File("example-smcp-server.cbor");
    private final FilePersistentStateManager mPersistentStateManager = FilePersistentStateManager.create(mStateFile);

    // A Technology instance is the object that is the interface
    // between the underlying transport and the Splot API. In this
    // case the underlying transport is SMCP, a CoAP-based M2M protocol.
    private final SmcpTechnology mTechnology = new SmcpTechnology(mLocalEndpointManager);

    // The automation manager is a functional endpoint that we will
    // register (host) with the technology. It provides automation
    // primitives that can be used to set up automation relationships
    // between functional endpoints, local or remote. Unlike most functional
    // endpoints, it needs a reference to the technology.
    private final LocalAutomationManager mAutomationManager = new LocalAutomationManager(mTechnology);

    // Functional endpoint which exposes things like load average and CPU count.
    private final SystemInfo mSystemInfo = new SystemInfo();

    // Demo light-bulb functional endpoint instance.
    private final MyLightBulb mLightBulb = new MyLightBulb();

    public App() throws IOException, CorruptPersistentStateException, TechnologyException {
        // If we want to log packets, this section adds an "interceptor"
        // that will log out packets to System.err. Turning this off will
        // improve performance.
        if (LOG_PACKETS) {
            LoggingInterceptorFactory mInterceptorFactory = new LoggingInterceptorFactory();
            mLocalEndpointManager.setDefaultInterceptor(mInterceptorFactory.create());
            mInterceptorFactory.setPrintStream(System.err);
        }

        mPersistentStateManager.startManaging("tech", mTechnology);

        mPersistentStateManager.startManaging("automationManager", mAutomationManager);
        mTechnology.host(mAutomationManager);

        mPersistentStateManager.startManaging("light", mLightBulb);
        mTechnology.host(mLightBulb);

        mPersistentStateManager.startManaging("sysinfo", mSystemInfo);
        mTechnology.host(mSystemInfo);
    }

    public void start() {
        try {
            // The next two lines prepare the technology for hosting on the
            // default ports and start the CoAP server instance allowing it
            // to handle incoming messages.
            mTechnology.prepareToHost();
            mTechnology.getServer().start();

        } catch (IOException x) {
            throw new IOError(x);
        }
    }

    public void stop() {
        synchronized (this) {
            notifyAll();
        }
    }

    public void waitForStop() throws InterruptedException {
        synchronized (this) {
            wait();
        }
    }

    public void close() {
        try {
            mPersistentStateManager.close();
            mTechnology.getServer().close();
            mLocalEndpointManager.close();
            mSystemInfo.close();

        } catch (IOException x) {
            throw new IOError(x);
        }
    }

    public static void main( String[] args )
    {
        final App app;

        try {
            app = new App();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        app.start();

        // Ensure we can shutdown properly.
        Runtime.getRuntime().addShutdownHook(new Thread(app::stop));

        System.out.println( "Server is running." );

        try {
            app.waitForStop();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

        } finally {
            System.out.println( "Server is stopping." );

            app.close();
        }
    }
}
