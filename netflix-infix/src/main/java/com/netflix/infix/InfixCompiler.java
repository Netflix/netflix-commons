package com.netflix.infix;

import com.google.common.base.Predicate;
import com.netflix.infix.lang.infix.antlr.EventFilterParser;
import com.netflix.infix.lang.infix.antlr.PredicateTranslatable;

/**
 * Compile an INFIX string into a Predicate
 * 
 * @author elandau
 */
public class InfixCompiler implements PredicateCompiler {
    public Predicate<Object> compile(String input) throws Exception {
        EventFilterParser parser               = EventFilterParser.createParser(input);
        EventFilterParser.filter_return result = parser.filter();
        
        return ((PredicateTranslatable) result.getTree()).translate();
    }
}
