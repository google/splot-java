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
package com.google.iot.m2m.local.rpn;

import com.google.iot.m2m.base.InvalidValueException;
import com.google.iot.m2m.base.TypeConverter;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.ref.WeakReference;
import java.util.*;

/**
 * Variable context for forth-like RPN expression evaluator.
 *
 * <h3>Mathematical operators:</h3>
 * <ul>
 *     <li>{@code +}: Adds the last two items on the stack.
 *     <li>{@code -}: Subtracts the last item on the stack from the item before it.
 *     <li>{@code *}: Multiplies the last two items on the stack.
 *     <li>{@code /}: Divides the second to last item on the stack by the last item on the stack.
 *     <li>{@code %}: <small>(x y -> x%y)</small> Modulus operator, using floored division.
 *                    Note that this is different than what C and Java use (truncated division).
 *     <li>{@code ^}: <small>(x y -> x^y)</small> Power operator.
 *     <li>{@code LOG}: <small>(x -> ln(x))</small> Natural logarithm
 *     <li>{@code NEG}: <small>(x -> -x)</small> Negate.
 *     <li>{@code ABS}: <small>(x -> |x|)</small> Absolute value
 *     <li>{@code MIN}: <small>(x y -> min)</small> Returns the smaller of two values.
 *     <li>{@code MAX}: <small>(x y -> max)</small> Returns the larger of two values.
 *     <li>{@code ROUND}: Rounds the number to the nearest whole number.
 *     <li>{@code FLOOR}: Discards any fractional part of the number.
 *     <li>{@code CEIL}: If the number has a fractional component, return the next largest integer.
 *     <li>{@code SIN}: <small>(turns -> sin(turns*tau))</small>
 *     <li>{@code COS}: <small>(turns -> cos(turns*tau))</small>
 *     <li>{@code ASIN}: <small>(x -> asin(x)/tau)</small>
 *     <li>{@code ACOS}: <small>(x -> acos(x)/tau)</small>
 *     <li>{@code RANGE}: <small>(value, min, max -> boolean)</small> Determines if the given value is between min and max.
 *     <li>{@code CLAMP}: <small>(value, min, max -> clampedValue)</small>
 *     <li>{@code POLY2}: <small>(x, a, b, c -> y)</small> Second-degree polynomial
 *     <li>{@code POLY3}: <small>(x, a, b, c, d -> y)</small> Third-degree polynomial
 *     <li>{@code RND}: <small>(-> n)</small> Returns a random number between 0.0 and 1.0.
 * </ul>
 *
 * <h3>Comparison Operations:</h3>
 * <ul>
 *     <li>{@code ==}: General equality. Will massage types to favor returning true.
 *     <li>{@code !=}: Inverse of {@code ==}.
 *     <li>{@code <}: Less than
 *     <li>{@code <=}: Less than or equal to
 *     <li>{@code >}: Greater than
 *     <li>{@code >=}: Greater than or equal to
 *     <li>{@code ===}: Absolute equality. Only returns true if the operands are fundamentally identical.
 * </ul>
 *
 * <h3>Boolean Logic Operations:</h3>
 * <ul>
 *     <li>{@code &&}: Logical "and" operator.
 *     <li>{@code ||}: Logical "or" operator.
 *     <li>{@code XOR}: Logical "xor" operator.
 *     <li>{@code !}: Logical "not" operator
 * </ul>
 *
 * <h3>Stack Operations:</h3>
 * <ul>
 *     <li>{@code DUP}: <small>(x -> x, x)</small> Duplicates the last item on the stack.
 *     <li>{@code SWAP}: <small>(x, y -> y, x)</small> Swaps the last two items on the stack.
 *     <li>{@code DROP}: <small>(x ->)</small> Removes the last item on the stack.
 *     <li>{@code OVER}: <small>(x, y -> x, y, x)</small>
 *     <li>{@code ROT}: <small>(a, b, c -> b, c, a)</small>
 * </ul>
 *
 * <h3>Map Operations:</h3>
 * <ul>
 *     <li>{@code {}}: Pushes an empty map onto the stack.
 *     <li>{@code GET}: <small>(dict, key -> dict, value)</small>
 *     <li>{@code PUT}: <small>(dict, value, key -> dict)</small>
 * </ul>
 *
 * <h3>Array Operations:</h3>
 * <ul>
 *     <li>{@code POP}: <small>(array -> newArray, value)</small> Removes the last item from an array.
 *     <li>{@code PUSH}: <small>(array, value -> newArray)</small> Appends a value to the end of an array.
 *     <li>{@code []}: Pushes an empty array onto the stack.
 *     <li>{@code [1]}: Pops one item off the stack and pushes a new array with that item.
 *     <li>{@code [2]}: Pops two items off the stack and pushes a new array with those items.
 *     <li>{@code [3]}: Pops three items off the stack and pushes a new array with those items.
 *     <li>{@code [4]}: Pops four items off the stack and pushes a new array with those items.
 * </ul>
 *
 * <h3>Constants:</h3>
 * <ul>
 *     <li>{@code PI}: ~3.14
 *     <li>{@code TAU}: ~6.28
 *     <li>{@code E}: ~2.7
 *     <li>{@code TRUE}: 1.0
 *     <li>{@code FALSE}: 0.0
 *     <li>{@code STOP}:
 *     <li>{@code NULL}:
 * </ul>
 *
 * <h3>Branches/Loops:</h3>
 * <ul>
 *     <li>{@code IF ENDIF}:
 *     <li>{@code IF ELSE ENDIF}:
 *     <li>{@code CASE OF ENDOF ENDCASE}:
 *     <li>{@code DO LOOP}:
 * </ul>
 *
 * <h3>Examples:</h3>
 *
 * <p>{@code "POP 0.1858 - SWAP POP 0.3320 - SWAP DROP SWAP / -449 3525 -6823.3 5520.33 POLY3"}
 * would convert CIE xy chromaticity coordinates in an array into an approximate correlated
 * color temperature in K. The input would be the CIE xy coordinates in a two-element array.
 *
 * <p>{@code "0 1 CLAMP DUP 1 SWAP - 2 / SWAP 2 / 0 [3]"}
 * would convert a number between 0.0 and 1.0 into an sRGB triplet with 0.0 being red and 1.0 being
 * green. Values in between would be a linear combination of the two. Inputs outside of the range
 * of zero to one are clamped to either zero or one, whichever is closer.
 */
public class RPNContext {
    static private final double EPSILON = 0.0000000001;
    private static final String REGEX_NUMBER = "^[-+]?[0-9]+([.][0-9]+)?";
    private static final String REGEX_STRING = "^:.*";
    private static final String REGEX_VARIABLE = "^[a-zA-Z_][a-zA-Z0-9_.:]*$";

    enum TokenType {
        CONST_NUMBER,
        CONST_STRING,
        OPERATOR,
        BRANCH,
        VARIABLE,
        INVALID
    }

    public static final Object STOP = new Object();
    private static final TypeConverter<Double> doubleType = TypeConverter.DOUBLE;
    private static final TypeConverter<Integer> integerType = new TypeConverter<>(Integer.class);
    private static final TypeConverter<Boolean> booleanType = TypeConverter.BOOLEAN;
    private static final TypeConverter<String> stringType = TypeConverter.STRING;

    static private final Map<String,RPNOperation> mOperators = new HashMap<>();
    static private final Set<String> mBranchOperators = new HashSet<>();

    @SuppressWarnings("unchecked")
    private static final TypeConverter<List<Object>> listType = new TypeConverter(List.class);

    @SuppressWarnings("unchecked")
    private static final TypeConverter<Map<String,Object>> dictType = new TypeConverter(Map.class);

    private static final Random mRng = new Random();

    private static boolean objectEquals(@Nullable Object lhs, @Nullable Object rhs) {
        if (Objects.equals(lhs, rhs)) {
            return true;
        } else if (rhs instanceof Number || lhs instanceof Number) {
            try {
                double rhsd = doubleType.coerceNonNull(rhs);
                double lhsd = doubleType.coerceNonNull(lhs);
                return Math.abs(lhsd-rhsd) < EPSILON;
            } catch (InvalidValueException x) {
                return false;
            }
        } else {
            return false;
        }
    }

    static {
        mBranchOperators.add("IF");
        mBranchOperators.add("ELSE");
        mBranchOperators.add("ENDIF");

        mBranchOperators.add("DO");
        mBranchOperators.add("LOOP");

        mBranchOperators.add("CASE");
        mBranchOperators.add("OF");
        mBranchOperators.add("ENDOF");
        mBranchOperators.add("ENDCASE");

        mOperators.put("+", (context, stack) -> {
            double rhs = doubleType.coerceNonNull(stack.pop());
            double lhs = doubleType.coerceNonNull(stack.pop());
            stack.push(lhs + rhs);
        });

        mOperators.put("-", (context, stack) -> {
            double rhs = doubleType.coerceNonNull(stack.pop());
            double lhs = doubleType.coerceNonNull(stack.pop());
            stack.push(lhs - rhs);
        });

        mOperators.put("*", (context, stack) -> {
            double rhs = doubleType.coerceNonNull(stack.pop());
            double lhs = doubleType.coerceNonNull(stack.pop());
            stack.push(lhs * rhs);
        });

        mOperators.put("/", (context, stack) -> {
            double rhs = doubleType.coerceNonNull(stack.pop());
            double lhs = doubleType.coerceNonNull(stack.pop());
            stack.push(lhs / rhs);
        });

        mOperators.put("^", (context, stack) -> {
            double rhs = doubleType.coerceNonNull(stack.pop());
            double lhs = doubleType.coerceNonNull(stack.pop());
            stack.push(Math.pow(lhs, rhs));
        });

        mOperators.put("%", (context, stack) -> {
            double div = doubleType.coerceNonNull(stack.pop());
            double num = doubleType.coerceNonNull(stack.pop());
            double r = num - div*Math.floor(num/div);
            stack.push(r);
        });

        mOperators.put("LOG", (context, stack) -> {
            double arg = doubleType.coerceNonNull(stack.pop());
            stack.push(Math.log(arg));
        });

        mOperators.put("NEG", (context, stack) -> {
            double arg = doubleType.coerceNonNull(stack.pop());
            stack.push(-arg);
        });

        mOperators.put("ABS", (context, stack) -> {
            double arg = doubleType.coerceNonNull(stack.pop());
            stack.push(Math.abs(arg));
        });

        mOperators.put("<", (context, stack) -> {
            Object rhsObj = stack.pop();
            Object lhsObj = stack.pop();
            try {
                double rhs = doubleType.coerceNonNull(rhsObj);
                double lhs = doubleType.coerceNonNull(lhsObj);
                stack.push(lhs < rhs);

            } catch (InvalidValueException ignore) {
                stack.push(false);
            }
        });

        mOperators.put("<=", (context, stack) -> {
            Object rhsObj = stack.pop();
            Object lhsObj = stack.pop();
            try {
                double rhs = doubleType.coerceNonNull(rhsObj);
                double lhs = doubleType.coerceNonNull(lhsObj);
                stack.push(lhs < rhs + EPSILON);

            } catch (InvalidValueException ignore) {
                stack.push(false);
            }
        });

        mOperators.put(">", (context, stack) -> {
            Object rhsObj = stack.pop();
            Object lhsObj = stack.pop();
            try {
                double rhs = doubleType.coerceNonNull(rhsObj);
                double lhs = doubleType.coerceNonNull(lhsObj);
                stack.push(lhs > rhs);

            } catch (InvalidValueException ignore) {
                stack.push(false);
            }
        });

        mOperators.put(">=", (context, stack) -> {
            Object rhsObj = stack.pop();
            Object lhsObj = stack.pop();
            try {
                double rhs = doubleType.coerceNonNull(rhsObj);
                double lhs = doubleType.coerceNonNull(lhsObj);
                stack.push(lhs + EPSILON > rhs);

            } catch (InvalidValueException ignore) {
                stack.push(false);
            }
        });

        mOperators.put("==", (context, stack) -> {
            Object rhs = stack.pop();
            Object lhs = stack.pop();
            stack.push(objectEquals(lhs, rhs));
        });

        mOperators.put("===", (context, stack) -> {
            Object rhs = stack.pop();
            Object lhs = stack.pop();
            stack.push(Objects.equals(lhs, rhs));
        });

        mOperators.put("!=", (context, stack) -> {
            Object rhs = stack.pop();
            Object lhs = stack.pop();
            if (Objects.equals(lhs, rhs)) {
                stack.push(false);
            } else if (rhs instanceof Number || lhs instanceof Number) {
                try {
                    double rhsd = doubleType.coerceNonNull(rhs);
                    double lhsd = doubleType.coerceNonNull(lhs);
                    stack.push(Math.abs(lhsd-rhsd) >= EPSILON);
                } catch (InvalidValueException x) {
                    stack.push(true);
                }
            } else {
                stack.push(true);
            }
        });

        mOperators.put("&&", (context, stack) -> {
            boolean rhs = booleanType.coerceNonNull(stack.pop());
            boolean lhs = booleanType.coerceNonNull(stack.pop());
            stack.push(lhs && rhs);
        });

        mOperators.put("||", (context, stack) -> {
            boolean rhs = booleanType.coerceNonNull(stack.pop());
            boolean lhs = booleanType.coerceNonNull(stack.pop());
            stack.push(lhs || rhs);
        });

        mOperators.put("XOR", (context, stack) -> {
            boolean rhs = booleanType.coerceNonNull(stack.pop());
            boolean lhs = booleanType.coerceNonNull(stack.pop());
            stack.push(lhs ^ rhs);
        });

        mOperators.put("MIN", (context, stack) -> {
            double rhs = doubleType.coerceNonNull(stack.pop());
            double lhs = doubleType.coerceNonNull(stack.pop());
            stack.push(lhs < rhs  ? lhs : rhs);
        });

        mOperators.put("MAX", (context, stack) -> {
            double rhs = doubleType.coerceNonNull(stack.pop());
            double lhs = doubleType.coerceNonNull(stack.pop());
            stack.push(lhs < rhs  ? rhs : lhs);
        });

        mOperators.put("ROUND", (context, stack) -> {
            double arg = doubleType.coerceNonNull(stack.pop());
            stack.push(Math.round(arg));
        });

        mOperators.put("FLOOR", (context, stack) -> {
            double arg = doubleType.coerceNonNull(stack.pop());
            stack.push(Math.floor(arg));
        });

        mOperators.put("CEIL", (context, stack) -> {
            double arg = doubleType.coerceNonNull(stack.pop());
            stack.push(Math.ceil(arg));
        });

        mOperators.put("!", (context, stack) -> {
            boolean arg = booleanType.coerceNonNull(stack.pop());
            stack.push(!arg);
        });

        mOperators.put("SIN", (context, stack) -> {
            double arg = doubleType.coerceNonNull(stack.pop());
            stack.push(Math.sin(arg*2.0*Math.PI));
        });

        mOperators.put("COS", (context, stack) -> {
            double arg = doubleType.coerceNonNull(stack.pop());
            stack.push(Math.cos(arg*2.0*Math.PI));
        });

        mOperators.put("RND", (context, stack) -> {
            stack.push(mRng.nextDouble());
        });

        mOperators.put("ASIN", (context, stack) -> {
            double arg = doubleType.coerceNonNull(stack.pop());
            stack.push(Math.asin(arg)/(2.0*Math.PI));
        });

        mOperators.put("ACOS", (context, stack) -> {
            double arg = doubleType.coerceNonNull(stack.pop());
            stack.push(Math.acos(arg)/(2.0*Math.PI));
        });

        mOperators.put("PI", (context, stack) -> {
            stack.push(Math.PI);
        });

        mOperators.put("TAU", (context, stack) -> {
            stack.push(Math.PI*2.0);
        });

        mOperators.put("E", (context, stack) -> {
            stack.push(Math.E);
        });

        mOperators.put("TRUE", (context, stack) -> {
            stack.push(true);
        });

        mOperators.put("FALSE", (context, stack) -> {
            stack.push(false);
        });

        mOperators.put("NOP", (context, stack) -> {
        });

        mOperators.put("RANGE", (context, stack) -> {
            double max = doubleType.coerceNonNull(stack.pop());
            double min = doubleType.coerceNonNull(stack.pop());
            double value = doubleType.coerceNonNull(stack.pop());

            if (value < min || value > max) {
                stack.push(0.0);
            } else {
                stack.push(1.0);
            }
        });

        mOperators.put("CLAMP", (context, stack) -> {
            double max = doubleType.coerceNonNull(stack.pop());
            double min = doubleType.coerceNonNull(stack.pop());
            double value = doubleType.coerceNonNull(stack.pop());

            if (value < min) {
                stack.push(min);
            } else if (value > max) {
                stack.push(max);
            } else {
                stack.push(value);
            }
        });

        mOperators.put("POLY2", (context, stack) -> {
            // ax^2 + bx + c
            double c = doubleType.coerceNonNull(stack.pop());
            double b = doubleType.coerceNonNull(stack.pop());
            double a = doubleType.coerceNonNull(stack.pop());
            double x = doubleType.coerceNonNull(stack.pop());

            stack.push(x * (x * a + b) + c);
        });

        mOperators.put("POLY3", (context, stack) -> {
            // ax^3 + bx^2 + cx + d
            double d = doubleType.coerceNonNull(stack.pop());
            double c = doubleType.coerceNonNull(stack.pop());
            double b = doubleType.coerceNonNull(stack.pop());
            double a = doubleType.coerceNonNull(stack.pop());
            double x = doubleType.coerceNonNull(stack.pop());

            stack.push(x * (x * (x * a + b) + c) + d);
            //stack.push(a*x*x*x + b*x*x + c*x + d);
        });

        mOperators.put("STOP", (context, stack) -> {
            stack.push(STOP);
        });

        mOperators.put("NULL", (context, stack) -> {
            stack.push(null);
        });

        mOperators.put("DUP", (context, stack) -> {
            Object value = stack.pop();
            stack.push(value);
            stack.push(value);
        });

        mOperators.put("SWAP", (context, stack) -> {
            Object valueA = stack.pop();
            Object valueB = stack.pop();
            stack.push(valueA);
            stack.push(valueB);
        });

        mOperators.put("DROP", (context, stack) -> {
            stack.pop();
        });

        mOperators.put("OVER", (context, stack) -> {
            Object valueA = stack.pop();
            Object valueB = stack.pop();
            stack.push(valueB);
            stack.push(valueA);
            stack.push(valueB);
        });

        mOperators.put("ROT", (context, stack) -> {
            Object valueA = stack.pop();
            Object valueB = stack.pop();
            Object valueC = stack.pop();
            stack.push(valueB);
            stack.push(valueA);
            stack.push(valueC);
        });

        mOperators.put("GET", (context, stack) -> {
            String key = stringType.coerceNonNull(stack.pop());
            Map<String,Object> dict = dictType.coerceNonNull(stack.pop());
            stack.push(dict);
            stack.push(dict.get(key));
        });

        mOperators.put("PUT", (context, stack) -> {
            String key = stringType.coerceNonNull(stack.pop());
            Object value = stack.pop();
            Map<String,Object> dict = dictType.coerceNonNull(stack.pop());
            dict.put(key, value);
            stack.push(dict);
        });

        mOperators.put("{}", (context, stack) -> stack.push(new HashMap<String,Object>()));

        mOperators.put("[]", (context, stack) -> stack.push(new ArrayList<>()));

        mOperators.put("[1]", (context, stack) -> {
            Object valueA = stack.pop();
            ArrayList<Object> list = new ArrayList<>();
            list.add(valueA);
            stack.push(list);
        });

        mOperators.put("[2]", (context, stack) -> {
            Object valueA = stack.pop();
            Object valueB = stack.pop();
            ArrayList<Object> list = new ArrayList<>();
            list.add(valueB);
            list.add(valueA);
            stack.push(list);
        });

        mOperators.put("[3]", (context, stack) -> {
            Object valueA = stack.pop();
            Object valueB = stack.pop();
            Object valueC = stack.pop();
            ArrayList<Object> list = new ArrayList<>();
            list.add(valueC);
            list.add(valueB);
            list.add(valueA);
            stack.push(list);
        });

        mOperators.put("[4]", (context, stack) -> {
            Object valueA = stack.pop();
            Object valueB = stack.pop();
            Object valueC = stack.pop();
            Object valueD = stack.pop();
            ArrayList<Object> list = new ArrayList<>();
            list.add(valueD);
            list.add(valueC);
            list.add(valueB);
            list.add(valueA);
            stack.push(list);
        });

        mOperators.put("POP", (context, stack) -> {
            List<Object> array = listType.coerceNonNull(stack.pop());
            if (array.isEmpty()) {
                throw new InvalidValueException("Cannot pop value from empty array");
            }
            array = new ArrayList<>(array);
            Object value = array.remove(array.size()-1);
            stack.push(array);
            stack.push(value);
        });

        mOperators.put("PUSH", (context, stack) -> {
            Object value = stack.pop();
            List<Object> array = listType.coerceNonNull(stack.pop());
            array = new ArrayList<>(array);
            array.add(value);
            stack.push(array);
        });
    }

    public static boolean isStopSignal(Object x) {
        return x == STOP;
    }

    // --------------------------------------------------------------------------------------------

    @Nullable
    private RPNContext mParent;
    private Map<String,Object> mVariables = new HashMap<>();
    private Set<WeakReference<RPNContext>> mChildren = new HashSet<>();
    private Map<String, Set<WeakReference<RPNFunction>>> mVariableFunctionLookup = new HashMap<>();

    public RPNContext() {
        mParent = null;
    }

    public RPNContext(RPNContext parent) {
        mParent = parent;
        mParent.mChildren.add(new WeakReference<>(this));
    }

    public void didChangeVariable(String key) {
        boolean needsChildCleanup = false;
        for (WeakReference<RPNContext> ref : mChildren) {
            RPNContext child = ref.get();

            if (child == null) {
               needsChildCleanup = true;
               continue;
            }

            child.didChangeVariable(key);
        }

        if (needsChildCleanup) {
            Set<WeakReference<RPNContext>> newChildren = new HashSet<>();
            for (WeakReference<RPNContext> ref : mChildren) {
                RPNContext child = ref.get();
                if (child == null) {
                    continue;
                }
                newChildren.add(ref);
            }
            mChildren = newChildren;
        }

        if (mVariableFunctionLookup.containsKey(key)) {
            boolean needsFuncCleanup = false;
            Set<WeakReference<RPNFunction>> funcs = mVariableFunctionLookup.get(key);
            for (WeakReference<RPNFunction> ref : funcs) {
                RPNFunction func = ref.get();
                if (func == null) {
                    needsFuncCleanup = true;
                    continue;
                }
                func.didChange();
            }

            if (needsFuncCleanup) {
                Set<WeakReference<RPNFunction>> newFuncs = new HashSet<>();
                for (WeakReference<RPNFunction> ref : funcs) {
                    RPNFunction func = ref.get();
                    if (func == null) {
                        continue;
                    }
                    newFuncs.add(ref);
                }
                mVariableFunctionLookup.put(key, newFuncs);
            }
        }
    }

    public void addFunctionVariableDependency(RPNFunction function, String variable) {
        Set<WeakReference<RPNFunction>> functions = mVariableFunctionLookup.computeIfAbsent(variable, (k) -> new HashSet<>());
        functions.add(new WeakReference<>(function));
    }

    public void setVariable(String key, @Nullable Object value) {
        if (mOperators.containsKey(key)) {
            throw new IllegalArgumentException("Can't set constant, \"" + key + "\" is a function");
        }

        mVariables.put(key, value);
        didChangeVariable(key);
    }

    public @Nullable Object getVariable(String key) {
        if (!mVariables.containsKey(key)) {
            if (mOperators.containsKey(key)) {
                throw new IllegalArgumentException("Can't get value for function \"" + key + "\"");
            }

            if (mParent != null) {
                return mParent.getVariable(key);
            }

            throw new RPNUnknownVariableException("Unknown variable \"" + key + "\"");
        }

        return mVariables.get(key);
    }

    public void updateRtcVariables(Calendar now) {
        final int SECONDS_PER_HOUR = 60*60;

        now.setFirstDayOfWeek(Calendar.MONDAY);

        setVariable("rtc.dom", now.get(Calendar.DAY_OF_MONTH) - now.getMinimum(Calendar.DAY_OF_MONTH));
        setVariable("rtc.doy", now.get(Calendar.DAY_OF_YEAR)  - now.getMinimum(Calendar.DAY_OF_YEAR));
        setVariable("rtc.moy", now.get(Calendar.MONTH) - now.getMinimum(Calendar.MONTH));
        setVariable("rtc.awm", now.get(Calendar.DAY_OF_WEEK_IN_MONTH) - now.getMinimum(Calendar.DAY_OF_WEEK_IN_MONTH));
        setVariable("rtc.y", now.get(Calendar.YEAR));
        setVariable("rtc.wom", now.get(Calendar.WEEK_OF_MONTH) - now.getMinimum(Calendar.WEEK_OF_MONTH));
        setVariable("rtc.woy", now.get(Calendar.WEEK_OF_YEAR) - now.getMinimum(Calendar.WEEK_OF_YEAR));

        int secondOfDay = now.get(Calendar.SECOND)
                + now.get(Calendar.MINUTE)*60
                + now.get(Calendar.HOUR_OF_DAY)*3600;
        setVariable("rtc.tod", (double)secondOfDay/SECONDS_PER_HOUR);

        // Convert to zero-based day with monday as start of week.
        int dow = now.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY;
        if (dow < 0) {
            dow = 7 - dow;
        }
        setVariable("rtc.dow", dow);
    }

    private TokenType identifyToken(String token) {
        if (token.matches(REGEX_NUMBER)) {
            return TokenType.CONST_NUMBER;
        }

        if (token.matches(REGEX_STRING)) {
            return TokenType.CONST_STRING;
        }

        if (mBranchOperators.contains(token)) {
            return TokenType.BRANCH;
        }

        if (mOperators.containsKey(token)) {
            return TokenType.OPERATOR;
        }

        if (token.matches(REGEX_VARIABLE)) {
            return TokenType.VARIABLE;
        }

        return TokenType.INVALID;
    }

    private RPNOperation compileRestOfIfThen(RPNFunction function, List<String> tokens) {

        // Remove IF token
        tokens.remove(0);

        RPNOperation opIf = compile(function, tokens);
        RPNOperation opElse;

        if (tokens.isEmpty()) {
            throw new RPNSyntaxErrorException("Missing ENDIF");
        }

        if ("ENDIF".equals(tokens.get(0))) {
            opElse = RPNOperation.NOOP;

        } else if ("ELSE".equals(tokens.get(0))) {
            tokens.remove(0);

            opElse = compile(function, tokens);

            if (tokens.isEmpty()) {
                throw new RPNSyntaxErrorException("Missing ENDIF");
            }

        } else {
            throw new RPNSyntaxErrorException("Unexpected token \"" + tokens.get(0) + "\"");
        }

        if (!"ENDIF".equals(tokens.get(0))) {
            throw new RPNSyntaxErrorException("Unexpected token \"" + tokens.get(0) + "\"");
        }

        tokens.remove(0);

        return (context,stack) -> {
            if (booleanType.coerceNonNull(stack.pop())) {
                opIf.perform(context, stack);
            } else {
                opElse.perform(context, stack);
            }
        };
    }

    private RPNOperation compileRestOfCaseOf(RPNFunction function, List<String> tokens) {
        Map<Object, RPNOperation> labels = new HashMap<>();
        RPNOperation opDefault = RPNOperation.NOOP;

        // Remove CASE token
        tokens.remove(0);

        do {
            RPNOperation opLabel = compile(function, tokens);

            if (tokens.isEmpty()) {
                throw new RPNSyntaxErrorException("Missing ENDCASE");
            }

            if ("OF".equals(tokens.get(0))) {
                tokens.remove(0);
                RPNOperation opAction = compile(function, tokens);

                if (tokens.isEmpty()) {
                    throw new RPNSyntaxErrorException("Missing ENDCASE");
                }

                if (!"ENDOF".equals(tokens.get(0))) {
                    throw new RPNSyntaxErrorException("Unexpected token \"" + tokens.get(0) + "\"");
                }

                // Remove ENDOF token
                tokens.remove(0);

                RPNStack stack = new RPNStack();
                Object label;
                try {
                    opLabel.perform(new RPNContext(), stack);
                    label = stack.pop();
                } catch (InvalidValueException|RPNException e) {
                    throw new RPNSyntaxErrorException("Case label must be constant", e);
                }

                labels.put(label, opAction);

                continue;
            }

            if ("ENDCASE".equals(tokens.get(0))) {
                opDefault = opLabel;
                break;
            }

            throw new RPNSyntaxErrorException("Unexpected token \"" + tokens.get(0) + "\"");

        } while (!tokens.isEmpty() && !"ENDCASE".equals(tokens.get(0)));

        if (tokens.isEmpty()) {
            throw new RPNSyntaxErrorException("Missing ENDCASE");
        }

        if (!"ENDCASE".equals(tokens.get(0))) {
            throw new RPNSyntaxErrorException("Unexpected token \"" + tokens.get(0) + "\"");
        }

        tokens.remove(0);

        final RPNOperation realDefault = opDefault;

        return (context,stack) -> {
            Object value = stack.pop();
            for (Map.Entry<Object, RPNOperation> entry : labels.entrySet()) {
                if (objectEquals(entry.getKey(), value)) {
                    entry.getValue().perform(context,stack);
                    return;
                }
            }
            stack.push(value);
            realDefault.perform(context,stack);
            stack.pop();
        };
    }

    private RPNOperation compileRestOfDoLoop(RPNFunction function, List<String> tokens) {
        // Remove DO token
        tokens.remove(0);

        RPNOperation opLoop = compile(function, tokens);

        if (tokens.isEmpty()) {
            throw new RPNSyntaxErrorException("Missing LOOP");
        }

        tokens.remove(0);

        return (context,stack) -> {
            int start = integerType.coerceNonNull(stack.pop());
            int limit = integerType.coerceNonNull(stack.pop());
            RPNContext newContext = new RPNContext(context);
            for (; limit > start ; start++) {
                newContext.setVariable("i",start);
                opLoop.perform(newContext, stack);
            }
        };
    }

    private RPNOperation compile(RPNFunction function, List<String> tokens) {
        RPNComboOperation operation = new RPNComboOperation();

        while (!tokens.isEmpty()) {
            String token = tokens.get(0);
            if (token.isEmpty()) {
                tokens.remove(0);
                continue;
            }
            switch (identifyToken(token)) {
                case CONST_NUMBER:
                    tokens.remove(0);
                    try {
                        double value = Double.valueOf(token);
                        operation.mOperations.add((context, stack) -> stack.push(value));
                    } catch (NumberFormatException e) {
                        throw new RPNSyntaxErrorException(e);
                    }
                    break;

                case CONST_STRING:
                    tokens.remove(0);
                    try {
                        String value = token.replaceAll("^:", "");
                        operation.mOperations.add((context, stack) -> stack.push(value));
                    } catch (NumberFormatException e) {
                        throw new RPNSyntaxErrorException(e);
                    }
                    break;

                case OPERATOR:
                    tokens.remove(0);
                    operation.mOperations.add(mOperators.get(token));
                    break;

                case BRANCH:
                    switch (token) {
                        case "IF":
                            operation.mOperations.add(compileRestOfIfThen(function, tokens));
                            break;

                        case "CASE":
                            operation.mOperations.add(compileRestOfCaseOf(function, tokens));
                            break;

                        case "DO":
                            operation.mOperations.add(compileRestOfDoLoop(function, tokens));
                            break;

                        default:
                            return operation;
                    }
                    break;

                case VARIABLE:
                    tokens.remove(0);
                    addFunctionVariableDependency(function, token);
                    operation.mOperations.add((context, stack) -> stack.push(context.getVariable(token)));
                    break;

                case INVALID:
                    throw new RPNSyntaxErrorException("Invalid token \"" + token + "\"");
            }
        }
        return operation;
    }

    public RPNFunction compile(String recipe) {
        List<String> tokenList = new ArrayList<>(Arrays.asList(recipe.split("\\s+")));

        RPNComboOperation operation = new RPNComboOperation();
        RPNFunction function = new RPNFunction(this, operation);

        operation.mOperations.add(compile(function, tokenList));

        if (!tokenList.isEmpty()) {
            throw new RPNSyntaxErrorException("Unexpected token \"" + tokenList.get(0) + "\"");
        }

        return function;
    }

    class RPNComboOperation implements RPNOperation {
        final List<RPNOperation> mOperations = new ArrayList<>();

        public void addOperation(RPNOperation x) {
            mOperations.add(x);
        }

        @Override
        public void perform(RPNContext context, RPNStack stack) throws InvalidValueException {
            for (RPNOperation op : mOperations) {
                op.perform(context, stack);
            }
        }
    }
}
