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

import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.iot.coap.*;
import com.google.iot.m2m.local.LocalSceneFunctionalEndpoint;
import com.google.iot.m2m.local.LocalTransitioningFunctionalEndpoint;
import com.google.iot.m2m.trait.*;
import java.util.*;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.MockitoAnnotations;

@SuppressWarnings("ConstantConditions")
class SmcpTestBase {
    private static final boolean DEBUG = false;
    private static final Logger LOGGER = Logger.getLogger(SmcpTestBase.class.getCanonicalName());

    ListeningScheduledExecutorService mExecutor = null;
    volatile Throwable mThrowable = null;
    LocalEndpointManager mContextA = null;
    LocalEndpointManager mContextB = null;

    LoggingInterceptorFactory mInterceptorFactory = null;
    Interceptor mInterceptorA = null;
    Interceptor mInterceptorB = null;

    boolean mDidDump = false;

    public void rethrow() {
        if (mExecutor != null) {
            try {
                mExecutor.shutdown();
                if (!mExecutor.awaitTermination(50, TimeUnit.MILLISECONDS)) {
                    if (DEBUG) {
                        LOGGER.warning(
                                "Call to awaitTermination() is taking a long time"
                                        + " to finish, trying shutdownNow()");
                    }
                    mExecutor.shutdownNow();
                    if (mThrowable != null) {
                        mThrowable.printStackTrace();
                    }
                    assertTrue(
                            mExecutor.awaitTermination(1, TimeUnit.SECONDS),
                            "Call to awaitTermination() failed waiting for jobs to finish");
                }
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
            dumpLogs();
            LOGGER.info("Rethrowing throwable: " + x);
            if (x instanceof Error) throw (Error) x;
            if (x instanceof RuntimeException) throw (RuntimeException) x;
            throw new RuntimeException(x);
        }
    }

    public void dumpLogs() {
        /* We only dump logs here if DEBUG isn't set: because otherwise we are
         * dumping logs continuously.
         */
        if (!DEBUG && !mDidDump) System.err.println(mInterceptorFactory.toString());
        mDidDump = true;
    }

    @BeforeEach
    public void before() {
        if (DEBUG) LOGGER.info(" *** BEFORE");
        mThrowable = null;
        mDidDump = false;
        mExecutor =
                MoreExecutors.listeningDecorator(
                        new ScheduledThreadPoolExecutor(4) {
                            @Override
                            protected void afterExecute(Runnable r, @Nullable Throwable t) {
                                super.afterExecute(r, t);

                                if (t != null) {
                                    LOGGER.warning("Caught throwable: " + t);
                                    mThrowable = t;
                                }
                            }
                        });

        mContextA = new LocalEndpointManager(mExecutor);
        mContextB = new LocalEndpointManager(mExecutor);

        BehaviorContext behaviorContext = mContextA.getDefaultBehaviorContext();

        behaviorContext =
                new BehaviorContextPassthru(behaviorContext) {
                    @Override
                    public int getMulticastResponseAverageDelayMs() {
                        // Make the multicast response delay predictable.

                        return 0;
                    }
                };

        mInterceptorFactory = new LoggingInterceptorFactory();
        if (DEBUG) {
            mInterceptorFactory.setPrintStream(System.err);
        }

        mContextA.setDefaultBehaviorContext(behaviorContext);
        mInterceptorA = mInterceptorFactory.create("ContextA");
        mContextA.setDefaultInterceptor(mInterceptorA);

        mContextB.setDefaultBehaviorContext(behaviorContext);
        mInterceptorB = mInterceptorFactory.create("ContextB");
        mContextB.setDefaultInterceptor(mInterceptorB);

        MockitoAnnotations.initMocks(this);
    }

    @AfterEach
    public void after() throws Exception {
        if (DEBUG) LOGGER.info(" *** AFTER");
        mContextA.close();
        mContextB.close();
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

        private final OnOffTrait.AbstractLocalTrait mOnOffTrait =
                new OnOffTrait.AbstractLocalTrait() {
                    private float mDurationOff = 0.4f;
                    private float mDurationOn = 0.4f;

                    @Override
                    public Boolean onGetValue() {
                        return mIsOn;
                    }

                    @Override
                    public void onSetValue(@Nullable Boolean value) {
                        if (value != null && mIsOn != value) {
                            mIsOn = value;
                            didChangeValue(value);
                        }
                    }

                    @Override
                    public Float onGetDurationOn() {
                        return mDurationOn;
                    }

                    @Override
                    public Float onGetDurationOff() {
                        return mDurationOff;
                    }

                    @Override
                    public void onSetDurationOn(@Nullable Float value) {
                        if (value != null && value != mDurationOn) {
                            mDurationOn = value;
                            didChangeDurationOn(value);
                        }
                    }

                    @Override
                    public void onSetDurationOff(@Nullable Float value) {
                        if (value != null && value != mDurationOff) {
                            mDurationOff = value;
                            didChangeDurationOff(value);
                        }
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
