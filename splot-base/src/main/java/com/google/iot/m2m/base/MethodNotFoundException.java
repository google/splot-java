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
 * Checked exception that indicates that a given method could not be found on this {@link
 * Thing}.
 *
 * @see Thing#invokeMethod(MethodKey, Map)
 * @see Thing#invokeMethod(MethodKey)
 * @see Thing#invokeMethod(MethodKey, ParamKey, Object...)
 */
public class MethodNotFoundException extends MethodException {
    public MethodNotFoundException() {}

    public MethodNotFoundException(String reason) {
        super(reason);
    }

    @SuppressWarnings("unused")
    public MethodNotFoundException(String reason, Throwable t) {
        super(reason, t);
    }

    @SuppressWarnings("unused")
    public MethodNotFoundException(Throwable t) {
        super(t);
    }
}
