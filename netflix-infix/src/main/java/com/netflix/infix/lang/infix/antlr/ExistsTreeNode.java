package com.netflix.infix.lang.infix.antlr;

import com.google.common.base.Predicate;
import com.netflix.infix.PathExistsEventFilter;

import org.antlr.runtime.Token;
import org.antlr.runtime.tree.Tree;

import static com.netflix.infix.lang.infix.antlr.TreeNodeUtil.getXPath;

public class ExistsTreeNode extends PredicateBaseTreeNode implements PredicateTranslatable {

	@Override
	public Predicate<Object> translate() {
		return new PathExistsEventFilter(getXPath(getChild(0)));
	}

	public ExistsTreeNode(Token t) {
		super(t);
	} 

	public ExistsTreeNode(ExistsTreeNode node) {
		super(node);
	} 

	public Tree dupNode() {
		return new ExistsTreeNode(this);
	} 
}
