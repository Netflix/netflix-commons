/**
 * @file
 *
 * @author slanning@netflix.com
 * @date <2007-08-31 15:42:56 slanning>
 *
 * Copyright (C) 2007 Netflix, Inc.  All rights reserved.
 */

package com.netflix.util;

import java.io.Serializable;


/**
 * A simple class that holds a pair of values.
 * This may be useful for methods that care to
 * return two values (instead of just one).
 */
public class Pair<E1,E2> implements Serializable {

    // ========================================
    // Static vars: public, protected, then private
    // ========================================
    private static final long serialVersionUID = 2L;

    // ========================================
    // Instance vars: public, protected, then private
    // ========================================

    private E1 mFirst;
    private E2 mSecond;

    // ========================================
    // Constructors
    // ========================================

    /**
     * Construct a new pair
     *
     * @param first the object to store as the first value
     * @param second the object to store as the second value
     */
    public Pair(E1 first, E2 second) {
        mFirst = first;
        mSecond = second;
    }

    // ========================================
    // Methods, grouped by functionality, *not* scope
    // ========================================

    /**
     * Get the first value from the pair.
     *
     * @return the first value
     */
    public E1 first() {
        return mFirst;
    }

    /**
     * Get the second value from the pair.
     *
     * @return the second value
     */
    public E2 second() {
        return mSecond;
    }

    /**
     * Set the first value of the pair.
     *
     * @param first the new first value
     */
    public void setFirst(E1 first) {
        mFirst = first;
    }

    /**
     * Set the second value of the pair.
     *
     * @param second the new second value
     */
    public void setSecond(E2 second) {
        mSecond = second;
    }

    // ----------------------------------------
    // Generic Object methods

    /**
     * Pair objects are equal iff they have the same content.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj == null || obj.getClass() != getClass()) {
            return false;
        }
        Pair other = (Pair)obj;
        return HashCode.equalObjects(mFirst, other.mFirst)
            && HashCode.equalObjects(mSecond, other.mSecond);
    }

    // The hash code needs to align with the
    // definition of equals.
    @Override
    public int hashCode() {
        HashCode h = new HashCode();
        h.addValue(mFirst);
        h.addValue(mSecond);
        return h.hashCode();
    }

} // Pair
