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
 * Checked exception thrown when a given {@link Technology} instance rejects a {@link
 * Thing} argument because it is somehow unacceptable. For example, if you attempt to
 * call {@link Group#addMember(Thing)} with a {@link Thing} that is
 * unrelated to that group's {@link Technology} this exception will be thrown.
 *
 * @see Group#addMember(Thing)
 * @see Technology#host(Thing)
 */
public class TechnologyCannotHostException extends TechnologyRuntimeException {
    @SuppressWarnings("unused")
    public TechnologyCannotHostException() {}

    @SuppressWarnings("unused")
    public TechnologyCannotHostException(String reason) {
        super(reason);
    }

    @SuppressWarnings("unused")
    public TechnologyCannotHostException(String reason, Throwable t) {
        super(reason, t);
    }

    @SuppressWarnings("unused")
    public TechnologyCannotHostException(Throwable t) {
        super(t);
    }
}
