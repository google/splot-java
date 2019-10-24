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

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;

/**
 * A persistent state manager handles the retrieval and updating of the persistent state of objects
 * which implement {@link PersistentStateInterface}. Such objects are registered by calling {@link
 * #startManaging(String, PersistentStateInterface)}. Each object managed must have a unique object
 * name that is long-lived. Typically, it consists of the RDNS domain of a project, followed by a
 * static string containing a short description of the object. For example, if we wanted to manager
 * a Thing representing the third outlet on a smart power strip, the name might be
 * <code>"com.example.smart-power-strip.outlet.1"</code>.
 */
public interface PersistentStateManager extends Closeable {

    /**
     * Starts managing the persistent state of the given object. If there exists state for this
     * objectName that was previously saved to non-volatile memory, it is restored by calling {@link
     * PersistentStateInterface#initWithPersistentState(Map)}. Then the given instance is monitored
     * for indications that it needs to refresh the persistent state to non-volatile memory.
     *
     * @param objectName the name of the object being managed
     * @param objectInstance the instance of the object being managed
     */
    void startManaging(String objectName, PersistentStateInterface objectInstance);

    /**
     * Stops the manager from monitoring an object previously registered with {@link
     * #startManaging}. If the given object is not being currently managed, calling this method does
     * nothing.
     *
     * @param objectName the name of the object to stop managing
     */
    void stopManaging(String objectName);

    /**
     * Reads the current persistent state of the indicated managed object and writes it to
     * non-volatile memory. Note that {@link #flush()} should be called after calling this method if
     * the intent is to save the state before powering down.
     */
    void refresh(String objectName);

    /**
     * Reads the current persistent state of all managed objects and writes it to non-volatile
     * memory. Note that {@link #flush()} should be called after calling this method if the intent
     * is to save the state before powering down.
     */
    void refresh();

    /**
     * Resets the persistent state for a particular object. Note that if the given object is
     * currently being managed, then the state will be restored if they end up indicating that they
     * need to be saved or if {@link #refresh(String)} is called.
     *
     * @param objectName the name of the object to erase the persistent state
     */
    void reset(String objectName);

    /**
     * Resets all persistent states managed by this object. Note that any objects which are
     * currently being managed will end up restoring their state if they end up indicating that they
     * need to be saved or if {@link #refresh()} is called.
     *
     * <p>This method is generally used to implement factory-reset capabilities.
     */
    void reset();

    /**
     * Blocks execution until all pending changes have been successfully written to non-volatile
     * storage.
     */
    void flush() throws IOException;
}
