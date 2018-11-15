/*
 * Copyright (C) 2018 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.iot.smcp;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.iot.coap.LocalEndpoint;
import com.google.iot.coap.Message;
import com.google.iot.coap.Transaction;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import org.checkerframework.checker.nullness.qual.Nullable;

/** Decorator class for wrapping a CoAP transaction in a future. */
class TransactionFuture<T> extends java.util.concurrent.FutureTask<T>
        implements ListenableFuture<T> {
    private final Transaction mTransaction;
    private Map<Runnable, Executor> mListeners = null;

    TransactionFuture(Transaction transaction) {
        super(() -> null);
        mTransaction = transaction;
        mTransaction.registerCallback(
                new Transaction.Callback() {
                    @Override
                    public void onTransactionCancelled() {
                        cancel(false);
                    }

                    @Override
                    public void onTransactionException(Exception exception) {
                        setException(exception);
                    }

                    @Override
                    public void onTransactionResponse(LocalEndpoint endpoint, Message response) {
                        try {
                            TransactionFuture.this.onTransactionResponse(endpoint, response);
                        } catch (Exception x) {
                            setException(x);
                        }
                    }

                    @Override
                    public void onTransactionFinished() {
                        set(null);
                    }
                });
    }

    @Override
    @CanIgnoreReturnValue
    public boolean cancel(boolean mayInterruptIfRunning) {
        mTransaction.cancel();
        return super.cancel(mayInterruptIfRunning);
    }

    @Override
    public T get(long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        try {
            return super.get(timeout, unit);
        } catch (TimeoutException x) {
            if (mTransaction.isMulticast()) {
                // We eat the timeout and return null if our transaction is multicast.
                return null;
            }
            throw x;
        }
    }

    @SuppressWarnings("RedundantThrows")
    <E extends Throwable> void onTransactionResponse(LocalEndpoint endpoint, Message response)
            throws E {
        set(null);
    }

    @Override
    protected synchronized void done() {
        super.done();

        if (mListeners != null) {
            mListeners.forEach((listener, executor) -> executor.execute(listener));
        }
    }

    @SuppressWarnings("EmptyMethod")
    @Override
    protected void set(@Nullable T t) {
        super.set(t);
    }

    @SuppressWarnings("EmptyMethod")
    @Override
    protected void setException(Throwable t) {
        super.setException(t);
    }

    @Override
    public synchronized void addListener(Runnable listener, Executor executor) {
        if (isDone()) {
            executor.execute(listener);
        } else {
            if (mListeners == null) {
                mListeners = new HashMap<>();
            }
            mListeners.put(listener, executor);
        }
    }
}
