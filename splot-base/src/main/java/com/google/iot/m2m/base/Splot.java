package com.google.iot.m2m.base;

/**
 * Splot-related constants.
 */
public class Splot {
    public enum Section {
        STATE(SECTION_STATE),
        CONFIG(SECTION_CONFIG),
        METADATA(SECTION_METADATA);

        public final String name;

        static public Section fromName(String name) throws InvalidSectionException {
            switch(name) {
                case SECTION_STATE:
                    return STATE;
                case SECTION_CONFIG:
                    return CONFIG;
                case SECTION_METADATA:
                    return METADATA;
            }
            throw new InvalidSectionException("Invalid Splot section \"" + name + "\"");
        }

        Section(String name) {
            this.name = name;
        }
    }

    /** "Directory" name for state section. */
    public static final String SECTION_STATE = "s";

    /** "Directory" name for config section. */
    public static final String SECTION_CONFIG = "c";

    /** "Directory" name for metadata section. */
    public static final String SECTION_METADATA = "m";

    public static final String SECTION_FUNC = "f";

    public static final String GROUP_RESOURCE = "g";

    public static final String PROP_METHOD_TOGGLE = "tog";
    public static final String PROP_METHOD_INCREMENT = "inc";
    public static final String PROP_METHOD_INSERT = "ins";
    public static final String PROP_METHOD_REMOVE = "rem";

    public static final String PARAM_DURATION = "d";
}
