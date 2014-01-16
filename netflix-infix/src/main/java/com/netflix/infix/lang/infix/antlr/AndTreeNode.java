package com.netflix.infix.lang.infix.antlr;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import com.netflix.infix.Predicates;

import org.antlr.runtime.Token;
import org.antlr.runtime.tree.Tree;

public class AndTreeNode extends PredicateBaseTreeNode implements PredicateTranslatable {

	@Override
	@SuppressWarnings("unchecked")
	public Predicate<Object> translate() {
		return Predicates.and(
                Lists.transform(getChildren(), new Function<Object, Predicate<Object>>() {
                    @Override
                    public Predicate<Object> apply(Object input) {
                        PredicateTranslatable node = (PredicateTranslatable) input;
                        return node.translate();
                    }
                })
        );
	}

	public AndTreeNode(Token t) {
		super(t);
	} 

	public AndTreeNode(AndTreeNode node) {
		super(node);
	} 

	public Tree dupNode() {
		return new AndTreeNode(this);
	} 
}
