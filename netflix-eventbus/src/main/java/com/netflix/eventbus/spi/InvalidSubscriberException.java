package com.netflix.eventbus.spi;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

/**
 * Thrown if the subscriber registered with the {@link EventBus} is invalid. A subscriber will be
 * invalid if
 * <ul>
 <li>The method annotated with {@link Subscribe} does not contain one and only one argument.</li>
 <li>The method uses a batching strategy other than {@link com.netflix.eventbus.spi.Subscribe.BatchingStrategy#None} and
 does not have the argument as {@link Iterable}</li>
 <li>If the subscriber uses batching and does not provide a batch size &gt; 1</li>
 <li>If the subscriber uses batching and does not provide a batch age.</li>
 </ul>
 *
 * @author Nitesh Kant (nkant@netflix.com)
 */
public class InvalidSubscriberException extends Exception {

    private static final long serialVersionUID = 4258884942423525335L;

    private Class<?> subscriberClass;
    private Set<Method> offendingMethods;

    public InvalidSubscriberException(Class<?> subscriberClass, Map<Method, String> errors) {
        super(getErrorMessage(subscriberClass, errors));
        this.subscriberClass = subscriberClass;
        this.offendingMethods = errors.keySet();
    }

    public Set<Method> getOffendingMethods() {
        return offendingMethods;
    }

    public Class<?> getSubscriberClass() {
        return subscriberClass;
    }

    private static String getErrorMessage(Class<?> subscriberClass, Map<Method, String> errors) {
        StringBuilder errMsgBuilder = new StringBuilder();
        errMsgBuilder.append("Invalid subscriber class: ");
        errMsgBuilder.append(subscriberClass);
        errMsgBuilder.append(". Errors: \n");
        for (Map.Entry<Method, String> anEntry : errors.entrySet()) {
            errMsgBuilder.append("Method: ");
            errMsgBuilder.append(anEntry.getKey().toGenericString());
            errMsgBuilder.append(" is invalid. Error: ");
            errMsgBuilder.append(anEntry.getValue());
            errMsgBuilder.append("\n");
        }
        return errMsgBuilder.toString();
    }
}
