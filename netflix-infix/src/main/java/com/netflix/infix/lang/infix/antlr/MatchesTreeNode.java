package com.netflix.infix.lang.infix.antlr;

import com.google.common.base.Predicate;
import com.netflix.infix.PathValueEventFilter;
import com.netflix.infix.RegexValuePredicate;

import org.antlr.runtime.Token;
import org.antlr.runtime.tree.Tree;

import static com.netflix.infix.lang.infix.antlr.TreeNodeUtil.getXPath;

public class MatchesTreeNode extends PredicateBaseTreeNode implements PredicateTranslatable {

	@Override
	public Predicate<Object> translate() {
		String xpath = getXPath(getChild(0)); 
    	
		StringTreeNode valueNode = (StringTreeNode)getChild(1);
    	
    	return new PathValueEventFilter(xpath, new RegexValuePredicate(valueNode.getValue(), RegexValuePredicate.MatchPolicy.FULL));
		
	}

	public MatchesTreeNode(Token t) {
		super(t);
	} 

	public MatchesTreeNode(MatchesTreeNode node) {
		super(node);
	} 

	public Tree dupNode() {
		return new MatchesTreeNode(this);
	} 
}
