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

import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import org.checkerframework.checker.nullness.qual.Nullable;

class Utils {
    private static ListeningScheduledExecutorService sDefaultExecutor = null;

    private static class SafeScheduledExecutorService extends ScheduledThreadPoolExecutor {
        SafeScheduledExecutorService(int corePoolSize) {
            super(corePoolSize);
        }

        @Override
        protected void afterExecute(Runnable r, @Nullable Throwable t) {
            super.afterExecute(r, t);

            if (t != null) {
                Thread.getDefaultUncaughtExceptionHandler()
                        .uncaughtException(Thread.currentThread(), t);
            }
        }
    }

    /** Returns a singleton {@link ScheduledExecutorService}. */
    static synchronized ListeningScheduledExecutorService getDefaultExecutor() {
        if (sDefaultExecutor == null || sDefaultExecutor.isShutdown()) {
            sDefaultExecutor =
                    MoreExecutors.listeningDecorator(new SafeScheduledExecutorService(3));
        }

        return sDefaultExecutor;
    }
}
