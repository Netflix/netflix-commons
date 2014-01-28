package com.netflix.infix;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

public class OrPredicate implements Predicate<Object> {

	final private Predicate<Object> orPredicate;

	@SafeVarargs
    public OrPredicate(Predicate<Object>... predicates) {
        this.orPredicate = Predicates.or(predicates);
	}

    public OrPredicate(Iterable<? extends Predicate<Object>> filters) {
        this.orPredicate = Predicates.or(filters);
    }

    @Override
    public boolean apply(Object input) {
        return orPredicate.apply(input);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("OrEventFilter");
        sb.append("{orPredicate=").append(orPredicate);
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        OrPredicate that = (OrPredicate) o;

        if (orPredicate != null ? !orPredicate.equals(that.orPredicate) : that.orPredicate != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return orPredicate != null ? orPredicate.hashCode() : 0;
    }
}
