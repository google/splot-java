package com.google.iot.m2m.base;

/**
 * Enumeration constants representing property sections.
 */
public enum Section {
    /**
     * STATE section enum.
     */
    STATE(Splot.SECTION_STATE),

    /**
     * CONFIG section enum.
     */
    CONFIG(Splot.SECTION_CONFIG),

    /**
     * METADATA section enum.
     */
    METADATA(Splot.SECTION_METADATA);

    /**
     * The ID string for the given section.
     */
    public final String id;

    /**
     * Convenience function for looking up the section enum value for a given
     * section ID.
     *
     * @param sectionId the section ID to look up
     * @return the Section enum constant
     * @throws InvalidSectionException if the given section Id is valid.
     */
    static public Section fromId(String sectionId) throws InvalidSectionException {

        switch(sectionId) {
            case Splot.SECTION_STATE:
                return STATE;

            case Splot.SECTION_CONFIG:
                return CONFIG;

            case Splot.SECTION_METADATA:
                return METADATA;
        }
        throw new InvalidSectionException("Invalid Splot section ID \"" + sectionId + "\"");
    }

    Section(String id) {
        this.id = id;
    }
}
