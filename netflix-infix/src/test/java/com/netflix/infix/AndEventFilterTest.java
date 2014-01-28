package com.netflix.infix;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;

public class AndEventFilterTest {
	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testAllAcceptancesLeadToAcceptance() {
		List<? extends Predicate<Object>> filters = ImmutableList.of(
			VerificationUtil.getTrueFilter(), 
			VerificationUtil.getTrueFilter(),
			VerificationUtil.getTrueFilter());
		
		AndPredicate filter = new AndPredicate(filters);
		assertTrue(filter.apply(VerificationUtil.DUMMY_INPUT));
	}

	@Test
	public void testOneRejectionLeadsToRejection() {
		List<? extends Predicate<Object>> filters = ImmutableList.of(
			VerificationUtil.getTrueFilter(),
			VerificationUtil.getTrueFilter(),
			VerificationUtil.getFalseFilter()
		);
		
		AndPredicate filter = new AndPredicate(filters);
		assertFalse(filter.apply(VerificationUtil.DUMMY_INPUT));
	}
	
	@Test
	public void testAndEventFilterShortcuts() {
		Predicate<Object> falseFilter = VerificationUtil.getFalseFilter();
		
		Predicate<Object> trueFilter = VerificationUtil.getTrueFilter();
		
		
		List<? extends Predicate<Object>> filters = ImmutableList.of(
			falseFilter, trueFilter
		);
		
		assertFalse(new AndPredicate(filters).apply(VerificationUtil.DUMMY_INPUT));
		verify(trueFilter, never()).apply(VerificationUtil.DUMMY_INPUT);
	}
}
