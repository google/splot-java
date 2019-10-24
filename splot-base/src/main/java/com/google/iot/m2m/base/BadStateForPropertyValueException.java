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

/**
 * Checked exception thrown after an attempt to set a property to a value that is not acceptable
 * for the current state of the thing.
 *
 * @see Thing#setProperty(PropertyKey, Object)
 * @see Thing#incrementProperty(PropertyKey, Number)
 * @see Thing#addValueToProperty(PropertyKey, Object)
 * @see Thing#removeValueFromProperty(PropertyKey, Object)
 */
public class BadStateForPropertyValueException extends PropertyException {
    @SuppressWarnings("unused")
    public BadStateForPropertyValueException() {}

    public BadStateForPropertyValueException(String reason) {
        super(reason);
    }

    public BadStateForPropertyValueException(String reason, Throwable t) {
        super(reason, t);
    }

    public BadStateForPropertyValueException(Throwable t) {
        super(t.getMessage() != null ? t.getMessage() : t.toString(), t);
    }
}
