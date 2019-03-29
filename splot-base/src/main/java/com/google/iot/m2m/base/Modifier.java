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

public abstract class Modifier {
    public static final Modifier[] EMPTY_LIST = new Modifier[0];

    @Nullable
    public static Mutation getMutation(Modifier ... modifiers) throws InvalidModifierListException {
        Mutation mutation = null;

        for (Modifier mod : modifiers) {
            if (mod instanceof Mutation) {
                if (mutation == null) {
                    mutation = (Mutation)mod;
                } else {
                    throw new InvalidModifierListException(
                            "Modifier list has more than one mutator");
                }
            }
        }

        return mutation;
    }

    public static String convertToQuery(Modifier ... modifiers)
            throws InvalidModifierListException {
        Mutation mutation = getMutation(modifiers);

        StringBuilder ret = new StringBuilder();
        boolean first = true;

        if (mutation != null) {
            ret.append(mutation.toString());
            first = false;
        }

        for (Modifier mod : modifiers) {
            if (!(mod instanceof Mutation)) {
                if (!first) {
                    ret.append("&");
                }
                ret.append(mod.toString());
            }
        }
        return ret.toString();
    }

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
                case Splot.PROP_METHOD_INCREMENT:
                    ret.add(increment());
                    break;

                case Splot.PROP_METHOD_TOGGLE:
                    ret.add(toggle());
                    break;

                case Splot.PROP_METHOD_INSERT:
                    ret.add(insert());
                    break;

                case Splot.PROP_METHOD_REMOVE:
                    ret.add(remove());
                    break;

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
                            } catch (NumberFormatException x) {
                                throw new InvalidModifierListException(
                                        "Bad duration \"" + value + "\"");
                            }
                            if (seconds <= 0) {
                                seconds = 0;
                            }
                        }
                        ret.add(duration(seconds));
                    }
                    break;
            }
        }

        return ret.toArray(EMPTY_LIST);
    }

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

    public static abstract class Mutation extends Modifier {
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

    public static class Increment extends Mutation {
        @Override
        public String toString() {
            return Splot.PROP_METHOD_INCREMENT;
        }
    }

    public static class Toggle extends Mutation {
        @Override
        public String toString() {
            return Splot.PROP_METHOD_TOGGLE;
        }
    }

    public static class Insert extends Mutation {
        @Override
        public String toString() {
            return Splot.PROP_METHOD_INSERT;
        }
    }

    public static class Remove extends Mutation {
        @Override
        public String toString() {
            return Splot.PROP_METHOD_REMOVE;
        }
    }

    public static Duration duration(double seconds) {
        return new Duration(seconds);
    }

    public static TransitionTarget transitionTarget() {
        return new TransitionTarget();
    }

    public static All all() {
        return new All();
    }

    public static Insert insert() {
        return new Insert();
    }

    public static Remove remove() {
        return new Remove();
    }

    public static Increment increment() {
        return new Increment();
    }

    public static Toggle toggle() {
        return new Toggle();
    }
}
