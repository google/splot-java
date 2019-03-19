package com.google.iot.m2m.local;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.iot.m2m.base.*;
import com.google.iot.m2m.trait.TransitionTrait;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

abstract class PropertyResourceLink<T> extends AbstractResourceLink<T> implements PropertyListener<T> {
    final FunctionalEndpoint mFe;
    final PropertyKey<T> mKey;


    PropertyResourceLink(FunctionalEndpoint fe, PropertyKey<T> key) {
        mFe = fe;
        mKey = key;
    }

    public static <T> ResourceLink<T> create(FunctionalEndpoint fe, PropertyKey<T> key) {
        return new PropertyResourceLink<T>(fe, key) {
            @Override
            public ListenableFuture<?> invoke(@Nullable T value) {
                return mFe.setProperty(mKey, value);
            }
        };
    }

    public static <T> ResourceLink<T> createWithDuration(FunctionalEndpoint fe, PropertyKey<T> key, double duration) {
        return new PropertyResourceLink<T>(fe, key) {
            @Override
            public ListenableFuture<?> invoke(@Nullable T value) {
                Map<String, Object> props = new HashMap<>();
                key.putInMap(props, value);
                TransitionTrait.STAT_DURATION.putInMap(props, (float)duration);

                return mFe.applyProperties(props);
            }
        };
    }

    public static <T extends Number> ResourceLink<T> createIncrement(FunctionalEndpoint fe, PropertyKey<T> key) {
        return new PropertyResourceLink<T>(fe, key) {
            @Override
            public ListenableFuture<?> invoke(@Nullable T value) {
                if (value == null) {
                    try {
                        // Default to incrementing by one.
                        value = mKey.coerce(1);

                    } catch (InvalidValueException e) {
                        // Should never happen.
                        throw new AssertionError("Unable to coerce '1' to " + mKey.getType(), e);
                    }

                    if (value == null) {
                        // Should never happen, checking only to silence warnings.
                        throw new AssertionError("Unable to coerce '1' to " + mKey.getType());
                    }
                }

                return mFe.incrementProperty(mKey, value);
            }
        };
    }

    public static ResourceLink<Boolean> createToggle(FunctionalEndpoint fe, PropertyKey<Boolean> key) {
        return new PropertyResourceLink<Boolean>(fe, key) {
            @Override
            public ListenableFuture<?> invoke(@Nullable Boolean value) {
                return mFe.toggleProperty(mKey);
            }
        };
    }

    public static <T> ResourceLink<T> createInsert(FunctionalEndpoint fe, PropertyKey<T[]> key) {
        return new ResourceLink<T>() {
            @Override
            public ListenableFuture<T> fetchValue() {
                // We can't fetch from insert/remove operations because of
                // the type mismatch.
                return Futures.immediateFailedFuture(new PropertyOperationUnsupportedException());
            }

            @Override
            public ListenableFuture<?> invoke(@Nullable T value) {
                if (value == null) {
                    return Futures.immediateFailedFuture(
                            new InvalidPropertyValueException("Can't add null to property value"));
                } else {
                    return fe.addValueToProperty(key, value);
                }
            }

            @Override
            public void registerListener(Executor executor, Listener listener) {
                /* Does nothing, because we don't implement fetchValue(). */
            }

            @Override
            public void unregisterListener(Listener listener) {
                /* Does nothing, because we don't implement fetchValue(). */
            }
        };
    }

    public static <T> ResourceLink<T> createRemove(FunctionalEndpoint fe, PropertyKey<T[]> key) {
        return new ResourceLink<T>() {
            @Override
            public ListenableFuture<T> fetchValue() {
                // We can't fetch from insert/remove operations because of
                // the type mismatch.
                return Futures.immediateFailedFuture(new PropertyOperationUnsupportedException());
            }

            @Override
            public ListenableFuture<?> invoke(@Nullable T value) {
                if (value == null) {
                    return Futures.immediateFailedFuture(new InvalidPropertyValueException(
                            "Can't remove null from property value"));
                } else {
                    return fe.removeValueFromProperty(key, value);
                }
            }

            @Override
            public void registerListener(Executor executor, Listener listener) {
                /* Does nothing, because we don't implement fetchValue(). */
            }

            @Override
            public void unregisterListener(Listener listener) {
                /* Does nothing, because we don't implement fetchValue(). */
            }
        };
    }

    @Override
    public ListenableFuture<T> fetchValue() {
        return mFe.fetchProperty(mKey);
    }

    @Override
    protected void onListenerCountChanged(int listeners) {
        if (listeners == 0) {
            mFe.unregisterPropertyListener(mKey, this);
        } else if (listeners == 1) {
            mFe.registerPropertyListener(Runnable::run, mKey, this);
        }
    }

    @Override
    public void onPropertyChanged(FunctionalEndpoint fe, PropertyKey<T> key, @Nullable T value) {
        didChangeValue(value);
    }

    @Override
    public String toString() {
        return "<PropertyResourceLink " + mKey + " " + mFe + ">";
    }
}
