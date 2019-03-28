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
import static com.google.iot.m2m.base.Splot.SECTION_METADATA;
import static com.google.iot.m2m.base.Splot.SECTION_STATE;

import com.google.iot.m2m.annotation.Property;
import com.google.iot.m2m.annotation.Trait;
import com.google.iot.m2m.base.*;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * The base trait is the one trait which is required on all Functional Endpoints. It contains
 * information about the model, manufacturer, and identifier, as well as administratively
 * configurable properties like the administrative name, administrative id, and the hidden flag.
 */
@Trait
public final class BaseTrait {
    // Prevent instantiation
    private BaseTrait() {}

    /**
     * Abstract class used to implement @{link BaseTrait} behavior on a local functional endpoint.
     */
    @SuppressWarnings("RedundantThrows")
    public abstract static class AbstractLocalTrait extends LocalBaseTrait {
        private String mName = null;
        private String mUid = null;
        private boolean mHidden = false;

        @Override
        public String onGetUid() {
            if (mUid == null) {
                mUid = FunctionalEndpoint.generateNewUid();
            }
            return mUid;
        }

        @Override
        public void onSetUid(@Nullable String uid)
                throws PropertyReadOnlyException, InvalidPropertyValueException {
            if (!Objects.equals(mUid, uid)) {
                mUid = uid;
                didChangeUid(mUid);
            }
        }

        /** Override this method to set the default value for {@link BaseTrait#META_NAME}. */
        public String onGetNameDefault() {
            try {
                Map<String, String> productName = onGetProductName();

                if (productName != null && !productName.isEmpty()) {
                    String lang = Locale.getDefault().getLanguage();
                    for (Map.Entry<String, String> entry : productName.entrySet()) {
                        if (lang.equals(new Locale(entry.getKey()).getLanguage())) {
                            return entry.getValue();
                        }
                    }
                    lang = Locale.ENGLISH.getLanguage();
                    for (Map.Entry<String, String> entry : productName.entrySet()) {
                        if (lang.equals(new Locale(entry.getKey()).getLanguage())) {
                            return entry.getValue();
                        }
                    }
                    return productName.values().iterator().next();
                }
            } catch (TechnologyException ignored) {
            }

            return "Unknown";
        }

        @Override
        public String onGetName() {
            if (mName == null) {
                mName = onGetNameDefault();
            }

            return mName;
        }

        @Override
        public void onSetName(@Nullable String value)
                throws PropertyReadOnlyException, InvalidPropertyValueException {
            if (!Objects.equals(mName, value)) {
                mName = value;
                didChangeName(mName);
            }
        }

        @Override
        public Boolean onGetHidden() {
            return mHidden;
        }

        @Override
        public void onSetHidden(@Nullable Boolean value)
                throws PropertyReadOnlyException, InvalidPropertyValueException {
            if (value != null && value != mHidden) {
                mHidden = value;
                didChangeHidden(mHidden);
            }
        }
    }

    /** The name of this trait */
    public static final String TRAIT_NAME = "Base";

    /** The URI that identifies the specification used to implement this trait. */
    public static final String TRAIT_URI = "tag:google.com,2018:m2m:traits:base:v1:v0#r0";

    /** The Short ID of this trait */
    public static final String TRAIT_ID = "base";

    /** Flag indicating if this trait supports children or not. */
    public static final boolean TRAIT_SUPPORTS_CHILDREN = false;

    /**
     * Property key for the URI that identifies the specification used to implement this trait.
     *
     * <p>This property is present on all traits.
     */
    @Property(READ_ONLY | REQUIRED)
    public static final PropertyKey<String> META_TRAIT_URI =
            new PropertyKey<>(SECTION_METADATA, TRAIT_ID, "turi", String.class);

    /**
     * Administrative, human-readable name of the functional endpoint.
     *
     * <p>After a factory reset, this is set to a descriptive default value by the manufacturer.
     * This value does not need to be unique.
     */
    @Property(READ_WRITE | REQUIRED)
    public static final PropertyKey<String> META_NAME =
            new PropertyKey<>(SECTION_METADATA, TRAIT_ID, "name", java.lang.String.class);

    /**
     * Administrative unique identifier of the FE.
     *
     * <p>Identifies the function of the FE. Rules may reference this identifier instead of a direct
     * path in order to make replacement easier. After a factory reset, the value of this field is
     * set to a random UID. This value must be unique.
     */
    @Property(READ_WRITE | REQUIRED)
    public static final PropertyKey<String> META_UID =
            new PropertyKey<>(SECTION_METADATA, TRAIT_ID, "uid", java.lang.String.class);

    /**
     * The localized names of the product.
     *
     * <p>This property is a dictionary containing the localized names of the product in at least
     * one language. The key to the dictionary is the locale code (like "en" or "jp"), and the value
     * is the localized name for that locale.
     */
    @SuppressWarnings("unchecked")
    @Property(CONSTANT)
    public static final PropertyKey<Map<String, String>> META_PRODUCT_NAME =
            new PropertyKey(SECTION_METADATA, TRAIT_ID, "prod", java.util.Map.class);

    /**
     * The model identifier for this functional endpoint, unique to the manufacturer.
     *
     * <p>It identifies the specific model of the device hosting this FE. Note that this field is
     * not for the marketing name: use ‘prod’ for that.
     */
    @Property(CONSTANT)
    public static final PropertyKey<String> META_MODEL =
            new PropertyKey<>(SECTION_METADATA, TRAIT_ID, "modl", java.lang.String.class);

    /**
     * Manufacturer name.
     *
     * <p>Unique to the manufacturer. This property identifies the specific model of the device
     * hosting this FE. Note that this field is not for the marketing name: use ‘prod’ for that.
     */
    @Property(CONSTANT)
    public static final PropertyKey<String> META_MANUFACTURER =
            new PropertyKey<>(SECTION_METADATA, TRAIT_ID, "mfgr", java.lang.String.class);

    /**
     * Software version description string.
     *
     * <p>This is often the version of the software running on the device, but in the case of a
     * bridge may differ.
     */
    @Property(CONSTANT)
    public static final PropertyKey<String> META_SW_VERSION =
            new PropertyKey<>(SECTION_METADATA, TRAIT_ID, "sver", java.lang.String.class);

    /**
     * Manufacturer unique-identifier/serial-number.
     *
     * <p>This is typically the FE index appended to the serial number of the device. The presence
     * of this field is optional and may be omitted for privacy purposes.
     */
    @Property(CONSTANT)
    public static final PropertyKey<String> META_SERIAL =
            new PropertyKey<>(SECTION_METADATA, TRAIT_ID, "seri", java.lang.String.class);

    /**
     * Supported trait profiles.
     *
     * <p>This property identifies which trait profiles this functional endpoint implements. Trait
     * profiles define what the minimum implementation requirements are for specific types of
     * functionality, such as lights. The first listed profile is intended to best describe the
     * functionality of the FE, with subordinate profiles listed subsequently.
     */
    @Property(CONSTANT)
    public static final PropertyKey<String[]> META_TRAIT_PROFILES =
            new PropertyKey<>(SECTION_METADATA, TRAIT_ID, "prof", java.lang.String[].class);

    /**
     * Hidden flag.
     *
     * <p>This property is a simple boolean flag indicating if this FE should be hidden from
     * administrative views.
     */
    @Property(READ_WRITE | REQUIRED)
    public static final PropertyKey<Boolean> META_HIDDEN =
            new PropertyKey<>(SECTION_METADATA, TRAIT_ID, "hide", java.lang.Boolean.class);

    /**
     * Permanent flag.
     *
     * <p>This property indicates if this FE is a permanent fixture on this device or if it can be
     * administratively created or deleted.
     */
    @Property(CONSTANT)
    public static final PropertyKey<Boolean> META_PERMANENT =
            new PropertyKey<>(SECTION_METADATA, TRAIT_ID, "perm", java.lang.Boolean.class);

    /**
     * Current trap condition.
     *
     * <p>When not null, contains a string indicating the current error/trap condition. This
     * property will automatically revert to null when the error/trap condition has been cleared,
     * which depends on the type of device. This would be used to indicate things like power
     * overload, door obstructed, manual override, or battery too low.
     */
    @Property(READ_ONLY | NO_SAVE)
    public static final PropertyKey<String> STAT_TRAP =
            new PropertyKey<>(SECTION_STATE, TRAIT_ID, "trap", java.lang.String.class);
}
