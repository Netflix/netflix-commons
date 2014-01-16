package com.netflix.infix;

import com.google.common.base.Predicate;

final public class AlwaysTruePredicate implements Predicate<Object> {

    public static final AlwaysTruePredicate INSTANCE = new AlwaysTruePredicate();
    
    private AlwaysTruePredicate() {
	}

	@Override
	public boolean apply(Object input) {
		return true;
	}

    @Override
	public String toString() {
		return "AlwaysTruePredicate []";
	}

	@Override
	public int hashCode() {
		return Boolean.TRUE.hashCode();
	}

	@Override
	public boolean equals(Object obj){
		return obj instanceof AlwaysTruePredicate;
	}
}
