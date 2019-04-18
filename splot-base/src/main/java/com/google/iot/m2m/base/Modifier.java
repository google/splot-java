/*
 * Copyright (C) 2019 Google Inc.
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
package com.google.iot.m2m.base;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.ArrayList;

/**
 * Class for specifying additional context for operations on properties.
 *
 * <p>Instances of this class are generally passed as a variable list of arguments
 * to property-related functions on a {@link FunctionalEndpoint}, like
 * {@link FunctionalEndpoint#setProperty} or
 * {@link FunctionalEndpoint#fetchSection}. They provide a
 * mechanism for indicating things like the intent to read the transition target
 * value rather than the immediate value, or to indicate that a given change should
 * transition to the target value over a period of time.
 *
 * @see FunctionalEndpoint#setProperty
 * @see FunctionalEndpoint#fetchProperty
 * @see FunctionalEndpoint#fetchSection
 * @see FunctionalEndpoint#incrementProperty
 * @see FunctionalEndpoint#toggleProperty
 * @see FunctionalEndpoint#insertValueIntoProperty
 * @see FunctionalEndpoint#removeValueFromProperty
 */
public abstract class Modifier {
    /**
     * Constant value representing an empty modifier list.
     */
    public static final Modifier[] EMPTY_LIST = new Modifier[0];

    /**
     * Converts the modifier list into a URI query string.
     *
     * @param modifiers the modifier list to convert to a query string
     * @return the query string representing the modifiers
     */
    public static String convertToQuery(Modifier ... modifiers) {

        StringBuilder ret = new StringBuilder();
        boolean first = true;

        for (Modifier mod : modifiers) {
            if (first) {
                first = false;
            } else {
                ret.append("&");
            }
            ret.append(mod.toString());
        }
        return ret.toString();
    }

    /**
     * Converts a URI query string into an array of {@link Modifier modifiers}.
     * @param query The URI query string to convert. May be null
     * @return The list of modifiers
     * @throws InvalidModifierListException if one of the query components fails
     *         to parse correctly.
     */
    public static Modifier[] convertFromQuery(@Nullable String query)
            throws InvalidModifierListException {
        if (query == null || "".equals(query)) {
            return EMPTY_LIST;
        }

        ArrayList<Modifier> ret = new ArrayList<>();
        String[] queryComponents = query.split("[&;]");

        for (String key : queryComponents) {
            String value = null;

            if (key.contains("=")) {
                value = key.substring(key.indexOf('=') + 1);
                key = key.substring(0, key.indexOf('='));
            }

            switch (key) {
                case "tt":
                    ret.add(transitionTarget());
                    break;

                case "all":
                    ret.add(all());
                    break;

                case Splot.PARAM_DURATION:
                    {
                        double seconds = 0;
                        if (value != null) {
                            try {
                                seconds = Double.valueOf(value);

                                if (seconds <= 0) {
                                    seconds = 0;
                                }

                                ret.add(duration(seconds));

                            } catch (IllegalArgumentException x) {
                                throw new InvalidModifierListException(
                                        "Bad duration \"" + value + "\"", x);
                            }
                        }
                    }
                    break;
            }
        }

        return ret.toArray(EMPTY_LIST);
    }

    /**
     * Modifier class indicating a transition duration for changes to properties in
     * {@link Section#STATE}.
     *
     * <p>This modifier will only have an effect if the target {@link FunctionalEndpoint} supports
     * {@link com.google.iot.m2m.trait.TransitionTrait transitions}.
     *
     * <p>Even when transitions are supported, not all properties in {@link Section#STATE}
     * necessarily support transitions. See the documentation for the FunctionalEndpoint for
     * more information.</p>
     *
     * <p>If the duration is set to zero, then this modifier also has the same behavior
     * as {@link TransitionTarget}.
     *
     * @see com.google.iot.m2m.trait.TransitionTrait
     * @see Section#STATE
     * @see FunctionalEndpoint#setProperty
     * @see FunctionalEndpoint#incrementProperty
     * @see FunctionalEndpoint#toggleProperty
     * @see FunctionalEndpoint#insertValueIntoProperty
     * @see FunctionalEndpoint#removeValueFromProperty
     */
    public static class Duration extends Modifier {
        double mValue;

        Duration(double v) {
            if (v <= 0) {
                throw new IllegalArgumentException("Duration cannot be less than zero");
            }
            mValue = v;
        }

        public double getDuration() {
            return mValue;
        }

        @Override
        public int hashCode() {
            return toString().hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            return obj != null
                    && getClass().equals(obj.getClass())
                    && obj.toString().equals(toString());
        }

        @Override
        public String toString() {
            if (mValue == 0) {
                return "d=0";
            }

            return String.format("d=%.2f", mValue);
        }
    }

    /**
     * Modifier class indicating that the transition target value is desired,
     * rather than the immediate value.
     *
     * <p>This modifier will have no effect if...
     *
     * <ul>
     *     <li>the target {@link FunctionalEndpoint} doesn't support
     *     {@link com.google.iot.m2m.trait.TransitionTrait transitions}.</li>
     *     <li>the fetch operation isn't to {@link Section#STATE}.</li>
     * </ul>
     *
     * @see com.google.iot.m2m.trait.TransitionTrait
     * @see FunctionalEndpoint#fetchProperty
     * @see FunctionalEndpoint#fetchSection
     * @see Section#STATE
     */
    public static class TransitionTarget extends Modifier {
        @Override
        public String toString() {
            return "tt";
        }

        @Override
        public int hashCode() {
            return toString().hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            return obj != null
                    && getClass().equals(obj.getClass());
        }
    }

    /**
     * Modifier class indicating that properties with {@code null} values should
     * be included in the section results.
     *
     * @see FunctionalEndpoint#fetchSection
     */
    public static class All extends Modifier {
        @Override
        public String toString() {
            return "all";
        }

        @Override
        public int hashCode() {
            return toString().hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            return obj != null
                    && getClass().equals(obj.getClass());
        }
    }

    /**
     * Convenience method for creating a {@link Duration} modifier.
     * @param seconds the number of seconds for the transition to last
     * @return a new {@link Duration} modifier
     */
    public static Duration duration(double seconds) {
        return new Duration(seconds);
    }

    /**
     * Convenience method for creating a {@link TransitionTarget} modifier.
     * @return a new {@link TransitionTarget} modifier
     */
    public static TransitionTarget transitionTarget() {
        return new TransitionTarget();
    }

    /**
     * Convenience method for creating a {@link All} modifier.
     * @return a new {@link All} modifier
     */
    public static All all() {
        return new All();
    }
}
