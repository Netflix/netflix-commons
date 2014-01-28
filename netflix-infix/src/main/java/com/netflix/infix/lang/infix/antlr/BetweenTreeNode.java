package com.netflix.infix.lang.infix.antlr;

import com.google.common.base.Predicate;
import com.netflix.infix.Predicates;
import com.netflix.infix.NumericValuePredicate;
import com.netflix.infix.PathValueEventFilter;

import org.antlr.runtime.Token;
import org.antlr.runtime.tree.Tree;

public class BetweenTreeNode extends PredicateBaseTreeNode implements PredicateTranslatable {

	@Override
	public Predicate<Object> translate() {
		ValueTreeNode xpathNode = (ValueTreeNode)getChild(0);
		String xpath = (String)xpathNode.getValue(); 
		
		ValueTreeNode lowerBoundNode = (ValueTreeNode)getChild(1);
		Number lowerBound = (Number)lowerBoundNode.getValue();
		
		ValueTreeNode upperBoundNode = (ValueTreeNode)getChild(2);
		Number upperBound = (Number)upperBoundNode.getValue(); 
		
		return Predicates.and(
                new PathValueEventFilter(xpath, new NumericValuePredicate(lowerBound, ">=")),
                new PathValueEventFilter(xpath, new NumericValuePredicate(upperBound, "<"))
        );
		
	}

	public BetweenTreeNode(Token t) {
		super(t);
	} 

	public BetweenTreeNode(BetweenTreeNode node) {
		super(node);
	} 

	public Tree dupNode() {
		return new BetweenTreeNode(this);
	} 
}
