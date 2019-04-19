package com.google.iot.m2m.base;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Enum for specifying the operator acting on a property.
 */
public enum Operation {
    UNSPECIFIED(""),
    INCREMENT(Splot.PROP_METHOD_INCREMENT),
    TOGGLE(Splot.PROP_METHOD_TOGGLE),
    INSERT(Splot.PROP_METHOD_INSERT),
    REMOVE(Splot.PROP_METHOD_REMOVE);

    public final String id;

    Operation(String id) {
        this.id = id;
    }

    public static Operation fromId(String id) {
        switch (id) {
            case Splot.PROP_METHOD_INCREMENT:
                return INCREMENT;
            case Splot.PROP_METHOD_TOGGLE:
                return TOGGLE;
            case Splot.PROP_METHOD_INSERT:
                return INSERT;
            case Splot.PROP_METHOD_REMOVE:
                return REMOVE;
            default:
                return UNSPECIFIED;
        }
    }

    /**
     * Extracts the {@link Operation} from a URI query string
     * @param query The URI query string. May be null
     * @return The contained {@link Operation}
     */
    public static Operation fromQuery(@Nullable String query) {
        if (query == null || "".equals(query)) {
            return UNSPECIFIED;
        }

        String[] queryComponents = query.split("[&;]");

        for (String component : queryComponents) {
            Operation op = fromId(component);

            if (op.equals(UNSPECIFIED)) {
                continue;
            }

            return op;
        }

        return UNSPECIFIED;
    }
}
