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

import java.io.File;

/**
 * Checked exception for when the underlying persistent state data is corrupted.
 *
 * @see PersistentStateManager
 * @see com.google.iot.m2m.util.FilePersistentStateManager#create(File)
 */
@SuppressWarnings("unused")
public class CorruptPersistentStateException extends Exception {
    public CorruptPersistentStateException() {}

    public CorruptPersistentStateException(String reason) {
        super(reason);
    }

    public CorruptPersistentStateException(String reason, Throwable t) {
        super(reason, t);
    }

    public CorruptPersistentStateException(Throwable t) {
        super(t);
    }
}
