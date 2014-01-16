package com.netflix.infix.lang.infix.antlr;

import com.google.common.base.Predicate;
import com.netflix.infix.Predicates;
import com.netflix.infix.PathValueEventFilter;
import com.netflix.infix.TimeMillisValuePredicate;

import org.antlr.runtime.Token;
import org.antlr.runtime.tree.Tree;

public class BetweenTimeMillisTreeNode extends PredicateBaseTreeNode implements PredicateTranslatable {

	@Override
	public Predicate<Object> translate() {
		ValueTreeNode xpathNode = (ValueTreeNode)getChild(0);
		String xpath = (String)xpathNode.getValue(); 
		
		TimeMillisValueTreeNode lowerBoundNode = (TimeMillisValueTreeNode)getChild(1);
		
		TimeMillisValueTreeNode upperBoundNode = (TimeMillisValueTreeNode)getChild(2);
		
		return Predicates.and(
                new PathValueEventFilter(
                        xpath,
                        new TimeMillisValuePredicate(lowerBoundNode.getValueFormat(), lowerBoundNode.getValue(), ">=")),
                new PathValueEventFilter(
                        xpath,
                        new TimeMillisValuePredicate(upperBoundNode.getValueFormat(), upperBoundNode.getValue(), "<"))
        );
		
	}

	public BetweenTimeMillisTreeNode(Token t) {
		super(t);
	} 

	public BetweenTimeMillisTreeNode(BetweenTimeMillisTreeNode node) {
		super(node);
	} 

	public Tree dupNode() {
		return new BetweenTimeMillisTreeNode(this);
	} 
}
