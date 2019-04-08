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
package com.google.iot.m2m.util;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class IdentityTest {
    private static final boolean DEBUG = false;
    private static final Logger LOGGER = Logger.getLogger(IdentityTest.class.getCanonicalName());

    ScheduledExecutorService mExecutor = null;
    volatile Throwable mThrowable = null;

    public void rethrow() {
        if (mExecutor != null) {
            try {
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
        mThrowable = null;
        mExecutor =
                new ScheduledThreadPoolExecutor(1) {
                    @Override
                    public void execute(Runnable command) {
                        super.execute(
                                () -> {
                                    try {
                                        command.run();
                                    } catch (Throwable x) {
                                        LOGGER.info("Caught throwable: " + x);
                                        mThrowable = x;
                                    }
                                });
                    }
                };
    }

    @AfterEach
    public void after() throws Exception {
        rethrow();
    }

    @Test
    void get() {
        Identity idBlah = Identity.get("blah");

        assertEquals(idBlah, Identity.get("blah"));
        assertNotEquals(idBlah, Identity.get("foobar"));
    }

    @Test
    void isCurrent() {
        assertTrue(Identity.DEFAULT.isCurrent());
        assertFalse(Identity.create().isCurrent());
    }

    @Test
    void current() {
        assertEquals(Identity.DEFAULT, Identity.current());

        Identity idFoobar = Identity.get("foobar");
        Identity idBlah = Identity.get("blah");

        assertNotEquals(idBlah, idFoobar);

        idFoobar.execute(() -> assertEquals(idFoobar, Identity.current()));

        assertEquals(Identity.DEFAULT, Identity.current());

        idBlah.execute(
                () -> {
                    assertEquals(idBlah, Identity.current());

                    idFoobar.execute(() -> assertEquals(idFoobar, Identity.current()));

                    try {
                        mExecutor
                                .submit(
                                        () -> {
                                            assertEquals(Identity.DEFAULT, Identity.current());
                                            idFoobar.execute(
                                                    () ->
                                                            assertEquals(
                                                                    idFoobar, Identity.current()));
                                            assertEquals(Identity.DEFAULT, Identity.current());
                                        })
                                .get();
                    } catch (Exception x) {
                        throw new RuntimeException(x);
                    }
                });

        assertEquals(Identity.DEFAULT, Identity.current());
    }

    @Test
    void name() {
        Identity idBlah = Identity.get("blah");

        assertEquals("blah", idBlah.name());
    }

    @Test
    void require() {
        assertEquals(Identity.DEFAULT, Identity.current());

        Identity idFoobar = Identity.get("foobar");
        Identity idBlah = Identity.get("blah");

        idFoobar.execute(
                () -> {
                    assertThrows(SecurityException.class, idBlah::require);
                    idFoobar.require();
                });

        assertThrows(SecurityException.class, idFoobar::require);
        assertThrows(SecurityException.class, idBlah::require);
    }

    @Test
    void requireAny() {
        assertEquals(Identity.DEFAULT, Identity.current());

        Identity idFoobar = Identity.get("foobar");
        Identity idBlah = Identity.get("blah");

        idFoobar.execute(
                () -> {
                    assertThrows(SecurityException.class,
                            ()->Identity.require(Arrays.asList(idBlah)));
                    Identity.require(Arrays.asList(idBlah, idFoobar));
                });

        assertThrows(SecurityException.class,
                ()->Identity.require(Arrays.asList(idBlah, idFoobar)));
    }

    @Test
    void exclude() {
        assertEquals(Identity.DEFAULT, Identity.current());

        Identity idFoobar = Identity.get("foobar");
        Identity idBlah = Identity.get("blah");

        idFoobar.execute(
                () -> {
                    assertThrows(SecurityException.class, idFoobar::exclude);
                    idBlah.exclude();
                    Identity.DEFAULT.exclude();
                });

        idFoobar.exclude();
        idBlah.exclude();
    }

    @Test
    void wrapExecutor() {
        assertEquals(Identity.DEFAULT, Identity.current());

        Identity idFoobar = Identity.get("foobar");
        Identity idBlah = Identity.get("blah");
        AtomicBoolean didRun = new AtomicBoolean(false);

        idFoobar.wrapExecutor(Runnable::run)
                .execute(
                        () -> {
                            idFoobar.require();
                            idBlah.exclude();
                            Identity.DEFAULT.exclude();
                            didRun.set(true);
                        });

        assertTrue(didRun.get());

        assertEquals(Identity.DEFAULT, Identity.current());
    }

    @Test
    void wrapScheduledExecutor() throws Exception {
        assertEquals(Identity.DEFAULT, Identity.current());

        Identity idFoobar = Identity.get("foobar");
        Identity idBlah = Identity.get("blah");

        AtomicBoolean didRun = new AtomicBoolean(false);

        idFoobar.wrapExecutor(mExecutor)
                .execute(
                        () -> {
                            idFoobar.require();
                            idBlah.exclude();
                            Identity.DEFAULT.exclude();
                            didRun.set(true);
                        });

        assertTrue(didRun.get());

        ScheduledExecutorService foobarExecutor = idFoobar.wrapExecutor(mExecutor);

        foobarExecutor.submit(
                () -> {
                    idFoobar.require();
                    idBlah.exclude();
                    Identity.DEFAULT.exclude();
                    didRun.set(true);
                }).get();

        foobarExecutor.schedule(
                () -> {
                    idFoobar.require();
                    idBlah.exclude();
                    Identity.DEFAULT.exclude();
                    didRun.set(true);
                },
                10,
                TimeUnit.MILLISECONDS).get();

        assertEquals("yay",
            foobarExecutor.schedule(
                    () -> {
                        idFoobar.require();
                        idBlah.exclude();
                        Identity.DEFAULT.exclude();
                        didRun.set(true);
                        return "yay";
                    },
                    10,
                    TimeUnit.MILLISECONDS).get());

        assertEquals(Identity.DEFAULT, Identity.current());
    }

    private static void forceGc() {
        final WeakReference<Object> ref = new WeakReference<>(new Object());
        while (ref.get() != null) {
            System.gc();
        }
    }

    @Test
    void garbageCollection() {
        /* This test ensures that orphaned Identity objects get garbage collected. */

        final int total = 100000;
        int before = Identity.count();

        for (int i = 0; i < total; i++) {
            Identity.create().exclude();
        }

        forceGc();

        assertEquals(before, Identity.count());
    }

    @Test
    void toStringTest() {
        Identity idBlah = Identity.get("blah");

        assertEquals("blah", idBlah.toString());
    }
}
