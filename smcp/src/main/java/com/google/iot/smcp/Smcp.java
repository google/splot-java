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
package com.google.iot.smcp;

/** Contains SMCP-specific constants */
final class Smcp {

    private Smcp() {}

    /** Interface description for things (w/out version). */
    public static final String IF_DESC_FE = "som.fe";

    /** Interface description for properties. */
    public static final String IF_DESC_PROP = "som.p";

    /** Interface description for sections. */
    public static final String IF_DESC_SECT = "som.s";

    /** Interface description for groups container */
    public static final String IF_DESC_GROUPS = "som.g";

    /** Interface description for group thing */
    public static final String IF_DESC_GROUP_FE = "som.g.fe";

    /** Interface description for things (with version). */
    public static final String IF_DESC_FE_FULL = IF_DESC_FE + "#r0";

    /** Interface description prefix for trait profiles. */
    public static final String IF_DESC_TP_PREFIX = "som.tp.";

    /** Resource type prefix for traits. */
    public static final String RESOURCE_TYPE_PREFIX = "som.t.";

    /** URI path for thing discovery. */
    public static final String DISCOVERY_QUERY_URI = "/.well-known/core?if=" + IF_DESC_FE + "*";
}
