package com.netflix.infix.lang.infix.antlr;

import org.antlr.runtime.tree.Tree;

class TreeNodeUtil {
	private TreeNodeUtil(){}
	
	public static String getXPath(Tree pathNode) {
	    ValueTreeNode xpathNode = (ValueTreeNode)pathNode;
		
	    return (String)xpathNode.getValue();
    }
}
