package com.google.iot.m2m.base;

import org.checkerframework.checker.nullness.qual.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

public abstract class AbstractResourceLink<T> extends ResourceLink<T> {
    private final Map<Listener<T>, Executor> mListenerMap = new HashMap<>();

    protected final void didChangeValue(@Nullable T value) {
        synchronized (mListenerMap) {
            for (Map.Entry<Listener<T>, Executor> entry : mListenerMap.entrySet()) {
                entry.getValue().execute(() -> entry.getKey().onResourceLinkChanged(this, value));
            }
        }
    }

    protected abstract void onListenerCountChanged(int listeners);

    @Override
    public final void registerListener(Executor executor, Listener<T> listener) {
        synchronized (mListenerMap) {
            mListenerMap.put(listener, executor);
            onListenerCountChanged(mListenerMap.size());
        }
    }

    @Override
    public final void unregisterListener(Listener<T> listener) {
        synchronized (mListenerMap) {
            if (mListenerMap.remove(listener) != null) {
                onListenerCountChanged(mListenerMap.size());
            }
        }
    }
}
