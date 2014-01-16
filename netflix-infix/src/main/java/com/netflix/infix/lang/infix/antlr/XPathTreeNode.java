package com.netflix.infix.lang.infix.antlr;

import org.antlr.runtime.Token;
import org.antlr.runtime.tree.Tree;

public class XPathTreeNode extends PredicateBaseTreeNode implements ValueTreeNode {

	@Override
	public Object getValue() {
		return getChild(0).getText();
	}

	public XPathTreeNode(Token t) {
		super(t);
	} 

	public XPathTreeNode(XPathTreeNode node) {
		super(node);
	} 

	public Tree dupNode() {
		return new XPathTreeNode(this);
	} 
}
