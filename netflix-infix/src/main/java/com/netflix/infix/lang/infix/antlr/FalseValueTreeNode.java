package com.netflix.infix.lang.infix.antlr;

import com.google.common.base.Predicate;
import com.netflix.infix.Predicates;

import org.antlr.runtime.Token;
import org.antlr.runtime.tree.Tree;

public class FalseValueTreeNode extends PredicateBaseTreeNode implements ValueTreeNode, PredicateTranslatable {

	@Override
	public Object getValue() {
		return Boolean.FALSE;
	}

	public FalseValueTreeNode(Token t) {
		super(t);
	} 

	public FalseValueTreeNode(FalseValueTreeNode node) {
		super(node);
	} 

	public Tree dupNode() {
		return new FalseValueTreeNode(this);
	}

	@Override
    public Predicate<Object> translate() {
	    return Predicates.alwaysFalse();
    } 
}
