package com.netflix.infix;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

public class AndPredicate implements Predicate<Object> {

	final private Predicate<Object> andPredicate;

    @SafeVarargs
    public AndPredicate(Predicate<Object>... predicates) {
        this.andPredicate = Predicates.and(predicates);
	}

	public AndPredicate(Iterable<? extends Predicate<Object>> predicates) {
        this.andPredicate = Predicates.and(predicates);
	}

    @Override
    public boolean apply(Object input) {
        return andPredicate.apply(input);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        AndPredicate that = (AndPredicate) o;

        if (andPredicate != null ? !andPredicate.equals(that.andPredicate) : that.andPredicate != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return andPredicate != null ? andPredicate.hashCode() : 0;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("AndEventFilter");
        sb.append("{andPredicate=").append(andPredicate);
        sb.append('}');
        return sb.toString();
    }
}
