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
 * Checked exception that indicates that a given property can only be read, not written.
 *
 * @see Thing#setProperty(PropertyKey, Object)
 * @see Thing#incrementProperty(PropertyKey, Number)
 * @see Thing#toggleProperty(PropertyKey)
 * @see Thing#addValueToProperty(PropertyKey, Object)
 * @see Thing#removeValueFromProperty(PropertyKey, Object)
 */
public class PropertyReadOnlyException extends PropertyException {
    public PropertyReadOnlyException() {}

    public PropertyReadOnlyException(String reason) {
        super(reason);
    }

    @SuppressWarnings("unused")
    public PropertyReadOnlyException(String reason, Throwable t) {
        super(reason, t);
    }

    @SuppressWarnings("unused")
    public PropertyReadOnlyException(Throwable t) {
        super(t);
    }
}
