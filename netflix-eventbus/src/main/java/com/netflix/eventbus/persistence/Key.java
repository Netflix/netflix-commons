package com.netflix.eventbus.persistence;

import com.google.common.base.Preconditions;

/**
 * A key for the {@link PersistedData}.
 *
 * @author Nitesh Kant
 */
public class Key {

    public enum AttachmentPoint {

        Subscriber(Constants.SUB_DISPLAY_NAME),
        Event(Constants.EVENT_DISPLAY_NAME);

        private final String displayName;

        AttachmentPoint(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        public static AttachmentPoint fromDisplayName(String displayName) {
            if (displayName.equals(Constants.EVENT_DISPLAY_NAME)) {
                return Event;
            } else if (displayName.equals(Constants.SUB_DISPLAY_NAME)) {
                return Subscriber;
            } else {
                throw new IllegalArgumentException("Illegal attachment point display name: " + displayName);
            }
        }

        private static class Constants {
            public static final String SUB_DISPLAY_NAME = "Sub";
            public static final String EVENT_DISPLAY_NAME = "event";
        }
    }

    private AttachmentPoint attachmentPoint;
    private String subClassName;
    private String subMethodName;
    private String eventClassName;

    public Key(String eventClassName) {
        Preconditions.checkNotNull(eventClassName);
        this.attachmentPoint = AttachmentPoint.Event;
        this.eventClassName = eventClassName;
    }

    public Key(String subClassName, String subMethodName, String eventClassName) {
        Preconditions.checkNotNull(subClassName);
        Preconditions.checkNotNull(subMethodName);
        Preconditions.checkNotNull(eventClassName);
        this.attachmentPoint = AttachmentPoint.Subscriber;
        this.subClassName = subClassName;
        this.subMethodName = subMethodName;
        this.eventClassName = eventClassName;
    }

    public AttachmentPoint getAttachmentPoint() {
        return attachmentPoint;
    }

    public String getSubClassName() {
        return subClassName;
    }

    public String getSubMethodName() {
        return subMethodName;
    }

    public String getEventClassName() {
        return eventClassName;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Key{");
        sb.append("attachmentPoint=").append(attachmentPoint);
        sb.append(", subClassName='").append(subClassName).append('\'');
        sb.append(", subMethodName='").append(subMethodName).append('\'');
        sb.append(", eventClassName='").append(eventClassName).append('\'');
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Key key = (Key) o;

        if (attachmentPoint != key.attachmentPoint) {
            return false;
        }
        if (eventClassName != null ? !eventClassName.equals(key.eventClassName) : key.eventClassName != null) {
            return false;
        }
        if (subClassName != null ? !subClassName.equals(key.subClassName) : key.subClassName != null) {
            return false;
        }
        if (subMethodName != null ? !subMethodName.equals(key.subMethodName) : key.subMethodName != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = attachmentPoint != null ? attachmentPoint.hashCode() : 0;
        result = 31 * result + (subClassName != null ? subClassName.hashCode() : 0);
        result = 31 * result + (subMethodName != null ? subMethodName.hashCode() : 0);
        result = 31 * result + (eventClassName != null ? eventClassName.hashCode() : 0);
        return result;
    }
}
