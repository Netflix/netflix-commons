package com.netflix.infix.lang.infix.antlr;

import org.antlr.runtime.Token;
import org.antlr.runtime.tree.CommonTree;

public abstract class PredicateBaseTreeNode extends CommonTree {
	public PredicateBaseTreeNode(Token t) {
		super(t);
	} 

	public PredicateBaseTreeNode(PredicateBaseTreeNode node) {
	    super(node);
    }

	public String toString() {
		return String.format("%s<%s>", getText(), getClass().getSimpleName());
	}
}
