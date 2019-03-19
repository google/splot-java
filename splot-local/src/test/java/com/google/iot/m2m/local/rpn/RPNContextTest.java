package com.google.iot.m2m.local.rpn;

import com.google.iot.m2m.base.InvalidValueException;
import com.google.iot.m2m.base.TypeConverter;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

class RPNContextTest {
    static final double EPSILON = 0.000001;

    RPNContext mContext = null;

    @BeforeEach
    void setUp() {
        mContext = new RPNContext();
    }

    @AfterEach
    void tearDown() {
        mContext = null;
    }

    void assertExpression(double expected, String expr, double input) {
        try {
            double output = TypeConverter.DOUBLE.coerceNonNull(mContext.compile(expr).apply(input));

            if (EPSILON < Math.abs(output - expected)) {
                assertEquals(expected, output);
            }

        } catch(InvalidValueException x) {
            throw new AssertionError(x);
        }
    }

    void assertExpression(@Nullable Object expected, String expr, @Nullable Object input) {
        Object outputObj = mContext.compile(expr).apply(input);

        assertEquals(expected, outputObj);
    }

    void assertExpression(double expected, String expr) {
        assertExpression(expected, expr, 0.0);
    }

    @Test
    void tokensAndTypes() {
        assertExpression(new HashMap<String, Object>(), "{}", null);
        assertExpression(0.0, "0", null);
        assertExpression(-0.0, "-0", null);
        assertExpression(0.0, "+0", null);
        assertExpression(1.0, "1", null);
        assertExpression(1.0, "1.0", null);
        assertExpression(1.0, "1.000", null);
        assertExpression(1.0, "+1.000", null);
        assertExpression(-1.0, "-1.000", null);
        assertExpression("foobar", ":foobar", null);
        assertExpression(true, "TRUE", null);
        assertExpression(false, "FALSE", null);
    }


    @Test
    void operatorMultiply() {
        assertExpression(2.0, "2 *", 1.0);
        assertExpression(1.0, "0.5 *", 2.0);
        assertExpression(3.0, "0.5 2 * *", 3.0);
    }

    @Test
    void operatorAdd() {
        assertExpression(3.0, "2 +", 1.0);
        assertExpression(2.5, "0.5 +", 2.0);
        assertExpression(2.5, "0.5 -1 + +", 3.0);
    }

    @Test
    void operatorSubtract() {
        assertExpression(-1.0, "2 -", 1.0);
        assertExpression(1.5, "0.5 -", 2.0);
        assertExpression(1.5, "0.5 -1 - -", 3.0);
    }

    @Test
    void operatorDivide() {
        assertExpression(0.5, "2 /", 1.0);
        assertExpression(4.0, "0.5 /", 2.0);
        assertExpression(12.0, "0.5 2 / /", 3.0);
    }

    @Test
    void operatorModulus() {
        assertExpression(1.0, "2 %", 1.0);
        assertExpression(0.5, "2 %", 2.5);
        assertExpression(0.25, "0.5 %", 2.25);
        assertExpression(1.0, "5 2 %");
        assertExpression(0.0, "0 3 %");
        assertExpression(1.0, "1 3 %");
        assertExpression(2.0, "-1 3 %");
        assertExpression(-1.0, "-1 -3 %");
    }

    @Test
    void operatorNeg() {
        assertExpression(-1.0, "NEG", 1.0);
        assertExpression(-4.0, "0.5 NEG /", 2.0);
        assertExpression(1.5, "0.5 1 NEG - -", 3.0);
    }

    @Test
    void operatorGreaterThan() {
        assertExpression(0.0, "2 >", 1.0);
        assertExpression(0.0, "2 >", 2.0);
        assertExpression(1.0, "2 >", 3.0);
        assertExpression(1.0, "2 >", 2.0 + EPSILON);
        assertExpression(0.0, "2 >", 2.0 - EPSILON);
    }

    @Test
    void operatorGreaterThanOrEqual() {
        assertExpression(0.0, "2 >=", 1.0);
        assertExpression(1.0, "2 >=", 2.0);
        assertExpression(1.0, "2 >=", 3.0);
        assertExpression(1.0, "2 >=", 2.0 + EPSILON);
        assertExpression(0.0, "2 >=", 2.0 - EPSILON);
    }

    @Test
    void operatorLessThan() {
        assertExpression(1.0, "2 <", 1.0);
        assertExpression(0.0, "2 <", 2.0);
        assertExpression(0.0, "2 <", 3.0);
        assertExpression(0.0, "2 <", 2.0 + EPSILON);
        assertExpression(1.0, "2 <", 2.0 - EPSILON);
    }

    @Test
    void operatorLessThanOrEqual() {
        assertExpression(1.0, "2 <=", 1.0);
        assertExpression(1.0, "2 <=", 2.0);
        assertExpression(0.0, "2 <=", 3.0);
        assertExpression(0.0, "2 <=", 2.0 + EPSILON);
        assertExpression(1.0, "2 <=", 2.0 - EPSILON);
    }

    @Test
    void operatorAnd() {
        assertExpression(1.0, "1 &&", 1.0);
        assertExpression(0.0, "0 &&", 1.0);
        assertExpression(1.0, "4 &&", 2.0);
        assertExpression(0.0, "0 &&", 0.0);
        assertExpression(0.0, "1 &&", 0.0);
        assertExpression(1.0, "0.5 &&", 0.5);
        assertExpression(0.0, "0.5 &&", 0.4);
        assertExpression(0.0, "-1.5 &&", -0.4);
    }

    @Test
    void operatorOr() {
        assertExpression(1.0, "1 ||", 1.0);
        assertExpression(1.0, "0 ||", 1.0);
        assertExpression(1.0, "4 ||", 2.0);
        assertExpression(0.0, "0 ||", 0.0);
        assertExpression(1.0, "1 ||", 0.0);
        assertExpression(1.0, "0.5 ||", 0.5);
        assertExpression(1.0, "0.5 ||", 0.4);
        assertExpression(0.0, "-1.5 ||", -0.4);
    }

    @Test
    void operatorXor() {
        assertExpression(0.0, "1 XOR", 1.0);
        assertExpression(1.0, "0 XOR", 1.0);
        assertExpression(0.0, "4 XOR", 2.0);
        assertExpression(0.0, "0 XOR", 0.0);
        assertExpression(1.0, "1 XOR", 0.0);
        assertExpression(0.0, "0.5 XOR", 0.5);
        assertExpression(1.0, "0.5 XOR", 0.4);
        assertExpression(0.0, "-1.5 XOR", -0.4);
    }

    @Test
    void operatorNot() {
        assertExpression(0.0, "!", 1.0);
        assertExpression(0.0, "!", 0.5);
        assertExpression(1.0, "!", 0.0);
        assertExpression(1.0, "!", 0.2);
    }

    @Test
    void operatorMin() {
        assertExpression(1.0, "1 MIN", 1.0);
        assertExpression(0.0, "0 MIN", 1.0);
        assertExpression(2.0, "4 MIN", 2.0);
        assertExpression(-4.0, "-4 MIN", 3.0);
        assertExpression(-4.0, "-4 MIN", -3.0);
        assertExpression(-5.0, "-4 MIN", -5.0);
        assertExpression(10.0, "100 MIN", 10.0);
    }

    @Test
    void operatorMax() {
        assertExpression(1.0, "1 MAX", 1.0);
        assertExpression(1.0, "0 MAX", 1.0);
        assertExpression(4.0, "4 MAX", 2.0);
        assertExpression(3.0, "-4 MAX", 3.0);
        assertExpression(-3.0, "-4 MAX", -3.0);
        assertExpression(-4.0, "-4 MAX", -5.0);
        assertExpression(100.0, "100 MAX", 10.0);
    }

    @Test
    void operatorClamp() {
        assertExpression(0.0, "0 1 CLAMP", 0.0);
        assertExpression(0.5, "0 1 CLAMP", 0.5);
        assertExpression(1.0, "0 1 CLAMP", 1.0);
        assertExpression(1.0, "0 1 CLAMP", 1.1);
        assertExpression(0.0, "0 1 CLAMP", -0.1);
        assertExpression(50.0, "50 100 CLAMP", 0.0);
        assertExpression(75.0, "50 100 CLAMP", 75.0);
        assertExpression(100.0, "50 100 CLAMP", 101.0);
    }

    @Test
    void operatorRange() {
        assertExpression(1.0, "0 1 RANGE", 0.0);
        assertExpression(1.0, "0 1 RANGE", 0.5);
        assertExpression(1.0, "0 1 RANGE", 1.0);
        assertExpression(0.0, "0 1 RANGE", 1.1);
        assertExpression(0.0, "0 1 RANGE", -0.1);
        assertExpression(0.0, "50 100 RANGE", 0.0);
        assertExpression(1.0, "50 100 RANGE", 75.0);
        assertExpression(0.0, "50 100 RANGE", 101.0);
    }

    @Test
    void operatorRound() {
        assertExpression(2.0, "ROUND", 2.0);
        assertExpression(2.0, "ROUND", 2.4);
        assertExpression(3.0, "ROUND", 2.5);
        assertExpression(3.0, "ROUND", 2.6);
        assertExpression(3.0, "ROUND", 2.9);
        assertExpression(3.0, "ROUND", 3.0);
    }

    @Test
    void operatorFloor() {
        assertExpression(2.0, "FLOOR", 2.0);
        assertExpression(2.0, "FLOOR", 2.4);
        assertExpression(2.0, "FLOOR", 2.5);
        assertExpression(2.0, "FLOOR", 2.6);
        assertExpression(2.0, "FLOOR", 2.9);
        assertExpression(3.0, "FLOOR", 3.0);
    }

    @Test
    void operatorCeil() {
        assertExpression(2.0, "CEIL", 2.0);
        assertExpression(3.0, "CEIL", 2.4);
        assertExpression(3.0, "CEIL", 2.5);
        assertExpression(3.0, "CEIL", 2.6);
        assertExpression(3.0, "CEIL", 2.9);
        assertExpression(3.0, "CEIL", 3.0);
    }

    @Test
    void operatorSwap() {
        assertExpression(2.0, "4 SWAP /", 2.0);
    }

    @Test
    void operatorDup() {
        assertExpression(4.0, "DUP *", 2.0);
        assertExpression(9.0, "DUP *", 3.0);
    }

    @Test
    void operatorPow() {
        assertExpression(4.0, "2 ^", 2.0);
        assertExpression(9.0, "2 ^", 3.0);
        assertExpression(3.0, "0.5 ^", 9.0);
    }

    @Test
    void operatorLog() {
        assertExpression(0.0, "LOG", 1.0);
        assertExpression(1.0, "LOG", Math.E);
    }

    @Test
    void operatorEquals() {
        assertExpression(true, ":foo ==", "foo");
        assertExpression(false, ":foo ==", "bar");
        assertExpression(false, ":foo ==", 0.2);
        assertExpression(1.0, "2 ==", 2.0);
        assertExpression(true, "1 ==", true);
        assertExpression(0.0, "1 ==", 2.0);
        assertExpression(1.0, "0 ==", 0.0);
        assertExpression(1.0, "0 ==", -0.0);
        assertExpression(1.0, "-0 ==", 0.0);
        assertExpression(1.0, "-2 ==", -2.0);
        assertExpression(0.0, "-2 ==", -3.0);
    }

    @Test
    void operatorNotEquals() {
        assertExpression(false, ":foo !=", "foo");
        assertExpression(true, ":foo !=", "bar");
        assertExpression(true, ":foo !=", 0.2);
        assertExpression(0.0, "2 !=", 2.0);
        assertExpression(false, "1 !=", true);
        assertExpression(1.0, "1.0 !=", 2.0);
        assertExpression(0.0, "0.0 !=", 0.0);
        assertExpression(0.0, "0.0 !=", -0.0);
        assertExpression(0.0, "-0.0 !=", 0.0);
        assertExpression(0.0, "-2.0 !=", -2.0);
        assertExpression(1.0, "-2.0 !=", -3.0);
    }

    @Test
    void operatorGet() {
        HashMap<String,Object> dict = new HashMap<>();

        dict.put("x", 25.0);
        dict.put("y", 75.0);

        assertExpression(25.0, ":x GET", dict);
    }

    @Test
    void operatorPut() {
        HashMap<String,Object> dict = new HashMap<>();

        dict.put("x", 25.0);
        dict.put("y", 75.0);

        assertExpression(dict, "{} 25.0 :x PUT 75.0 :y PUT", null);
    }

    @Test
    void operatorEmptyArray() {
        ArrayList<Object> list = new ArrayList<>();

        assertExpression(list, "[]", null);
    }

    @Test
    void operatorArray1() {
        ArrayList<Object> list = new ArrayList<>();

        list.add(25.0);

        assertExpression(list, "25.0 [1]", null);
    }

    @Test
    void operatorArray2() {
        ArrayList<Object> list = new ArrayList<>();

        list.add(25.0);
        list.add(75.0);

        assertExpression(list, "25.0 75.0 [2]", null);
    }

    @Test
    void operatorArray3() {
        ArrayList<Object> list = new ArrayList<>();

        list.add(25.0);
        list.add(75.0);
        list.add("blah");

        assertExpression(list, "25.0 75.0 :blah [3]", null);
    }

    @Test
    void operatorArray4() {
        ArrayList<Object> list = new ArrayList<>();

        list.add(25.0);
        list.add(75.0);
        list.add("blah");
        list.add(200.0);

        assertExpression(list, "25.0 75.0 :blah 200.0 [4]", null);
    }

    @Test
    void operatorPop() {
        ArrayList<Object> inList = new ArrayList<>();
        ArrayList<Object> outList = new ArrayList<>();

        inList.add(25.0);
        inList.add(75.0);

        outList.add(25.0);

        assertExpression(outList, "POP DROP", inList);
    }

    @Test
    void emptyExpression() {
        assertExpression(0.0, "", 0.0);
        assertExpression(1.0, "", 1.0);
        assertExpression(55.5, "", 55.5);
        assertExpression("blah", "", "blah");
    }

    @Test
    void operatorSin() {
        assertExpression(0.0, "SIN", 0.0);
        assertExpression(1.0, "SIN", 0.25);
        assertExpression(0.0, "SIN", 0.5);
        assertExpression(-1.0, "SIN", 0.75);
        assertExpression(0.0, "SIN", 1.0);
        assertExpression(Math.sin(0.25*Math.PI), "SIN", 0.125);
    }

    @Test
    void operatorCos() {
        assertExpression(1.0, "COS", 0.0);
        assertExpression(0.0, "COS", 0.25);
        assertExpression(-1.0, "COS", 0.5);
        assertExpression(0.0, "COS", 0.75);
        assertExpression(1.0, "COS", 1.0);
        assertExpression(Math.cos(0.25*Math.PI), "COS", 0.125);
    }

    @Test
    void operatorArcSin() {
        assertExpression(0.0, "ASIN", 0.0);
        assertExpression(0.25, "ASIN", 1.0);
        assertExpression(-0.25, "ASIN", -1.0);
        assertExpression(Math.asin(0.125)/(Math.PI*2), "ASIN", 0.125);
    }

    @Test
    void operatorArcCos() {
        assertExpression(0.25, "ACOS", 0.0);
        assertExpression(0.0, "ACOS", 1.0);
        assertExpression(0.5, "ACOS", -1.0);
        assertExpression(Math.acos(0.125)/(Math.PI*2), "ACOS", 0.125);
    }

    @Test
    void operatorDrop() {
        assertExpression(20.0, "10 DROP", 20.0);
    }

    @Test
    void mathConstants() {
        assertExpression(Math.PI, "PI");
        assertExpression(Math.PI*2, "TAU");
        assertExpression(Math.E, "E");
        assertNull(mContext.compile("NULL").apply(0.0));
        assertTrue(RPNContext.isStopSignal(mContext.compile("STOP").apply(0.0)));
    }

    @Test
    void miscExpressions() {
        assertExpression(1.0, "E LOG");
        assertExpression(2.0, "LOG 10 LOG /", 100);
        assertExpression(4.0, "LOG 10 LOG /", 10000);
        assertExpression(1.0, "75 SWAP 100.0 RANGE", 50.0);
        assertExpression(0.0, "0 1 CLAMP 4 / SIN", 0.0);
        assertExpression(1.0, "0 1 CLAMP 4 / SIN", 1.0);
        assertExpression(0.0, "0 1 CLAMP 4 / SIN", -0.1);
        assertExpression(1.0, "0 1 CLAMP 4 / SIN", 1.1);
        assertExpression(Math.sin(Math.PI/4), "0 1 CLAMP 4 / SIN", 0.5);
    }

    @Test
    void variables() {
        mContext.setVariable("foo", 2.5);

        RPNFunction fooFunc = mContext.compile("foo");

        assertEquals((Double)2.5, fooFunc.apply(0));

        mContext.setVariable("foo", 2.0);

        assertEquals((Double)2.0, fooFunc.apply(0));

        mContext.setVariable("bar", 3);

        RPNFunction fooBarFunc = mContext.compile("foo bar *");

        assertEquals((Double)6.0, fooBarFunc.apply(0));

        mContext.setVariable("foo", 2.5);

        assertEquals((Double)7.5, fooBarFunc.apply(0));
    }

    @Test
    void nestedVariables() {
        mContext.setVariable("foo", 1);
        mContext = new RPNContext(mContext);
        mContext.setVariable("foo", 2.5);
        mContext.setVariable("bar", 3);
        assertExpression(2.5, "foo");
        assertExpression(7.5, "foo bar *");
    }

    @Test
    void exceptionStackUnderflow() {
        assertThrows(RPNStackUnderflowException.class, () -> mContext.compile("+").apply(0.0));
    }

    @Test
    void exceptionStackOverflow() {
        String overflowProg = "0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0";
        assertThrows(RPNStackOverflowException.class, () -> mContext.compile(overflowProg).apply(0.0));
    }

    @Test
    void exceptionUnknownVariable() {
        assertThrows(RPNUnknownVariableException.class, () -> mContext.compile("what").apply(0.0));
    }

    @Test
    void exceptionSyntaxError() {
        assertThrows(RPNSyntaxErrorException.class, () -> mContext.compile("CASE").apply(null));
        assertThrows(RPNSyntaxErrorException.class, () -> mContext.compile("1 IF 0 ELSE 1").apply(null));
        assertThrows(RPNSyntaxErrorException.class, () -> mContext.compile("1 OF 5 ENDOF").apply(null));
    }

    @Test
    void branchOperatorIfElse() {
        assertExpression(10.0, "IF 10 ELSE 100 ENDIF", 1.0);
        assertExpression(100.0, "IF 10 ELSE 100 ENDIF", 0.0);
    }

    @Test
    void branchOperatorCaseOf() {
        assertExpression(3, "CASE 3 SWAP ENDCASE", 100);

        assertExpression(0.0, "CASE 0 OF 0 ENDOF 1 OF 10 ENDOF 100 SWAP ENDCASE", 0.0);
        assertExpression(10.0, "CASE 0 OF 0 ENDOF 1 OF 10 ENDOF 100 SWAP ENDCASE", 1.0);
        assertExpression(100.0, "CASE 0 OF 0 ENDOF 1 OF 10 ENDOF 100 SWAP ENDCASE", 2.0);

        assertExpression(1.0, "CASE :foo OF 1 ENDOF :bar OF 2 ENDOF 3 SWAP ENDCASE", "foo");
        assertExpression(2.0, "CASE :foo OF 1 ENDOF :bar OF 2 ENDOF 3 SWAP ENDCASE", "bar");
        assertExpression(3.0, "CASE :foo OF 1 ENDOF :bar OF 2 ENDOF 3 SWAP ENDCASE", "blah");
    }

    @Test
    void branchOperatorDoLoop() {
        assertExpression(7.0, "4 0 DO i + LOOP", 1.0);
        assertExpression(40.0, "20 10 0 DO 2 + LOOP", 0.0);
    }

    double calcCct(double x, double y) {
        double n = (x - 0.3320) / (y - 0.1858);
        return -449.0*n*n*n + 3525.0*n*n - 6823.3*n + 5520.33;
    }
    @Test
    void correlatedColorTemperatureExample() {
        // This expression calculates the approx. correlated color temperature from an [x,y] input
        String cctProg =
                "POP 0.1858 - SWAP " + // y' = y - 0.1858
                "POP 0.3320 - SWAP " + // x' = y - 0.3320
                "DROP " + // Drop empty array
                "SWAP " + // y' x' -> x' y'
                "/ " + // n =  x'/y'
                "-449 3525 -6823.3 5520.33 POLY3 " +
                "1500 7000 CLAMP";

        // D65
        assertExpression(calcCct(0.3127, 0.3290), "0.3127 0.3290 [2] " + cctProg, null);

        // D50
        assertExpression(calcCct(0.34567, 0.35850), "0.34567 0.35850 [2] " + cctProg, null);
    }


}