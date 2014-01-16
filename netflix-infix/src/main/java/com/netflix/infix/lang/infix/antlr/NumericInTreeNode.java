package com.netflix.infix.lang.infix.antlr;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import com.netflix.infix.Predicates;
import com.netflix.infix.NumericValuePredicate;
import com.netflix.infix.PathValueEventFilter;

import org.antlr.runtime.Token;
import org.antlr.runtime.tree.Tree;

import java.util.List;

import static com.netflix.infix.lang.infix.antlr.TreeNodeUtil.getXPath;

public class NumericInTreeNode extends PredicateBaseTreeNode implements PredicateTranslatable {

	@SuppressWarnings("unchecked")
    @Override
	public Predicate<Object> translate() {
		final String xpath = getXPath(getChild(0));
		
		List children = getChildren();
		return Predicates.or(
                Lists.transform(children.subList(1, children.size()), new Function<Object, Predicate<Object>>() {
                    @Override
                    public Predicate<Object> apply(Object node) {
                        Number value = ((NumberTreeNode) node).getValue();
                        return new PathValueEventFilter(xpath, new NumericValuePredicate(value, "="));
                    }
                })
        );
	}

	public NumericInTreeNode(Token t) {
		super(t);
	} 

	public NumericInTreeNode(NumericInTreeNode node) {
		super(node);
	} 

	public Tree dupNode() {
		return new NumericInTreeNode(this);
	} 
}
