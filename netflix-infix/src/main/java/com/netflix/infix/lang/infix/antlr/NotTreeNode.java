package com.netflix.infix.lang.infix.antlr;

import com.google.common.base.Predicate;
import com.netflix.infix.Predicates;

import org.antlr.runtime.Token;
import org.antlr.runtime.tree.Tree;

public class NotTreeNode extends PredicateBaseTreeNode implements PredicateTranslatable {

	@Override
	public Predicate<Object> translate() {
	    Predicate<Object> filter = ((PredicateTranslatable)getChild(0)).translate();
		
		return Predicates.not(filter);
	}

	public NotTreeNode(Token t) {
		super(t);
	} 

	public NotTreeNode(NotTreeNode node) {
		super(node);
	} 

	public Tree dupNode() {
		return new NotTreeNode(this);
	} 
}
