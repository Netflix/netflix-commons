package com.netflix.infix;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.netflix.infix.VerificationUtil.DUMMY_INPUT;
import static com.netflix.infix.VerificationUtil.getFalseFilter;
import static com.netflix.infix.VerificationUtil.getTrueFilter;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class NotEventFilterTest {
	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testNotTrueIsFalse() {
		assertFalse(new NotPredicate(getTrueFilter()).apply(DUMMY_INPUT));
	}
	
	@Test
	public void testNotFalseIsTrue() {
		assertTrue(new NotPredicate(getFalseFilter()).apply(DUMMY_INPUT));
	}
}
