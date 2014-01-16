package com.netflix.infix.lang.infix.antlr;

import org.antlr.runtime.Token;
import org.antlr.runtime.tree.Tree;

public class NullValueTreeNode extends PredicateBaseTreeNode implements ValueTreeNode {

	@Override
	public Object getValue() {
		return null;
		
	}

	public NullValueTreeNode(Token t) {
		super(t);
	} 

	public NullValueTreeNode(NullValueTreeNode node) {
		super(node);
	} 

	public Tree dupNode() {
		return new NullValueTreeNode(this);
	} 
}
