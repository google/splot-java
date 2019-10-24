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
import com.google.iot.m2m.base.Section;

/** Trait for managing an item in a cryptographic keychain. */
@Trait
public final class KeychainItemTrait {
    // Prevent Instantiation
    private KeychainItemTrait() {}

    /** Abstract class for implementing trait behavior on a local thing. */
    public abstract static class AbstractLocalTrait extends LocalKeychainItemTrait {}

    /** The name of this trait */
    public static final String TRAIT_NAME = "KeychainItem";

    /** The URI that identifies the specification used to implement this trait. */
    public static final String TRAIT_URI = "tag:google.com,2018:m2m:traits:keychain-item:v1:v0#r0";

    /** The Short ID of this trait (<code>"kcit"</code>) */
    public static final String TRAIT_ID = "kcit";

    /** Flag indicating if this trait supports children or not. */
    public static final boolean TRAIT_SUPPORTS_CHILDREN = false;

    /**
     * Property key for the URI that identifies the specification used to implement this trait. This
     * property is present on all traits.
     */
    @Property(READ_ONLY | REQUIRED)
    public static final PropertyKey<String> META_TRAIT_URI =
            new PropertyKey<>(Section.METADATA, TRAIT_ID, "turi", String.class);

    /**
     * The identity associated with this key. This may be absent if the certificate is not
     * associated with an identity.
     */
    @Property(READ_WRITE | REQUIRED)
    public static final PropertyKey<String> CONF_IDENTITY =
            new PropertyKey<>(Section.CONFIG, TRAIT_ID, "iden", java.lang.String.class);

    /**
     * True if this object should be garbage collected when there are no references to the
     * underlying identity.
     */
    @Property
    public static final PropertyKey<Boolean> CONF_RECYCLABLE =
            new PropertyKey<>(
                    Section.CONFIG, TRAIT_ID, "recy", java.lang.Boolean.class);

    /**
     * Specifies what type of key this item contains.
     *
     * <ul>
     *   <li>0 = x.509
     *   <li>1 = password
     *   <li>2 = AES128 key
     * </ul>
     */
    @Property(READ_ONLY | REQUIRED)
    public static final PropertyKey<Integer> META_TYPE =
            new PropertyKey<>(
                    Section.METADATA, TRAIT_ID, "type", java.lang.Integer.class);

    /**
     * If this item is a certificate without a private key, this value is false. Otherwise it is
     * true. This field is only required when the contained key is asymmetric (public/private).
     */
    @Property(READ_ONLY)
    public static final PropertyKey<Boolean> META_OURS =
            new PropertyKey<>(
                    Section.METADATA, TRAIT_ID, "ours", java.lang.Boolean.class);

    /**
     * True if this item is permanent and cannot be deleted. Note that keys that are regenerated
     * after a factory reset for privacy purposes will also have this flag set, since they can only
     * be changed after via a factory reset.
     */
    @Property(READ_ONLY | REQUIRED)
    public static final PropertyKey<Boolean> META_PERMANENT =
            new PropertyKey<>(
                    Section.METADATA, TRAIT_ID, "perm", java.lang.Boolean.class);

    /**
     * If the key for this item is asymmetric, then this property contains the public portion. For
     * example, for X.509 keys this would contain the public certificate. If the underlying key is
     * symmetric, then this property is absent.
     */
    @Property(READ_ONLY)
    public static final PropertyKey<byte[]> META_CERTIFICATE =
            new PropertyKey<>(Section.METADATA, TRAIT_ID, "cert", byte[].class);

    /**
     * This property represents the actual secret data associated with this item. It is only used
     * when adding new items into the keychain and MUST NOT be present when reading.
     */
    @Property(WRITE_ONLY)
    public static final PropertyKey<byte[]> META_SECRET_KEY =
            new PropertyKey<>(Section.METADATA, TRAIT_ID, "secr", byte[].class);

    /** True if the secret/private data associated with this item is on a secure element. */
    @Property(READ_ONLY | REQUIRED)
    public static final PropertyKey<Boolean> META_HARDWARE_BACKED =
            new PropertyKey<>(
                    Section.METADATA, TRAIT_ID, "hard", java.lang.Boolean.class);

    /**
     * If this item is a certificate, this property contains the SHA256 hash of the certificate.
     * Otherwise it is absent.
     */
    @Property(READ_ONLY | REQUIRED)
    public static final PropertyKey<byte[]> META_HASH_SHA256 =
            new PropertyKey<>(Section.METADATA, TRAIT_ID, "sha2", byte[].class);

    /**
     * Contains one (of <i>m</i>) shares of the secret. It can only be read by someone who has
     * authenticated with the “init” identity. It allows someone to reconstruct the administrative
     * credentials of the network without requiring a factory reset of every device. This allows an
     * administrator to reconstruct their credential by only physically interacting with a subset of
     * the devices in the administrative domain.
     *
     * <p>This property can only be read using the "init" identity.
     */
    @Property
    public static final PropertyKey<byte[]> META_SECRET_SHARE =
            new PropertyKey<>(Section.METADATA, TRAIT_ID, "sssh", byte[].class);

    /**
     * This property contains the version of the secret share. This is typically incremented every
     * time the secret shares are recalculated. Only secret shares from the same version can be
     * combined to reconstruct the secret.
     *
     * <p>This property can only be read using the "init" identity.
     */
    @Property
    public static final PropertyKey<Integer> META_SECRET_SHARE_VER =
            new PropertyKey<>(
                    Section.METADATA, TRAIT_ID, "sssv", java.lang.Integer.class);
}
