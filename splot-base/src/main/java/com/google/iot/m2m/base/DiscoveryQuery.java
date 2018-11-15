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
package com.google.iot.m2m.base;

import java.util.Set;
import java.util.concurrent.Executor;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * An abstract class representing a specific functional endpoint discovery query. Instances of this
 * class are used to track the progress and results of a discovery operation. Objects of this class
 * can be constructed using the {@link DiscoveryBuilder} class, which in turn is obtained from
 * {@link Technology#createDiscoveryQueryBuilder()}.
 *
 * @see DiscoveryBuilder#buildAndRun()
 */
public abstract class DiscoveryQuery {
    /**
     * Callback class for handling functional endpoints immediately as they are discovered and being
     * notified of the completion of a {@link DiscoveryQuery}.
     */
    @SuppressWarnings("EmptyMethod")
    public abstract static class Listener {
        public void onDiscoveryQueryFoundFunctionalEndpoint(
                FunctionalEndpoint functionalEndpoint) {}

        public void onDiscoveryQueryIsDone() {}
    }

    /**
     * Method to retrieve the set of discovered {@link FunctionalEndpoint}s. If the discovery query
     * is still in progress, this method will block execution until the discovery operation is
     * complete.
     *
     * <p>If blocking is unacceptable, use {@link #setListener} to be notified of discovered
     * functional endpoints asynchronously.
     *
     * @return the set of discovered functional endpoints
     * @throws InterruptedException if this thread was interrupted while waiting for the query to
     *     complete
     * @throws TechnologyException if there was an underlying technology-related problem
     * @see Listener
     * @see #setListener(Executor, Listener)
     */
    public abstract Set<FunctionalEndpoint> get() throws InterruptedException, TechnologyException;

    /** Restarts a discovery query, allowing a discovery query to be reused multiple times. */
    public abstract void restart();

    /** Stops the discovery query if it is in progress. */
    public abstract void stop();

    /**
     * Indicates if this query is currently running or not.
     *
     * @return false if the query is still running, false otherwise.
     */
    public abstract boolean isDone();

    /**
     * Set the listener class that will be called asynchronously as functional endpoints are
     * discovered.
     *
     * <p>There can only be one listener registered at a time. If set with a non-null listener, the
     * listener is guaranteed to be notified about every discovered functional endpoint, including
     * those discovered before the listener was registered with this method.
     *
     * <p>If this discovery query has already completed by the time this method is called, the
     * listener is still guaranteed to be called with the final results.
     *
     * @param executor the executor to use to dispatch calls to the methods on {@code listener}
     * @param listener an object implementing the {@link Listener} interface, or {@code null} to
     *     clear the previous listener
     */
    public abstract void setListener(Executor executor, @Nullable Listener listener);
}
