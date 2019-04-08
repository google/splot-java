package com.google.iot.m2m.base;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Splot-related constants and utility functions.
 */
public class Splot {
    /**
     * "Directory" ID for state section.
     *
     * <p>The value of this constant is identical to {@code Section.STATE.id}.
     *
     * @see Section#STATE
     */
    public static final String SECTION_STATE = "s";

    /**
     * "Directory" ID for config section.
     *
     * <p>The value of this constant is identical to {@code Section.CONFIG.id}.
     *
     * @see Section#CONFIG
     */
    public static final String SECTION_CONFIG = "c";

    /**
     * "Directory" ID for metadata section.
     *
     * <p>The value of this constant is identical to {@code Section.METADATA.id}.
     *
     * @see Section#METADATA
     */
    public static final String SECTION_METADATA = "m";

    /** "Directory" ID for func section. */
    public static final String SECTION_FUNC = "f";

    /** Short ID for the group resource. Used in URIs. */
    public static final String GROUP_RESOURCE = "g";

    /**
     * Short ID for the toggle modifier. Used in URIs.
     * @see Modifier.Toggle
     * @see Modifier#toggle()
     */
    public static final String PROP_METHOD_TOGGLE = "tog";

    /**
     * Short ID for the increment modifier. Used in URIs.
     * @see Modifier.Increment
     * @see Modifier#increment()
     */
    public static final String PROP_METHOD_INCREMENT = "inc";

    /**
     * Short ID for the insert modifier. Used in URIs.
     * @see Modifier.Insert
     * @see Modifier#insert()
     */
    public static final String PROP_METHOD_INSERT = "ins";

    /**
     * Short ID for the remove modifier. Used in URIs.
     * @see Modifier.Remove
     * @see Modifier#remove()
     */
    public static final String PROP_METHOD_REMOVE = "rem";

    /**
     * Short ID for the duration modifier. Used in URIs.
     * @see Modifier.Duration
     * @see Modifier#duration(double)
     */
    public static final String PARAM_DURATION = "d";

    /**
     * {@hide} Generates a new 10-character random UID to be used with {@link
     * com.google.iot.m2m.trait.BaseTrait#META_UID}.
     */
    public static String generateNewUid() {
        String uid;
        SecureRandom random = new SecureRandom();

        /* This loop just keeps calculating random base-64 strings
         * until it finds one that doesn't include "+" or "/".
         */
        do {
            byte[] bytes = new byte[8];
            random.nextBytes(bytes);
            uid = Base64.getEncoder().encodeToString(bytes).substring(0, 10);
        } while (uid.contains("+") || uid.contains("/"));

        return uid;
    }

}
