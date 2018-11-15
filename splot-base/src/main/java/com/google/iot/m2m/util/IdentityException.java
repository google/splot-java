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
package com.google.iot.m2m.util;

/**
 * Exception thrown for identity violations. Identity support is not yet ready for wide usage, thus
 * this class is marked as package-private for now.
 *
 * @see Identity#require()
 * @see Identity#exclude()
 */
class IdentityException extends SecurityException {
    @SuppressWarnings("unused")
    public IdentityException() {}

    public IdentityException(String reason) {
        super(reason);
    }

    @SuppressWarnings("unused")
    public IdentityException(String reason, Throwable t) {
        super(reason, t);
    }

    @SuppressWarnings("unused")
    public IdentityException(Throwable t) {
        super(t);
    }
}
