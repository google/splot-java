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

import java.io.IOException;

/**
 * Interface used to receive updates from a {@link PersistentStateInterface} regarding the need to
 * refresh the data stored in non-volatile memory. Unless you are implementing a {@link
 * PersistentStateManager}, there is no need to implement this interface.
 */
@SuppressWarnings("unused")
public interface PersistentStateListener {
    /**
     * Called whenever the persistent state of the given instance has changed. The instance will
     * call this method when it wants to save state to nonvolatile memory.
     *
     * <p>Once this method is called, the state will be committed to non-volatile memory at some
     * point in the near future. Notably, this method does not block.
     *
     * @param persistentStateInterface the instance which wants to save the persistent state.
     */
    void onSavePersistentState(PersistentStateInterface persistentStateInterface);

    /**
     * Called whenever the persistent state of the given instance has changed and the caller needs
     * to block execution until the state has been committed.
     *
     * <p>The class implementing {@link PersistentStateInterface} will call this method when it
     * wants to save state to nonvolatile memory.
     *
     * <p>This method will block execution until the state has been successfully written to
     * non-volatile storage.
     *
     * @param persistentStateInterface the instance which wants to save the persistent state.
     */
    void onSavePersistentStateBlocking(PersistentStateInterface persistentStateInterface)
            throws IOException;
}
