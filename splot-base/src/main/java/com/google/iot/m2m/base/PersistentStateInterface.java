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

import java.util.Map;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Interface for enabling the persistent storage of information related to an object. Intended to be
 * used in conjunction with a @{link PersistentStateManager}.
 *
 * @see PersistentStateManager
 */
public interface PersistentStateInterface {

    /**
     * Retrieves a copy of the state for the instance that should be saved to nonvolatile storage.
     * When reconstructing the associated instance, call {@link #initWithPersistentState} to restore
     * this state.
     *
     * <p>The data contained in the returned Map should be considered opaque unless specified
     * otherwise by the implementing technology.
     *
     * <p>The types for the values contained in the map are only guaranteed to be faithfully
     * restoreable if they are one of the following types:
     *
     * <ul>
     *   <li>{@link Integer}
     *   <li>{@link Double}
     *   <li>{@link Float}
     *   <li>{@code byte[]}
     *   <li>{@link String}
     *   <li>{@link java.util.Map}{@code <String,Object>}, where {@link Object} is also one of these
     *       acceptable types
     *   <li>{@link java.util.List}{@code <Object>}, where {@link Object} is also one of these
     *       acceptable types
     * </ul>
     *
     * This allows the object to be easily serialized to CBOR.
     *
     * <p>This method MUST be thread safe.
     *
     * @return the persistent state of this instance
     * @see #initWithPersistentState(Map)
     */
    Map<String, Object> copyPersistentState();

    /**
     * Restores a persistent state previously retrieved from {@link #copyPersistentState()}. This
     * should be performed only once when this instance is being initialized. The behavior for
     * calling this method more than once is undefined.
     *
     * @param persistentState a map previously returned from copyPersistentState(). May be <code>
     *     null</code> if there is not yet any persistent state associated with this object.
     */
    void initWithPersistentState(@Nullable Map<String, Object> persistentState);

    /**
     * Sets the listener to be called when the persistent state needs to be stored to non-volatile
     * memory. There can only be one listener.
     *
     * <p>This method's implementation MUST be thread safe.
     *
     * @param listener the listener to use to indicate that this instance needs to save the
     *     persistent state to non-volatile memory.
     */
    void setPersistentStateListener(@Nullable PersistentStateListener listener);
}
