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

import com.google.iot.m2m.base.FunctionalEndpoint;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.Executor;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Class which allows you to associate an "identity" with an execution context. Identity objects are
 * singletons and may be compared directly using the <code>==</code> and <code>!=</code> operators.
 *
 * <p>Identity support is not yet ready for wide usage, thus it is marked as package-private for
 * now.
 */
class Identity implements Executor {
    private static final String DEFAULT_NAME = "DEFAULT";

    public static final Identity DEFAULT = new Identity(DEFAULT_NAME);

    private final String mName;

    private static final ThreadLocal<Identity> mCurrentIdentity = new ThreadLocal<>();
    private static final Map<String, WeakReference<Identity>> sIdentityMap = new WeakHashMap<>();

    /**
     * Package-private method for determining the number of outstanding {@link Identity} objects in
     * circulation. This method is slow and intended only to be used by the garbage collection unit
     * test.
     */
    static int count() {
        int ret = 0;
        synchronized (sIdentityMap) {
            for (WeakReference<Identity> ref : sIdentityMap.values()) {
                if (ref.get() != null) {
                    ret++;
                }
            }
        }

        return ret;
    }

    /**
     * Returns an {@link Identity} object with the given name.
     *
     * @param name the name of the returned identity.
     * @return the identity object with that name.
     */
    public static Identity get(String name) {
        if (DEFAULT_NAME.equals(name)) {
            return DEFAULT;
        }

        Identity ret = null;

        synchronized (sIdentityMap) {
            WeakReference<Identity> ref = sIdentityMap.get(name);

            if (ref != null) {
                ret = ref.get();
            }

            if (ret == null) {
                ret = new Identity(name);
                sIdentityMap.put(name, new WeakReference<>(ret));
            }
        }
        return ret;
    }

    /**
     * Returns a new {@link Identity} object with a random name.
     *
     * @return a new identity object
     */
    public static Identity create() {
        synchronized (sIdentityMap) {
            String uid;

            do {
                uid = FunctionalEndpoint.generateNewUid();
            } while (sIdentityMap.containsKey(uid));

            return get(uid);
        }
    }

    /**
     * Returns the identity that is associated with this execution context.
     *
     * @return the current identity, or null if there is no identity associated with this context.
     */
    @Nullable
    public static Identity current() {
        Identity ret = mCurrentIdentity.get();
        return ret == null ? DEFAULT : ret;
    }

    private Identity(String name) {
        mName = name;
    }

    /** Returns the name of this identity. */
    public String name() {
        return mName;
    }

    /**
     * Indicates if this is the current identity or not.
     *
     * @return True if this is the current identity, false otherwise.
     */
    public boolean isCurrent() {
        return current() == this;
    }

    /**
     * Requires that this be the current identity.
     *
     * @throws IdentityException if this Identity isn't the current identity.
     */
    public void require() {
        Identity identity = current();
        if (identity != this) {
            throw new IdentityException(
                    "Identity \"" + this + "\" required, but context was \"" + identity + "\"");
        }
    }

    /**
     * Requires that this NOT be the current identity.
     *
     * @throws IdentityException if this Identity is the current identity.
     */
    public void exclude() {
        if (isCurrent()) {
            throw new IdentityException("Identity \"" + this + "\" is explicitly not allowed");
        }
    }

    /**
     * Returns an executor that will execute commands using the given executor but using this
     * identity.
     */
    public Executor wrapExecutor(Executor executor) {
        return command -> executor.execute(() -> Identity.this.execute(command));
    }

    /**
     * Immediately executes the given {@link Runnable} with this identity as context. Execution will
     * block until the runnable object has finished.
     *
     * @param command the {@link Runnable} to execute from the context of this identity.
     */
    @Override
    public void execute(Runnable command) {
        Identity lastIdentity = current();
        try {
            mCurrentIdentity.set(this);
            command.run();
        } finally {
            mCurrentIdentity.set(lastIdentity);
        }
    }

    @Override
    public String toString() {
        return mName;
    }
}
