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

import com.google.iot.m2m.base.PersistentStateInterface;
import com.google.iot.m2m.base.PersistentStateListener;
import com.google.iot.m2m.base.PersistentStateManager;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A nested persistent state manager that aggregates the persistent state of several objects under a
 * single persistent state interface which can itself be managed by another PersistentStateManager.
 *
 * <p>Note that {@link #initWithPersistentState(Map)} must be called before any objects are managed
 * via {@link #startManaging(String, PersistentStateInterface)}. The behavior of calling {@link
 * #initWithPersistentState(Map)} after the first call to {@link #startManaging(String,
 * PersistentStateInterface)} is undefined.
 */
public class NestedPersistentStateManager
        implements PersistentStateManager, PersistentStateInterface {
    private static final boolean DEBUG = false;
    private static final Logger LOGGER =
            Logger.getLogger(NestedPersistentStateManager.class.getCanonicalName());

    private volatile PersistentStateListener mListener = null;
    private final Map<String, PersistentStateInterface> mManagedObjects = new HashMap<>();
    private final Map<String, Map<String, Object>> mPersistentState = new HashMap<>();

    @Override
    public Map<String, Object> copyPersistentState() {
        synchronized (mPersistentState) {
            return new HashMap<>(mPersistentState);
        }
    }

    @Override
    public void initWithPersistentState(@Nullable Map<String, Object> persistentState) {
        if (!mManagedObjects.isEmpty()) {
            throw new IllegalStateException(
                    "initWithPersistentState() cannot be called after first call to startManaging()");
        }
        if (persistentState != null) {
            @SuppressWarnings("unchecked")
            Map<String, Map<String, Object>> casted = (Map) persistentState;
            mPersistentState.putAll(casted);
        }
    }

    @Override
    public void setPersistentStateListener(@Nullable PersistentStateListener listener) {
        mListener = listener;
    }

    @Override
    public synchronized void startManaging(
            String objectName, PersistentStateInterface objectInstance) {
        synchronized (mManagedObjects) {
            mManagedObjects.put(objectName, objectInstance);
        }

        Map<String, Object> map;

        synchronized (mPersistentState) {
            map = mPersistentState.get(objectName);
        }

        objectInstance.initWithPersistentState(map);

        objectInstance.setPersistentStateListener(
                new PersistentStateListener() {
                    @Override
                    public void onSavePersistentState(
                            PersistentStateInterface persistentStateInterface) {
                        refresh(objectName);
                        changedPersistentState();
                    }

                    @Override
                    public void onSavePersistentStateBlocking(
                            PersistentStateInterface persistentStateInterface) throws IOException {
                        refresh(objectName);
                        changedPersistentStateBlocking();
                    }
                });

        refresh(objectName);

        if (map == null) {
            changedPersistentState();
        }
    }

    private void changedPersistentState() {
        final PersistentStateListener listener = mListener;
        if (listener != null) {
            listener.onSavePersistentState(this);
        }
    }

    private void changedPersistentStateBlocking() throws IOException {
        final PersistentStateListener listener = mListener;
        if (listener != null) {
            listener.onSavePersistentStateBlocking(this);
        }
    }

    @Override
    public synchronized void stopManaging(String objectName) {
        refresh(objectName);
        synchronized (mManagedObjects) {
            PersistentStateInterface objectInstance = mManagedObjects.get(objectName);
            objectInstance.setPersistentStateListener(null);
            mManagedObjects.remove(objectName);
        }
    }

    @Override
    public void refresh(String objectName) {
        final Map<String, Object> state;
        synchronized (mManagedObjects) {
            state = mManagedObjects.get(objectName).copyPersistentState();
        }
        synchronized (mPersistentState) {
            mPersistentState.put(objectName, state);
        }
    }

    @Override
    public void refresh() {
        synchronized (mManagedObjects) {
            mManagedObjects.keySet().forEach(this::refresh);
        }
    }

    @Override
    public void reset(String objectName) {
        synchronized (mPersistentState) {
            mPersistentState.remove(objectName);
        }
    }

    @Override
    public void reset() {
        synchronized (mPersistentState) {
            mPersistentState.clear();
        }
    }

    @Override
    public void flush() throws IOException {
        changedPersistentStateBlocking();
    }

    @Override
    public void close() {
        // Does nothing.
    }
}
