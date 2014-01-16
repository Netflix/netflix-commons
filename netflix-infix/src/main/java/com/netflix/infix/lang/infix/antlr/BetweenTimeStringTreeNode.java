package com.netflix.infix.lang.infix.antlr;

import com.google.common.base.Predicate;
import com.netflix.infix.Predicates;
import com.netflix.infix.PathValueEventFilter;
import com.netflix.infix.TimeStringValuePredicate;

import org.antlr.runtime.Token;
import org.antlr.runtime.tree.Tree;

public class BetweenTimeStringTreeNode extends PredicateBaseTreeNode implements PredicateTranslatable {

	@Override
	public Predicate<Object> translate() {
		ValueTreeNode xpathNode = (ValueTreeNode)getChild(0);
		String xpath = (String)xpathNode.getValue(); 
		
		TimeStringValueTreeNode lowerBoundNode = (TimeStringValueTreeNode)getChild(1);
		
		TimeStringValueTreeNode upperBoundNode = (TimeStringValueTreeNode)getChild(2);
		
		return Predicates.and(
                new PathValueEventFilter(
                        xpath,
                        new TimeStringValuePredicate(
                                lowerBoundNode.getValueTimeFormat(),
                                lowerBoundNode.getInputTimeFormat(),
                                lowerBoundNode.getValue(),
                                ">=")),
                new PathValueEventFilter(
                        xpath,
                        new TimeStringValuePredicate(
                                upperBoundNode.getValueTimeFormat(),
                                upperBoundNode.getInputTimeFormat(),
                                upperBoundNode.getValue(),
                                "<"))
        );
		
	}

	public BetweenTimeStringTreeNode(Token t) {
		super(t);
	} 

	public BetweenTimeStringTreeNode(BetweenTimeStringTreeNode node) {
		super(node);
	} 

	public Tree dupNode() {
		return new BetweenTimeStringTreeNode(this);
	} 
}
