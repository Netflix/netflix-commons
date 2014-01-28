package com.netflix.infix.lang.infix.antlr;

import com.google.common.base.Predicate;
import com.netflix.infix.NullValuePredicate;
import com.netflix.infix.PathValueEventFilter;

import org.antlr.runtime.Token;
import org.antlr.runtime.tree.Tree;

import static com.netflix.infix.lang.infix.antlr.TreeNodeUtil.getXPath;

public class NullTreeNode extends PredicateBaseTreeNode implements PredicateTranslatable {

	@Override
	public Predicate<Object> translate() {
		String xpath = getXPath(getChild(0));
		
		return new PathValueEventFilter(xpath, NullValuePredicate.INSTANCE);
	}

	public NullTreeNode(Token t) {
		super(t);
	} 

	public NullTreeNode(NullTreeNode node) {
		super(node);
	} 

	public Tree dupNode() {
		return new NullTreeNode(this);
	} 
}
