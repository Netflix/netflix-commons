package com.netflix.infix;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

public class NotPredicate implements Predicate<Object> {

	final private Predicate<Object> notPredicate;

	public NotPredicate(Predicate<Object> predicate) {
        this.notPredicate = Predicates.not(predicate);
	}

    @Override
    public boolean apply(Object input) {
        return notPredicate.apply(input);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("NotEventFilter");
        sb.append("{notPredicate=").append(notPredicate);
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

        NotPredicate that = (NotPredicate) o;

        if (notPredicate != null ? !notPredicate.equals(that.notPredicate) : that.notPredicate != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return notPredicate != null ? notPredicate.hashCode() : 0;
    }
}
