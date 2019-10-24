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
 * Checked exception thrown when the arguments to a Splot method are invalid.
 *
 * @see Thing#invokeMethod(MethodKey, Map)
 * @see Thing#invokeMethod(MethodKey)
 * @see Thing#invokeMethod(MethodKey, ParamKey, Object...)
 */
public class InvalidMethodArgumentsException extends MethodException {
    @SuppressWarnings("unused")
    public InvalidMethodArgumentsException() {}

    public InvalidMethodArgumentsException(String reason) {
        super(reason);
    }

    @SuppressWarnings("unused")
    public InvalidMethodArgumentsException(String reason, Throwable t) {
        super(reason, t);
    }

    @SuppressWarnings("unused")
    public InvalidMethodArgumentsException(Throwable t) {
        super(t);
    }
}
