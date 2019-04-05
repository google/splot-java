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
package com.google.iot.m2m.local;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.iot.m2m.base.*;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executor;

/**
 * Abstract class for implementing lazily-resolved ResourceLinks.
 */
public abstract class LazyResourceLink<T> extends ResourceLink<T> {
    private final Map<Listener<T>, Executor> mListenerMap = new HashMap<>();
    private volatile ResourceLink<T> mResourceLink = null;
    private volatile boolean mLastInvokedValueValid = false;
    private volatile T mLastInvokedValue = null;

    /**
     * Changes the underlying ResourceLink.
     *
     * <p>This method called as a result of a call to {@link #resolve()}. If a ResourceLink
     * previously set by this method becomes suddenly unavailable, then this method can be
     * called with a {@code null} argument.
     *
     * @param resourceLink the resolved ResourceLink, or {@code null} if a previously-resolved
     *                     ResourceLink is no longer available.
     */
    protected synchronized final void setResolvedResourceLink(@Nullable ResourceLink<T> resourceLink) {
        if (Objects.equals(mResourceLink, resourceLink)) {
            return;
        }

        if (mResourceLink != null) {
            synchronized (mListenerMap) {
                for (Listener<T> listener : mListenerMap.keySet()) {
                    mResourceLink.unregisterListener(listener);
                }
            }
        }

        mResourceLink = resourceLink;

        if (mResourceLink != null) {
            synchronized (mListenerMap) {
                for (Map.Entry<Listener<T>, Executor> entry : mListenerMap.entrySet()) {
                    mResourceLink.registerListener(entry.getValue(), entry.getKey());
                }
            }

            if (mLastInvokedValueValid) {
                mResourceLink.invoke(mLastInvokedValue);
                mLastInvokedValueValid = false;
                mLastInvokedValue = null;
            }
        }
    }

    protected final ResourceLink<T> getResolvedResourceLink() {
        return mResourceLink;
    }

    /**
     * Determines if this LazyResourceLink has been successfully resolved and is ready to use.
     * @return true if this instance has been successfully resolved, false otherwise.
     */
    public final boolean hasResolved() {
        return mResourceLink != null;
    }

    /**
     * Registers an external request to (re-)resolve the resource.
     *
     * <p>This method is implemented by subclasses of this class.
     *
     * <p>If the resolution was immediately successful, this method should return true.
     * Otherwise, it should return false. Invoking this method may optionally trigger
     * resolution asynchronously, in which case it should return false.
     *
     * <p>Once the resource has been resolved, the implementation of this method should call
     * {@link #setResolvedResourceLink(ResourceLink)} with the resolved ResourceLink instance.
     *
     * @return true if the resolution was immediately successful, false otherwise.
     * @see #setResolvedResourceLink(ResourceLink)
     * @see #getResolvedResourceLink()
     */
    @CanIgnoreReturnValue
    abstract public boolean resolve();

    @Override
    public synchronized ListenableFuture<T> fetchValue() {
        if (mResourceLink == null) {
            if (!resolve() || mResourceLink == null) {
                return Futures.immediateFailedFuture(new TechnologyException("Unable to resolve path"));
            }
        }

        return mResourceLink.fetchValue();
    }

    @Override
    public synchronized ListenableFuture<?> invoke(@Nullable T value) {
        if (mResourceLink == null) {
            if (!resolve() || mResourceLink == null) {
                mLastInvokedValue = value;
                mLastInvokedValueValid = true;
                return Futures.immediateFailedFuture(new TechnologyException("Unable to resolve path"));
            }
        }

        return mResourceLink.invoke(value);
    }

    @Override
    public synchronized final void registerListener(Executor executor, Listener<T> listener) {
        mListenerMap.put(listener, executor);
        if (mResourceLink != null) {
            mResourceLink.registerListener(executor, listener);
        }
    }

    @Override
    public synchronized final void unregisterListener(Listener<T> listener) {
        mListenerMap.remove(listener);
        if (mResourceLink != null) {
            mResourceLink.unregisterListener(listener);
        }
    }

    @Override
    public String toString() {
        return "<LazyResourceLink " + mResourceLink + ">";
    }
}
