package com.netflix.infix.lang.infix.antlr;

import com.google.common.base.Predicate;
import com.netflix.infix.*;

import org.antlr.runtime.Token;
import org.antlr.runtime.tree.Tree;

import static com.netflix.infix.lang.infix.antlr.EventFilterParser.*;
import static com.netflix.infix.lang.infix.antlr.TreeNodeUtil.getXPath;

public abstract class EqualityComparisonBaseTreeNode extends PredicateBaseTreeNode {

	public EqualityComparisonBaseTreeNode(Token t) {
		super(t);
	}

	public EqualityComparisonBaseTreeNode(PredicateBaseTreeNode node) {
		super(node);
	}

	// TODO this is an ugly workaround. We should really generate ^(NOT ^(Equals...) for NOT_EQUAL
	// but I can't get ANTLR to generated nested tree with added node.
	protected Predicate<Object> getEqualFilter() {
        String xpath = getXPath(getChild(0)); 
    	
    	Tree valueNode = getChild(1);
    	
    	switch(valueNode.getType()){
    	case NUMBER:
    		Number value = (Number)((ValueTreeNode)valueNode).getValue();
    		return new PathValueEventFilter(xpath, new NumericValuePredicate(value, "="));
    	case STRING:
    		String sValue = (String)((ValueTreeNode)valueNode).getValue();
    		return new PathValueEventFilter(xpath, new StringValuePredicate(sValue));
    	case TRUE:
    		return new PathValueEventFilter(xpath, BooleanValuePredicate.TRUE);
    	case FALSE:
    		return new PathValueEventFilter(xpath, BooleanValuePredicate.FALSE);
    	case NULL:
    		return new PathValueEventFilter(xpath, NullValuePredicate.INSTANCE);
    	case XPATH_FUN_NAME:
    		String aPath = (String)((ValueTreeNode)valueNode).getValue();
    		return new PathValueEventFilter(xpath, new XPathValuePredicate(aPath, xpath));
    	case TIME_MILLIS_FUN_NAME:
    		TimeMillisValueTreeNode timeNode = (TimeMillisValueTreeNode)valueNode;
    		return new PathValueEventFilter(xpath, 
    			new TimeMillisValuePredicate(
    				timeNode.getValueFormat(), 
    				timeNode.getValue(), 
    				"="));
    	case TIME_STRING_FUN_NAME:
    		TimeStringValueTreeNode timeStringNode = (TimeStringValueTreeNode)valueNode;
    		return new PathValueEventFilter(xpath, 
    			new TimeStringValuePredicate(
    				timeStringNode.getValueTimeFormat(), 
    				timeStringNode.getInputTimeFormat(),
    				timeStringNode.getValue(),
    				"="));
    	default:
    		throw new UnexpectedTokenException(valueNode, "Number", "String", "TRUE", "FALSE");
    	}
    }

}
