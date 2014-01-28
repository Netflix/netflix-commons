package com.netflix.infix.lang.infix.antlr;

import com.google.common.base.Predicate;
import com.netflix.infix.NumericValuePredicate;
import com.netflix.infix.PathValueEventFilter;
import com.netflix.infix.TimeMillisValuePredicate;
import com.netflix.infix.TimeStringValuePredicate;

import org.antlr.runtime.Token;
import org.antlr.runtime.tree.Tree;

import static com.netflix.infix.lang.infix.antlr.EventFilterParser.*;
import static com.netflix.infix.lang.infix.antlr.TreeNodeUtil.getXPath;

public class ComparableTreeNode extends PredicateBaseTreeNode implements PredicateTranslatable {

	@Override
	public Predicate<Object> translate() {
		String xpath = getXPath(getChild(0)); 
		
		Tree valueNode = getChild(1);
		
		switch(valueNode.getType()){
		case NUMBER:
			Number value = (Number)((ValueTreeNode)valueNode).getValue();
			return new PathValueEventFilter(xpath, new NumericValuePredicate(value, getToken().getText()));
		case TIME_MILLIS_FUN_NAME:
			TimeMillisValueTreeNode timeValueNode = (TimeMillisValueTreeNode)valueNode;
			return new PathValueEventFilter(
				xpath, 
				new TimeMillisValuePredicate(
					timeValueNode.getValueFormat(), 
					timeValueNode.getValue(), 
					getToken().getText()));
		case TIME_STRING_FUN_NAME:
			TimeStringValueTreeNode timeStringNode = (TimeStringValueTreeNode)valueNode;
			
			return new PathValueEventFilter(
				xpath, 
				new TimeStringValuePredicate(
					timeStringNode.getValueTimeFormat(), 
					timeStringNode.getInputTimeFormat(), 
					timeStringNode.getValue(), 
					getToken().getText()));
		default:
			throw new UnexpectedTokenException(valueNode, "Number", "time-millis", "time-string");
		}
		
	}

	
	public ComparableTreeNode(Token t) {
		super(t);
	} 

	public ComparableTreeNode(ComparableTreeNode node) {
		super(node);
	} 

	public Tree dupNode() {
		return new ComparableTreeNode(this);
	} 
}
