package com.netflix.infix;

import static com.netflix.infix.VerificationUtil.DUMMY_INPUT;
import static org.junit.Assert.assertFalse;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class AlwaysFalseEventFilterTest {

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testAlwaysFalse() {
		assertFalse(AlwaysFalsePredicate.INSTANCE.apply(DUMMY_INPUT));
	}
}
