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
 * Checked exception thrown after an attempt to set a property to an invalid value. The value may
 * either be of the wrong type or it may be that the value it contains is out of range or in the
 * incorrect format.
 *
 * @see Thing#setProperty(PropertyKey, Object)
 * @see Thing#incrementProperty(PropertyKey, Number)
 * @see Thing#addValueToProperty(PropertyKey, Object)
 * @see Thing#removeValueFromProperty(PropertyKey, Object)
 */
public class InvalidPropertyValueException extends PropertyException {
    @SuppressWarnings("unused")
    public InvalidPropertyValueException() {}

    public InvalidPropertyValueException(String reason) {
        super(reason);
    }

    public InvalidPropertyValueException(String reason, Throwable t) {
        super(reason, t);
    }

    public InvalidPropertyValueException(Throwable t) {
        super(t);
    }
}
