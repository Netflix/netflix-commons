package com.netflix.util.batch;

import java.util.List;
import java.util.concurrent.ExecutorService;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;

/**
 * Batching strategy that makes an inline call to the callback for every insert.
 * The call contains a list of 1 element and therefore does no batching at all.
 * 
 * @author elandau
 */
public class InlineNoBatchPolicy implements BatchingPolicy {
    
    public static class Builder {
        public InlineNoBatchPolicy build() {
            return new InlineNoBatchPolicy();
        }
    }
    
    @Override
    public <T> Batcher<T> create(final Function<List<T>, Boolean> callback, ExecutorService executor) {
        return new Batcher<T>() {
            @Override
            public void add(T object) {
                callback.apply(ImmutableList.of(object));
            }

            @Override
            public void flush() {
                // Nothing to flush here
            }

            @Override
            public void add(List<T> batch) {
                callback.apply(batch);
            }
        };
    }
}
