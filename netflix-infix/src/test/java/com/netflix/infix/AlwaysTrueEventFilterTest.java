package com.netflix.infix;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.netflix.infix.VerificationUtil.DUMMY_INPUT;
import static org.junit.Assert.assertTrue;

public class AlwaysTrueEventFilterTest {

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test() {
		assertTrue(AlwaysTruePredicate.INSTANCE.apply(DUMMY_INPUT));
	}
}
