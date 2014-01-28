package com.netflix.infix;

import com.google.common.base.Predicate;

final public class AlwaysFalsePredicate implements Predicate<Object> {

    public static final AlwaysFalsePredicate INSTANCE = new AlwaysFalsePredicate();
    
	// There's no point of creating multiple instance of this class
	private AlwaysFalsePredicate(){
    }
	
	@Override
    public boolean apply(Object input) {
	    return false;
    }

    @Override
    public String toString() {
        return "AlwaysFalsePredicate []";
    }

	@Override
	public int hashCode() {
		return Boolean.FALSE.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
        return obj instanceof AlwaysFalsePredicate;
    }
}
