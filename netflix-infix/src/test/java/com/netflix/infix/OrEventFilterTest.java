package com.netflix.infix;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static com.netflix.infix.VerificationUtil.DUMMY_INPUT;
import static com.netflix.infix.VerificationUtil.getFalseFilter;
import static com.netflix.infix.VerificationUtil.getTrueFilter;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class OrEventFilterTest {
	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testAllRejectionsLeadToRejection() {
		List<? extends Predicate<Object>> filters = ImmutableList.of(getFalseFilter(), getFalseFilter(), getFalseFilter());

		OrPredicate filter = new OrPredicate(filters);
		assertFalse(filter.apply(DUMMY_INPUT));
	}

	@Test
	public void testOneAcceptanceLeadsToAcceptance() {
		List<? extends Predicate<Object>> filters = ImmutableList.of(getFalseFilter(), getTrueFilter(), getFalseFilter());

		OrPredicate filter = new OrPredicate(filters);
		assertTrue(filter.apply(DUMMY_INPUT));
	}

	@Test
	public void testOrEventFilterShortcuts() {
		Predicate<Object> falseFilter = getFalseFilter();

		Predicate<Object> trueFilter = getTrueFilter();

		List<? extends Predicate<Object>> filters = ImmutableList.of(trueFilter, falseFilter);

		assertTrue(new OrPredicate(filters).apply(DUMMY_INPUT));
		verify(falseFilter, never()).apply(DUMMY_INPUT);
	}
}
