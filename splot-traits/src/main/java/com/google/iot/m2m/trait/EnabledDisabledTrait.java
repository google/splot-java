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
package com.google.iot.m2m.trait;

import static com.google.iot.m2m.annotation.Property.*;

import com.google.iot.m2m.annotation.Property;
import com.google.iot.m2m.annotation.Trait;
import com.google.iot.m2m.base.PropertyKey;
import com.google.iot.m2m.base.Splot;

/** Trait for monitoring or controlling something that can be enabled or disabled. */
@Trait
public final class EnabledDisabledTrait {
    // Prevent instantiation
    private EnabledDisabledTrait() {}

    /** Abstract class for implementing trait behavior on a local functional endpoint. */
    public abstract static class AbstractLocalTrait extends LocalEnabledDisabledTrait {}

    /** The name of this trait */
    public static final String TRAIT_NAME = "EnabledDisabled";

    /** The URI that identifies the specification used to implement this trait. */
    public static final String TRAIT_URI =
            "tag:google.com,2018:m2m:traits:enabled-disabled:v1:v0#r0";

    /** The Short ID of this trait (<code>"enab"</code>) */
    public static final String TRAIT_ID = "enab";

    /** Flag indicating if this trait supports children or not. */
    public static final boolean TRAIT_SUPPORTS_CHILDREN = false;

    /**
     * Property key for the URI that identifies the specification used to implement this trait. This
     * property is present on all traits.
     */
    @Property(READ_ONLY | REQUIRED)
    public static final PropertyKey<String> META_TRAIT_URI =
            new PropertyKey<>(Splot.SECTION_METADATA, TRAIT_ID, "turi", String.class);

    @Property(READ_WRITE | GET_REQUIRED)
    public static final PropertyKey<Boolean> STAT_VALUE =
            new PropertyKey<>(Splot.SECTION_STATE, TRAIT_ID, "v", java.lang.Boolean.class);
}
