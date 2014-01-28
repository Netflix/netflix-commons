package com.netflix.infix.lang.infix.antlr;

import com.google.common.base.Predicate;
import com.netflix.infix.Predicates;

import org.antlr.runtime.Token;
import org.antlr.runtime.tree.Tree;

public class NotEqualsTreeNode extends EqualityComparisonBaseTreeNode implements PredicateTranslatable {

	@Override
	public Predicate<Object> translate() {
		return Predicates.not(getEqualFilter());
    }

    public NotEqualsTreeNode(Token t) {
		super(t);
	} 

	public NotEqualsTreeNode(NotEqualsTreeNode node) {
		super(node);
	} 

	public Tree dupNode() {
		return new NotEqualsTreeNode(this);
	} 
}
