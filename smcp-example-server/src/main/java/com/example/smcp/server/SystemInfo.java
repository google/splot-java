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

import com.google.iot.m2m.local.LocalFunctionalEndpoint;
import com.google.iot.m2m.trait.BaseTrait;
import com.google.iot.m2m.trait.SystemTrait;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Functional Endpoint which exposes information about the operating system,
 * such as its name, version, load average, and number of available CPUs.
 */
public class SystemInfo extends LocalFunctionalEndpoint {
    private final static int LOAD_AVERAGE_PERIOD_SECONDS = 5;

    private final OperatingSystemMXBean mMxBean;
    private final ScheduledExecutorService mExecutor;
    private final Future<?> mLoadAverageUpdateTimer;

    private final BaseTrait.AbstractLocalTrait mBaseTrait =
            new BaseTrait.AbstractLocalTrait() {
                @Override
                public Map<String, String> onGetProductName() {
                    Map<String, String> nameMap = new HashMap<>();
                    nameMap.put(Locale.ENGLISH.toLanguageTag(), "System Info");
                    return nameMap;
                }

                @Override
                public String onGetManufacturer() {
                    return "Acme, Inc.";
                }

                @Override
                public String onGetModel() {
                    return "SI01";
                }
            };

    private final SystemTrait.AbstractLocalTrait mSystemTrait =
            new SystemTrait.AbstractLocalTrait() {
                @Override
                public Double onGetLoadAverage() {
                    return mMxBean.getSystemLoadAverage();
                }

                @Override
                public Integer onGetCpuCount() {
                    return mMxBean.getAvailableProcessors();
                }

                @Override
                public String onGetOsName() {
                    return mMxBean.getName();
                }

                @Override
                public String onGetOsVersion() {
                    return mMxBean.getVersion();
                }
            };

    public SystemInfo(ScheduledExecutorService executor, OperatingSystemMXBean mxBean) {
        mMxBean = mxBean;
        mExecutor = executor;
        mLoadAverageUpdateTimer = mExecutor.scheduleAtFixedRate(
                () -> mSystemTrait.didChangeLoadAverage(mMxBean.getSystemLoadAverage()),
                LOAD_AVERAGE_PERIOD_SECONDS,
                LOAD_AVERAGE_PERIOD_SECONDS,
                TimeUnit.SECONDS);
        registerTrait(mBaseTrait);
        registerTrait(mSystemTrait);
    }

    public SystemInfo(ScheduledExecutorService executor) {
        this(executor, ManagementFactory.getOperatingSystemMXBean());
    }

    public SystemInfo() {
        this(new ScheduledThreadPoolExecutor(1));
    }

    public void close() {
        mLoadAverageUpdateTimer.cancel(true);
    }
}
