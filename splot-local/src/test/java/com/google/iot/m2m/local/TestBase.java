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

import static org.junit.jupiter.api.Assertions.*;

import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.iot.m2m.trait.*;
import java.util.*;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.MockitoAnnotations;

abstract class TestBase {
    private static final boolean DEBUG = false;
    private static final Logger LOGGER = Logger.getLogger(TestBase.class.getCanonicalName());

    ListeningScheduledExecutorService mExecutor = null;
    volatile Throwable mThrowable = null;

    public void rethrow() {
        if (mExecutor != null) {
            try {
                Thread.sleep(1);
                mExecutor.shutdown();
                assertTrue(mExecutor.awaitTermination(1, TimeUnit.SECONDS));
            } catch (Exception x) {
                if (mThrowable == null) {
                    mThrowable = x;
                } else {
                    LOGGER.info("Got exception while flushing queue: " + x);
                    x.printStackTrace();
                }
            }
            mExecutor = null;
        }
        if (mThrowable != null) {
            Throwable x = mThrowable;
            mThrowable = null;
            LOGGER.info("Rethrowing throwable: " + x);
            if (x instanceof Error) throw (Error) x;
            if (x instanceof RuntimeException) throw (RuntimeException) x;
            throw new RuntimeException(x);
        }
    }

    @BeforeEach
    public void before() {
        MockitoAnnotations.initMocks(this);
        mThrowable = null;
        mExecutor =
                MoreExecutors.listeningDecorator(
                        new ScheduledThreadPoolExecutor(1) {
                            @Override
                            protected void afterExecute(Runnable r, @Nullable Throwable t) {
                                super.afterExecute(r, t);

                                if (t != null) {
                                    LOGGER.info("Caught throwable: " + t);
                                    mThrowable = t;
                                }
                            }
                        });
    }

    @AfterEach
    public void after() {
        rethrow();
    }

    void tick(int durationInMs) throws InterruptedException {
        Thread.sleep(durationInMs);
    }

    static class MyLightBulbImpl {
        boolean mIsOn = false;
        float mLevel = 0.0f;

        final BaseTrait.AbstractLocalTrait mBaseTrait =
                new BaseTrait.AbstractLocalTrait() {
                    @Override
                    public Map<String, String> onGetProductName() {
                        Map<String, String> nameMap = new LinkedHashMap<>();
                        nameMap.put(Locale.ENGLISH.toLanguageTag(), "Light Bulb");
                        nameMap.put(Locale.FRENCH.toLanguageTag(), "Ampoule");
                        nameMap.put(Locale.JAPANESE.toLanguageTag(), "電球");
                        return nameMap;
                    }

                    @Override
                    public String onGetManufacturer() {
                        return "Acme, Inc.";
                    }

                    @Override
                    public String onGetModel() {
                        return "LB01";
                    }
                };

        final OnOffTrait.AbstractLocalTrait mOnOffTrait =
                new OnOffTrait.AbstractLocalTrait() {
                    @Override
                    public Boolean onGetValue() {
                        return mIsOn;
                    }

                    @Override
                    public void onSetValue(@Nullable Boolean value) {
                        if (value != null && mIsOn != value) {
                            mIsOn = value;
                            didChangeValue(mIsOn);
                        }
                    }

                    @Override
                    public Float onGetDurationOn() {
                        return 0.4f;
                    }

                    @Override
                    public Float onGetDurationOff() {
                        return 0.4f;
                    }
                };

        final LevelTrait.AbstractLocalTrait mLevelTrait =
                new LevelTrait.AbstractLocalTrait() {
                    @Override
                    public Float onGetValue() {
                        return mLevel;
                    }

                    @Override
                    public void onSetValue(@Nullable Float value) {
                        if (value != null && mLevel != value) {
                            mLevel = value;
                            didChangeValue(mLevel);
                        }
                    }
                };
    }

    class MyLightBulb extends LocalTransitioningFunctionalEndpoint {
        private final MyLightBulbImpl mImpl = new MyLightBulbImpl();

        MyLightBulb() {
            registerTrait(mImpl.mBaseTrait);
            registerTrait(mImpl.mOnOffTrait);
            registerTrait(mImpl.mLevelTrait);
        }

        @Override
        protected ListeningScheduledExecutorService getExecutor() {
            return mExecutor;
        }
    }

    class MyLightBulbNoTrans extends LocalSceneFunctionalEndpoint {
        private final MyLightBulbImpl mImpl = new MyLightBulbImpl();

        MyLightBulbNoTrans() {
            registerTrait(mImpl.mBaseTrait);
            registerTrait(mImpl.mOnOffTrait);
            registerTrait(mImpl.mLevelTrait);
        }

        @Override
        protected ListeningScheduledExecutorService getExecutor() {
            return mExecutor;
        }
    }
}
