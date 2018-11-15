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
package com.google.iot.m2m.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation describing the characteristics of a <i>method</i> in a trait. This annotation is
 * applied to all {@link com.google.iot.m2m.base.MethodKey} fields designated as {@code public
 * static final} in a trait class. The value of this annotation is a bit-mask describing the
 * characteristics of the method.
 *
 * <h2>Example</h2>
 *
 * <pre><code>
 *     &#64;Method(REQUIRED | WANTS_GROUP_ID)
 *     public static final MethodKey<FunctionalEndpoint> METHOD_SAVE =
 *             new MethodKey<>(TRAIT_ID, "save", FunctionalEndpoint.class);
 * </code></pre>
 *
 * @see Trait
 * @see Property
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.CLASS)
public @interface Method {
    /** Indicates that this method is required for this trait. */
    int REQUIRED = 1;

    /**
     * Indicates that if this method is called through a group that the group id (with the argument
     * key "{@code gid}" should be automatically added to the arguments, unless that key is already
     * present.
     */
    int WANTS_GROUP_ID = (1 << 1);

    int value() default 0;
}
