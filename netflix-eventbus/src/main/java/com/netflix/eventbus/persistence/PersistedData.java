package com.netflix.eventbus.persistence;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;

import java.util.Collections;
import java.util.Set;

/**
 * Data persisted for eventbus as a bean. <br/>
 * This does not contain the context to which this data is attached i.e. which subscriber/publisher this particular data
 * is attached to. This information is contained in {@link Key}
 *
 * @author Nitesh Kant
 */
public class PersistedData {

    public enum Type {EventFilter}

    private Type type;
    private long hash;
    private Set<String> filters;
    private final int version = 1;

    public PersistedData(Type type, String... filters) {
        Preconditions.checkNotNull(filters);
        Preconditions.checkNotNull(type);
        this.type = type;
        this.filters = ImmutableSet.copyOf(filters);
        this.hash = createHash(this);
    }

    public PersistedData(Type type, Set<String> filters) {
        Preconditions.checkNotNull(filters);
        Preconditions.checkNotNull(type);
        this.type = type;
        this.filters = ImmutableSet.copyOf(filters);
        this.hash = createHash(this);
    }

    private PersistedData(Type type) {
        Preconditions.checkNotNull(type);
        this.type = type;
        filters = Collections.emptySet();
    }

    @SuppressWarnings("unused")
    public Type getType() {
        return type;
    }

    @SuppressWarnings("unused")
    public long getHash() {
        return hash;
    }

    /**
     * Returns an unmodifiable set of filters.
     *
     * @return An unmodifiable set of filters.
     */
    public Set<String> getFilters() {
        return filters;
    }

    @SuppressWarnings("unused")
    public int getVersion() {
        return version;
    }

    private static long createHash(PersistedData data) {
        return data.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        PersistedData that = (PersistedData) o;

        if (version != that.version) {
            return false;
        }
        if (filters != null ? !filters.equals(that.filters) : that.filters != null) {
            return false;
        }
        if (type != that.type) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = type != null ? type.hashCode() : 0;
        result = 31 * result + (filters != null ? filters.hashCode() : 0);
        result = 31 * result + version;
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("PersistedData{");
        sb.append("type=").append(type);
        sb.append(", hash='").append(hash).append('\'');
        sb.append(", filters=").append(filters);
        sb.append(", version=").append(version);
        sb.append('}');
        return sb.toString();
    }

    public static class Builder {

        private PersistedData data;

        public Builder(Type type) {
            data = new PersistedData(type);
        }

        @SuppressWarnings("unused")
        public Builder withFilter(String filter) {
            data.filters.add(filter);
            return this;
        }

        public PersistedData build() {
            data.hash = createHash(data);
            data.filters = Collections.unmodifiableSet(data.filters);
            return data;
        }
    }

}
