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

import com.google.iot.m2m.annotation.Property;
import com.google.iot.m2m.annotation.Trait;
import com.google.iot.m2m.base.PropertyKey;
import com.google.iot.m2m.base.Section;

import static com.google.iot.m2m.annotation.Property.*;

/**
 * Describes various parameters and state of the operating system.
 */
@Trait
public final class SystemTrait {
    private SystemTrait() {}

    /** Abstract class for implementing trait behavior on a local thing. */
    @SuppressWarnings("RedundantThrows")
    public abstract static class AbstractLocalTrait extends LocalSystemTrait {
    }

    /** The name of this trait */
    public static final String TRAIT_NAME = "System";

    /** The URI that identifies the specification used to implement this trait. */
    public static final String TRAIT_URI = "tag:google.com,2018:m2m:traits:system:v1:v0#r0";

    /** The Short ID of this trait (<code>"syst"</code>) */
    public static final String TRAIT_ID = "syst";

    /** Flag indicating if this trait supports children or not. */
    public static final boolean TRAIT_SUPPORTS_CHILDREN = false;

    /**
     * Property key for the URI that identifies the specification used to implement this trait. This
     * property is present on all traits.
     */
    @Property(READ_ONLY | REQUIRED)
    public static final PropertyKey<String> META_TRAIT_URI =
            new PropertyKey<>(Section.METADATA, TRAIT_ID, "turi", String.class);

    @Property(READ_ONLY)
    public static final PropertyKey<Double> STAT_LOAD_AVERAGE =
            new PropertyKey<>(Section.STATE, TRAIT_ID, "load", Double.class);

    @Property(READ_ONLY)
    public static final PropertyKey<Integer> META_CPU_COUNT =
            new PropertyKey<>(Section.METADATA, TRAIT_ID, "ncpu", Integer.class);

    @Property(READ_ONLY)
    public static final PropertyKey<String> META_OS_NAME =
            new PropertyKey<>(Section.METADATA, TRAIT_ID, "sysn", String.class);

    @Property(READ_ONLY)
    public static final PropertyKey<String> META_OS_VERSION =
            new PropertyKey<>(Section.METADATA, TRAIT_ID, "sysv", String.class);
}
