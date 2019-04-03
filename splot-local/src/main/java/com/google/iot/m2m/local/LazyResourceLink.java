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
import com.google.iot.m2m.trait.TransitionTrait;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executor;

public abstract class LazyResourceLink<T> extends ResourceLink<T> {
    private final Map<Listener<T>, Executor> mListenerMap = new HashMap<>();
    private volatile ResourceLink<T> mResourceLink = null;
    private volatile boolean mLastInvokedValueValid = false;
    private volatile T mLastInvokedValue = null;

    protected synchronized final void setContainedResourceLink(@Nullable ResourceLink<T> resourceLink) {
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

    protected final ResourceLink<T> getContainedResourceLink() {
        return mResourceLink;
    }

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
