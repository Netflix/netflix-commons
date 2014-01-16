package com.netflix.infix.lang.infix.antlr;

import com.google.common.base.Predicate;
import com.netflix.infix.Predicates;

import org.antlr.runtime.Token;
import org.antlr.runtime.tree.Tree;

public class TrueValueTreeNode extends PredicateBaseTreeNode implements PredicateTranslatable {

	public TrueValueTreeNode(Token t) {
		super(t);
	} 

	public TrueValueTreeNode(TrueValueTreeNode node) {
		super(node);
	} 

	public Tree dupNode() {
		return new TrueValueTreeNode(this);
	}

	@Override
    public Predicate<Object> translate() {
	   return Predicates.alwaysTrue();
    } 
}
