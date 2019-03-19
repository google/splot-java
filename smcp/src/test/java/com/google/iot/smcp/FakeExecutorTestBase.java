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
package com.google.iot.smcp;

import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Logger;

@SuppressWarnings("ConstantConditions")
class FakeExecutorTestBase extends ExecutorTestBase {
    private static final boolean DEBUG = false;
    private static final Logger LOGGER =
            Logger.getLogger(ExecutorTestBase.class.getCanonicalName());

    public ScheduledExecutorService createNewScheduledExecutorService() {
        return new FakeScheduledExecutorService() {
            @Override
            public void execute(Runnable command) {
                super.execute(
                        () -> {
                            try {
                                command.run();
                            } catch (Throwable x) {
                                LOGGER.info("Caught throwable: " + x);
                                x.printStackTrace();
                                mThrowable = x;
                            }
                        });
            }
        };
    }

    public long nanoTime() {
        return ((FakeScheduledExecutorService) mOriginalExecutor).nanoTime();
    }

    public void tick(int durationInMs) throws Exception {
        if (DEBUG) LOGGER.info("tick(" + durationInMs + ") ENTER");
        super.tick(1);
        ((FakeScheduledExecutorService) mOriginalExecutor).tick(durationInMs);
        super.tick(1);
        if (DEBUG) LOGGER.info("tick(" + durationInMs + ") EXIT");
    }
}
