package com.netflix.infix;

import static com.netflix.infix.Predicates.alwaysFalse;
import static com.netflix.infix.Predicates.alwaysTrue;
import static com.netflix.infix.Predicates.and;
import static com.netflix.infix.Predicates.not;
import static com.netflix.infix.Predicates.or;
import static com.netflix.infix.VerificationUtil.DUMMY_INPUT;
import static com.netflix.infix.VerificationUtil.getFalseFilter;
import static com.netflix.infix.VerificationUtil.getTrueFilter;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;

import com.google.common.base.Predicate;
import com.netflix.infix.RegexValuePredicate.MatchPolicy;

public class EventFiltersTest {
	@Test
	public void testAlwaysFalseReturnsFalse() {
		assertFalse(alwaysFalse().apply(DUMMY_INPUT));
	}

	@Test
	public void testAlwaysTrueReturnsTrue() {
		assertTrue(alwaysTrue().apply(DUMMY_INPUT));
	}
	
	@Test
	public void testNotAlwaysNegates(){
		assertTrue(not(getFalseFilter()).apply(DUMMY_INPUT));
		
		assertFalse(not(getTrueFilter()).apply(DUMMY_INPUT));
	}
	
	@Test
	public void testOr() {
		assertTrue(or(getFalseFilter(), getFalseFilter(), getTrueFilter()).apply(DUMMY_INPUT));
		
		assertFalse(or(getFalseFilter(), getFalseFilter()).apply(DUMMY_INPUT));
	}
	
	@Test
	public void testAnd(){
		assertTrue(and(getTrueFilter(), getTrueFilter()).apply(DUMMY_INPUT));
		
		assertFalse(and(getTrueFilter(), getFalseFilter()).apply(DUMMY_INPUT));
	}
	
	@Test
	public void showQuery() throws Exception {
		Predicate<Object> filter = Predicates.or(
			Predicates.and(
				new PathValueEventFilter("//path/to/property", new StringValuePredicate("foo")),
				new PathValueEventFilter("//path/to/property", new NumericValuePredicate(123, ">")),
				new PathValueEventFilter("//path/to/property", new XPathValuePredicate("//path/to/property", "//another/path"))
			), 
			Predicates.not(
				new PathValueEventFilter("//path/to/time", new TimeMillisValuePredicate("yyyy-MM-dd", "1997-08-29", "!="))
			), 
			new PathValueEventFilter("//path/to/stringProp", new RegexValuePredicate(".*", MatchPolicy.PARTIAL)),
			new PathValueEventFilter("//path/to/stringProp", new RegexValuePredicate(".*", MatchPolicy.FULL))
		);
		
		print(filter);
	}

	private void print(Predicate<Object> filter) throws IOException {
	    System.out.println(filter.toString());
    }
}
