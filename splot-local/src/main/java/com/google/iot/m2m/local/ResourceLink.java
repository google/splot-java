package com.google.iot.m2m.local;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.iot.m2m.base.InvalidPropertyValueException;
import com.google.iot.m2m.base.Technology;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.net.URI;
import java.util.concurrent.Executor;

/**
 * Generic, RESTful interface to an arbitrary resource.
 *
 * <p>{@code ResourceLink}s provide a way for automation primitives (like AutomationPairings and
 * AutomationRules) to perform RESTful operations without knowing any details about
 * the underlying transport.
 *
 * <p>Users of the Splot API should not have any need to use {@code ResourceLink} objects
 * directly: they are usually only used by local functional endpoints that need to
 * perform RESTful operations.
 *
 * <p>{@code ResourceLink}s are managed by {@link ResourceLinkManager} instances, which provides
 * a method for looking up a {@link URI} to a {@code ResourceLink}. Thus, a
 * given {@code ResourceLink} instance is always tied to a specific {@link ResourceLinkManager}
 * instance (which is typically also a {@link Technology} instance).
 *
 * <p>Local functional endpoints which need to perform RESTful operations on arbitrary
 * URIs should expose a property which takes a URI as an argument. That URI can then
 * be used to look up the {@code ResourceLink} object that allows RESTful operations to be performed
 * on it. Because of this, Local functional endpoints which use ResourceLinks need to
 * have a reference to the {@link Technology} that is hosting it.
 *
 * @see ResourceLinkManager#getResourceLinkForUri(URI)
 */
public abstract class ResourceLink<T> {

    public interface Listener<T> {
        /**
         * Called whenever the value of the {@link ResourceLink} has changed.
         *
         * @param rl the resource link that is reporting the change of value
         * @param value the reported value
         */
        void onResourceLinkChanged(ResourceLink<T> rl, @Nullable T value);
    }

    public static <T> ResourceLink<Object> stripType(ResourceLink<T> resourceLink, Class<T> clazz) {
        return new ResourceLink<Object>() {

            @SuppressWarnings("unchecked")
            @Override
            public ListenableFuture<Object> fetchValue() {
                return (ListenableFuture<Object>)resourceLink.fetchValue();
            }

            @Override
            public ListenableFuture<?> invoke(@Nullable Object value) {
                if (value != null && !clazz.isInstance(value)) {
                    return Futures.immediateFailedFuture(
                            new InvalidPropertyValueException("Bad type"));
                }
                return resourceLink.invoke(clazz.cast(value));
            }

            @Override
            public URI getUri() {
                return resourceLink.getUri();
            }

            @SuppressWarnings("unchecked")
            @Override
            public void registerListener(Executor executor, Listener<Object> listener) {
                resourceLink.registerListener(executor, (Listener<T>)listener);
            }

            @SuppressWarnings("unchecked")
            @Override
            public void unregisterListener(Listener<Object> listener) {
                resourceLink.unregisterListener((Listener<T>)listener);
            }
        };
    }

    public abstract ListenableFuture<T> fetchValue();

    @CanIgnoreReturnValue
    public abstract ListenableFuture<?> invoke(@Nullable T value);

    public abstract URI getUri();

    public abstract void registerListener(Executor executor, Listener<T> listener);

    public abstract void unregisterListener(Listener<T> listener);
}
