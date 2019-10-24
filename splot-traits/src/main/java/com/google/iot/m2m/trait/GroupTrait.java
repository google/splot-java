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
import com.google.iot.m2m.base.ParamKey;
import com.google.iot.m2m.base.PropertyKey;
import com.google.iot.m2m.base.Section;

/** Trait used by group things. */
@Trait
public final class GroupTrait {
    // Prevent instantiation
    private GroupTrait() {}

    /** Abstract class for implementing trait behavior on a local thing. */
    public abstract static class AbstractLocalTrait extends LocalGroupTrait {}

    /** The name of this trait */
    public static final String TRAIT_NAME = "Group";

    /** The URI that identifies the specification used to implement this trait. */
    public static final String TRAIT_URI = "tag:google.com,2018:m2m:traits:group:v1:v0#r0";

    /** The Short ID of this trait (<code>"grup"</code>) */
    public static final String TRAIT_ID = "grup";

    /** Flag indicating if this trait supports children or not. */
    public static final boolean TRAIT_SUPPORTS_CHILDREN = false;

    /**
     * Property key for the URI that identifies the specification used to implement this trait. This
     * property is present on all traits.
     */
    @Property(READ_ONLY | REQUIRED)
    public static final PropertyKey<String> META_TRAIT_URI =
            new PropertyKey<>(Section.METADATA, TRAIT_ID, "turi", String.class);

    @Property(READ_WRITE | GET_REQUIRED)
    public static final PropertyKey<String[]> CONF_LOCAL_MEMBERS =
            new PropertyKey<>(
                    Section.CONFIG, TRAIT_ID, "mbrl", java.lang.String[].class);

    @Property()
    public static final PropertyKey<String[]> CONF_REMOTE_MEMBERS =
            new PropertyKey<>(
                    Section.CONFIG, TRAIT_ID, "mbrr", java.lang.String[].class);

    @Property
    public static final PropertyKey<String> CONF_GROUP_ADDRESS =
            new PropertyKey<>(
                    Section.CONFIG, TRAIT_ID, "addr", java.lang.String.class);

    /** Method parameter key for Group ID. */
    public static final ParamKey<String> PARAM_GROUP_ID =
            new ParamKey<>("gid", java.lang.String.class);
}
