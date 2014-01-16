package com.netflix.infix;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;

/**
 * A number of static helper methods to simplify the construction of combined event filters. 
 */
public class Predicates {

    private Predicates(){}
	
    public static Predicate<Object> alwaysTrue(){
		return AlwaysTruePredicate.INSTANCE;
	}
	
    public static Predicate<Object> alwaysFalse() {
		return AlwaysFalsePredicate.INSTANCE;
	}
	
	@SafeVarargs
    public static Predicate<Object> or(Predicate<Object>...filters) {
		return new OrPredicate(filters);
	}
	
	public static Predicate<Object> or(Iterable<Predicate<Object>> filters) {
		return new OrPredicate(ImmutableList.copyOf(filters));
	}
	
	@SafeVarargs
    public static Predicate<Object> and(Predicate<Object>...filters) {
		return new AndPredicate(filters);
	}
	
	public static Predicate<Object> and(Iterable<Predicate<Object>> filters){
		return new AndPredicate(ImmutableList.copyOf(filters));
	}
	
	public static Predicate<Object> not(Predicate<Object> filter) {
		return new NotPredicate(filter);
	}
}
