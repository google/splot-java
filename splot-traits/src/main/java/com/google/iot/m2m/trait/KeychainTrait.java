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

import com.google.iot.m2m.annotation.Method;
import com.google.iot.m2m.annotation.Property;
import com.google.iot.m2m.annotation.Trait;
import com.google.iot.m2m.base.*;

/**
 * Trait for managing a cryptographic keychain.
 *
 * @see KeychainItemTrait
 */
@Trait
public final class KeychainTrait {
    // Prevent Instantiation
    private KeychainTrait() {}

    /** Abstract class for implementing trait behavior on a local functional endpoint. */
    public abstract static class AbstractLocalTrait extends LocalKeychainTrait {}

    /** The name of this trait */
    public static final String TRAIT_NAME = "Keychain";

    /** The URI that identifies the specification used to implement this trait. */
    public static final String TRAIT_URI = "tag:google.com,2018:m2m:traits:keychain:v1:v0#r0";

    /** The Short ID of this trait (<code>"keyc"</code>) */
    public static final String TRAIT_ID = "keyc";

    /** Flag indicating if this trait supports children or not. */
    public static final boolean TRAIT_SUPPORTS_CHILDREN = true;

    /**
     * Property key for the URI that identifies the specification used to implement this trait. This
     * property is present on all traits.
     */
    @Property(READ_ONLY | REQUIRED)
    public static final PropertyKey<String> META_TRAIT_URI =
            new PropertyKey<>(Splot.SECTION_METADATA, TRAIT_ID, "turi", String.class);

    /**
     * Method for creating a keychain item.
     *
     * @see #PARAM_IDENTITY
     * @see #PARAM_RECYCLABLE
     * @see #PARAM_OURS
     * @see #PARAM_CERTIFICATE
     * @see #PARAM_SECRET_KEY
     * @see #PARAM_TYPE
     * @see KeychainItemTrait
     */
    @Method(REQUIRED)
    public static final MethodKey<FunctionalEndpoint> METHOD_CREATE =
            new MethodKey<>(TRAIT_ID, "create", FunctionalEndpoint.class);

    /**
     * Identity parameter.
     *
     * @see #METHOD_CREATE
     * @see KeychainItemTrait#CONF_IDENTITY
     */
    public static final ParamKey<String> PARAM_IDENTITY = new ParamKey<>("iden", String.class);

    /**
     * Recyclable flag parameter.
     *
     * @see #METHOD_CREATE
     */
    public static final ParamKey<Boolean> PARAM_RECYCLABLE =
            new ParamKey<>("recy", java.lang.Boolean.class);

    /**
     * Keychain item type parameter.
     *
     * @see #METHOD_CREATE
     * @see KeychainItemTrait#META_TYPE
     */
    public static final ParamKey<Integer> PARAM_TYPE =
            new ParamKey<>("type", java.lang.Integer.class);

    /**
     * "Ours" flag parameter.
     *
     * @see #METHOD_CREATE
     * @see KeychainItemTrait#META_OURS
     */
    public static final ParamKey<Boolean> PARAM_OURS =
            new ParamKey<>("ours", java.lang.Boolean.class);

    /**
     * Certificate parameter.
     *
     * @see #METHOD_CREATE
     * @see KeychainItemTrait#META_CERTIFICATE
     */
    public static final ParamKey<byte[]> PARAM_CERTIFICATE = new ParamKey<>("cert", byte[].class);

    /**
     * Secret key material parameter.
     *
     * @see #METHOD_CREATE
     * @see KeychainItemTrait#META_SECRET_KEY
     */
    public static final ParamKey<byte[]> PARAM_SECRET_KEY = new ParamKey<>("secr", byte[].class);
}
