package com.netflix.infix.lang.infix.antlr;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import com.netflix.infix.Predicates;

import org.antlr.runtime.Token;
import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.tree.Tree;

public class OrTreeNode extends CommonTree implements PredicateTranslatable {

	@Override
	@SuppressWarnings("unchecked")
	public Predicate<Object> translate() {
		return Predicates.or(
                Lists.transform(getChildren(), new Function<Object, Predicate<Object>>() {
                    @Override
                    public Predicate<Object> apply(Object input) {
                        PredicateTranslatable node = (PredicateTranslatable) input;
                        return node.translate();
                    }
                })
        );
	}

	public OrTreeNode(Token t) {
		super(t);
	} 

	public OrTreeNode(OrTreeNode node) {
		super(node);
	} 

	public Tree dupNode() {
		return new OrTreeNode(this);
	} // for dup'ing type

	public String toString() {
		return String.format("%s<%s>", token.getText(), getClass().getSimpleName());
	}
}
