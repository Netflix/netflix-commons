/**
 * An infix notation based language for event filters. At a higher level, one creates an expression made out of multiple
 * "predicate"s chained together using the AND and OR operators.
 * <h3>Predicate</h3> A predicate by definition is an expression that results in a boolean value. There are multiple
 * predicates available in the language which are described below. On top of these predicates one can also use
 * comparison functions, described below, which results in a boolean value.
 * <h6>Between Predicate: </h6> A between predicate checks whether a value is between a lower and upper bound.
 * <br/><i>Example: "xpath("//a/b/c") between (100,150)". Here xpath() is a function which is described below.</i>
 * <b>This is only supported for numeric inputs.</b>
 * <h6>In Predicate: </h6> An in predicate checks whether a value is in a list of values provided to the function.
 * <b>This is supported for alphanumeric inputs.</b>
 * <br/><i>Example: "xpath("//a/b/c") in ("x", "y", "z")". Here xpath() is a function which is described below.</i>
 * <h6>Null Predicate: </h6> A null predicate checks whether the passed value is null.
 * <br/><i>Example: "xpath("//a/b/c") is null". Here xpath() is a function which is described below.</i>
 * <h6>Regex Predicate: </h6> A null predicate checks whether the passed value matches a provided regex.
 * <br/><i>Example: "xpath("//a/b/c") =~ "<a regex>"". Here xpath() is a function which is described below.</i>
 * <h6>Exists Predicate: </h6> A null predicate checks whether the passed value matches a provided regex.
 * <br/><i>Example: "xpath("//a/b/c") =~ "<a regex>"". Here xpath() is a function which is described below.</i>
 *
 * <h6>Comparison Functions</h6> This language currently supports the following comparison functions:
 * <ul>
 * <li>Equals: <br/><i>Example: "xpath("//a/b/c") = "xyz". Here xpath() is a function which is described below.</i></li>
 * <li>Not Equals: <br/><i>Example: "xpath("//a/b/c") != "xyz". Here xpath() is a function which is described below.</i></li>
 * <li>Greater than<br/><i>Example: "xpath("//a/b/c") > "xyz". Here xpath() is a function which is described below.</i></li>
 * <li>Greater than or equals<br/><i>Example: "xpath("//a/b/c") >= "xyz". Here xpath() is a function which is described below.</i></li>
 * <li>Lesser than<br/><i>Example: "xpath("//a/b/c") < "xyz". Here xpath() is a function which is described below.</i></li>
 * <li>Lesser than or equals<br/><i>Example: "xpath("//a/b/c") <= "xyz". Here xpath() is a function which is described below.</i></li>
 * </ul>
 * <b>All the above comparison functions are available only for numeric inputs.</b>
 *
 * <h6>XPath function</h6> This primarily is the only function that fetches runtime values in the filters. The XPath
 * will be run on the event object for which the filter is applied. For details about the evaluation and support
 * <a href="http://commons.apache.org/jxpath/">see this</a>
 *
 * <h6>Other value functions</h6> We also have certain functions that helps convert values in specific formats. These
 * functions are listed below:
 * <ul>
 <li>Time millis: Converts milliseconds since epoch to a date-time string. The format of date-time pattern is as
 specified <a href="http://joda-time.sourceforge.net/apidocs/org/joda/time/format/DateTimeFormat.html">here</a>
 <br/><i>Example: "xpath("//a/b/c") = time-millis("yyyy-MM-dd'T'HH:mm:ss:SSS", "2012-08-22T10:14:44:856") . Here xpath() is a function which is described above.</i></li>
 <li>Time string: Converts a stringified time from one format to another. The format of date-time pattern is as
 specified <a href="http://joda-time.sourceforge.net/apidocs/org/joda/time/format/DateTimeFormat.html">here</a>
 <br/><i>Example: "xpath("//a/b/c") = time-string("yyyy-MM-dd'T'hh:mm:ss:SSS","yyyy-MM-dd'T'HH:mm:ss:SSS", "2012-08-22T10:14:44:856") .
 <br/>Here xpath() is a function which is described above.
 <br/> First format argument is the target format.</i></li>
 <br/> Seconds format argument is the input source format.</i></li>
 </ul>
 */
package com.netflix.eventbus.infix.lang.infix;
