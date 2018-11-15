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

/**
 * A checked exception specific to {@link TypedKey} that indicates that a cast or coercion has
 * failed.
 *
 * @see TypedKey#coerce(Object)
 * @see TypedKey#coerceFromMap(Map)
 * @see TypedKey#getFromMap(Map)
 */
public class InvalidValueException extends Exception {
    public InvalidValueException() {}

    public InvalidValueException(String reason) {
        super(reason);
    }

    public InvalidValueException(String reason, Throwable t) {
        super(reason, t);
    }

    public InvalidValueException(Throwable t) {
        super(t);
    }
}
