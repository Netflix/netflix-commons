/*
*
* Copyright 2013 Netflix, Inc.
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
*
*/
package com.netflix.util;


/**
 * Helper class for computing hash codes.
 * <p>
 * The idea here is that objects that define "equals" to
 * mean that a number of member variables are equal needs a
 * hash code that is computed from those same member variables.
 * <p>
 * Example:
 * <pre>
 * class MyClass {
 *    ...
 *    private int membVar1;
 *    private OtherClass membVar2;
 *    private float membVar3;
 *    ...
 *    // MyClass objects are equal iff they have the same content.
 *    public boolean equals(Object obj) {
 *        if (obj == this) {
 *            return true;
 *        } else if (obj == null || obj.getClass() != getClass()) {
 *            return false;
 *        }
 *        MyClass cObj = (MyClass)obj;
 *        return (membVar1 == cObj.membVar1)
 *            &amp;&amp; HashCode.equalObjects(membVar2, cObj.membVar2)
 *            &amp;&amp; (membVar3 == bVar3);
 *    }
 *
 *    // The hash code needs to align with the
 *    // definition of equals.
 *    public int hashCode() {
 *        HashCode h = new HashCode();
 *        h.addValue(membVar1);
 *        h.addValue(membVar2);
 *        h.addValue(membVar2);
 *        return h.hashCode();
 *    }
 *    ...
 * }
 * </pre>
 */
public class HashCode {

    /*
     * Based on the algorithm in "Effective Java"
     */

    // ========================================
    // Static vars: public, protected, then private
    // ========================================

    private static final int SEED = 17;
    private static final long SCALE = 37;

    // ========================================
    // Instance vars: public, protected, then private
    // ========================================

    private int mVal;

    // ========================================
    // Constructors
    // ========================================

    /**
     * Create a new HashCode object
     */
    public HashCode() {
        mVal = SEED;
    }

    // ========================================
    // Methods, grouped by functionality, *not* scope
    // ========================================

    /**
     * Augment the current computed hash code with the
     * value <i>obj</i>.
     *
     * @param obj value being added
     * @return the new hash code.
     */
    public int addValue(Object obj) {
        return foldIn((obj != null) ? obj.hashCode() : 0);
    }

    /**
     * Augment the current computed hash code with the
     * value <i>b</i>.
     *
     * @param b value being added
     * @return the new hash code.
     */
    public int addValue(boolean b) {
        return foldIn(b ? 0 : 1);
    }

    /**
     * Augment the current computed hash code with the
     * value <i>i</i>.
     *
     * @param i value being added
     * @return the new hash code.
     */
    public int addValue(byte i) {
        return foldIn(i);
    }

    /**
     * Augment the current computed hash code with the
     * value <i>i</i>.
     *
     * @param i value being added
     * @return the new hash code.
     */
    public int addValue(char i) {
        return foldIn(i);
    }

    /**
     * Augment the current computed hash code with the
     * value <i>i</i>.
     *
     * @param i value being added
     * @return the new hash code.
     */
    public int addValue(short i) {
        return foldIn(i);
    }

    /**
     * Augment the current computed hash code with the
     * value <i>i</i>.
     *
     * @param i value being added
     * @return the new hash code.
     */
    public int addValue(int i) {
        return foldIn(i);
    }

    /**
     * Augment the current computed hash code with the
     * value <i>f</i>.
     *
     * @param f value being added
     * @return the new hash code.
     */
    public int addValue(float f) {
        return foldIn(Float.floatToIntBits(f));
    }

    /**
     * Augment the current computed hash code with the
     * value <i>f</i>.
     *
     * @param f value being added
     * @return the new hash code.
     */
    public int addValue(double f) {
        return foldIn(Double.doubleToLongBits(f));
    }

    // --------------------
    // Arrays

    /**
     * Augment the current computed hash code with the
     * value <i>array</i>.
     *
     * @param array value being added
     * @return the new hash code.
     */
    public int addValue(Object[] array) {
        int val = hashCode();
        for (Object obj : array) {
            val = addValue(obj);
        }
        return val;
    }

    /**
     * Augment the current computed hash code with the
     * value <i>array</i>.
     *
     * @param array value being added
     * @return the new hash code.
     */
    public int addValue(boolean[] array) {
        int val = hashCode();
        for (boolean b : array) {
            val = addValue(b);
        }
        return val;
    }

    /**
     * Augment the current computed hash code with the
     * value <i>i</i>.
     *
     * @param array value being added
     * @return the new hash code.
     */
    public int addValue(byte[] array) {
        int val = hashCode();
        for (byte i : array) {
            val = addValue(i);
        }
        return val;
    }

    /**
     * Augment the current computed hash code with the
     * value <i>array</i>.
     *
     * @param array value being added
     * @return the new hash code.
     */
    public int addValue(char[] array) {
        int val = hashCode();
        for (char i : array) {
            val = addValue(i);
        }
        return val;
    }

    /**
     * Augment the current computed hash code with the
     * value <i>array</i>.
     *
     * @param array value being added
     * @return the new hash code.
     */
    public int addValue(short[] array) {
        int val = hashCode();
        for (short i : array) {
            val = addValue(i);
        }
        return val;
    }

    /**
     * Augment the current computed hash code with the
     * value <i>array</i>.
     *
     * @param array value being added
     * @return the new hash code.
     */
    public int addValue(int[] array) {
        int val = hashCode();
        for (int i : array) {
            val = addValue(i);
        }
        return val;
    }

    /**
     * Augment the current computed hash code with the
     * value <i>array</i>.
     *
     * @param array value being added
     * @return the new hash code.
     */
    public int addValue(float[] array) {
        int val = hashCode();
        for (float f : array) {
            val = addValue(f);
        }
        return val;
    }

    /**
     * Augment the current computed hash code with the
     * value <i>array</i>.
     *
     * @param array value being added
     * @return the new hash code.
     */
    public int addValue(double[] array) {
        int val = hashCode();
        for (double f : array) {
            val = addValue(f);
        }
        return val;
    }

    // --------------------
    // Utility methods

    /**
     * Utility function to make it easy to compare two, possibly null, objects.
     *
     * @param o1 first object
     * @param o2 second object
     * @return true iff either both objects are null, or
     * neither are null and they are equal.
     */
    public static boolean equalObjects(Object o1, Object o2) {
        if (o1 == null) {
            return (o2 == null);
        } else if (o2 == null) {
            return false;
        } else {
            return o1.equals(o2);
        }
    }

    // --------------------
    // Internals

    private int foldIn(int c) {
        return setVal((SCALE * mVal) + c);
    }

    private int foldIn(long c) {
        return setVal((SCALE * mVal) + c);
    }

    private int setVal(long l) {
        mVal = (int)(l ^ (l>>>32));
        return mVal;
    }

    // ----------------------------------------
    // Generic object protocol

    /**
     * Get the currently computed hash code value.
     */
    @Override
    public int hashCode() {
        return mVal;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj == null || obj.getClass() != getClass()) {
            return false;
        }
        HashCode h = (HashCode)obj;
        return (h.hashCode() == hashCode());
    }

    @Override
    public String toString() {
        return "{HashCode " + mVal + "}";
    }

} // HashCode
