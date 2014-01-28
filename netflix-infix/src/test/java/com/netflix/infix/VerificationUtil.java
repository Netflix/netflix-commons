package com.netflix.infix;

import org.mockito.Mockito;

import com.google.common.base.Predicate;

public class VerificationUtil {
	public static final MockAnnotatable DUMMY_INPUT = Mockito.mock(MockAnnotatable.class);

	private VerificationUtil(){}

	/**
     * Creating mocked filter instead of using AwaysTrueFilter so this test case
     * is independent of other test target.  
     */
    public static Predicate<Object> getTrueFilter() {
        Predicate<Object> trueFilter = Mockito.mock(Predicate.class);
        Mockito.when(trueFilter.apply(VerificationUtil.DUMMY_INPUT)).thenReturn(true);
        return trueFilter;
    }

	public static Predicate<Object> getFalseFilter() {
        Predicate<Object> falseFilter = Mockito.mock(Predicate.class);
        Mockito.when(falseFilter.apply(VerificationUtil.DUMMY_INPUT)).thenReturn(false);
        return falseFilter;
    }
}
