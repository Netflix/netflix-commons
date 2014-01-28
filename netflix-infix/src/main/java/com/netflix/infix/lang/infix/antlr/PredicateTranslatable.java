package com.netflix.infix.lang.infix.antlr;

import com.google.common.base.Predicate;

public interface PredicateTranslatable {
	public Predicate<Object> translate();
}
