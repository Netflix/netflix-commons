package com.netflix.infix.lang.infix.antlr;

import com.google.common.base.Predicate;
import org.antlr.runtime.Token;
import org.antlr.runtime.tree.Tree;

public class EqualsTreeNode extends EqualityComparisonBaseTreeNode implements PredicateTranslatable {

	@Override
	public Predicate<Object> translate() {
		return getEqualFilter();
	}

	public EqualsTreeNode(Token t) {
		super(t);
	} 

	public EqualsTreeNode(EqualsTreeNode node) {
		super(node);
	} 

	public Tree dupNode() {
		return new EqualsTreeNode(this);
	} 
}
