package com.netflix.infix;

import com.google.common.base.Predicate;

public interface PredicateCompiler {
    public Predicate<Object> compile(String input) throws Exception;
}
