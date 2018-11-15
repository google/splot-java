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

import com.google.iot.coap.*;
import com.google.iot.m2m.base.*;
import com.google.iot.m2m.trait.BaseTrait;
import java.io.IOException;
import java.io.StringReader;
import java.net.SocketAddress;
import java.net.URI;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.checkerframework.checker.nullness.qual.Nullable;

class SmcpDiscoveryQuery extends DiscoveryQuery {
    private static final boolean DEBUG = false;
    private static final Logger LOGGER =
            Logger.getLogger(SmcpDiscoveryQuery.class.getCanonicalName());

    private final SmcpTechnology mTechnology;
    private final Transaction mTransaction;
    private final long mTimeoutInMs;
    private final HashSet<FunctionalEndpoint> mFunctionalEndpoints = new HashSet<>();
    private Future<?> mTimeout;
    private Exception mException = null;
    private Listener mListener = null;
    private Executor mListenerExecutor;
    private final int mMaxResults;

    private ScheduledExecutorService getExecutor() {
        return mTechnology.getExecutor();
    }

    SmcpDiscoveryQuery(
            SmcpTechnology technology, Transaction transaction, long timeoutInMs, int maxResults) {
        mTechnology = technology;
        mTransaction = transaction;
        mTimeoutInMs = timeoutInMs;
        mMaxResults = maxResults;

        mTransaction.registerCallback(
                new Transaction.Callback() {
                    @Override
                    public void onTransactionResponse(LocalEndpoint endpoint, Message response) {
                        SmcpDiscoveryQuery.this.onTransactionResponse(endpoint, response);
                    }

                    @Override
                    public void onTransactionException(Exception exception) {
                        if (exception instanceof TechnologyException
                                || exception instanceof TechnologyRuntimeException) {
                            mException = exception;

                        } else if (exception instanceof RuntimeException) {
                            mException = new TechnologyRuntimeException(exception);

                        } else {
                            mException = new TechnologyException(exception);
                        }
                    }

                    @Override
                    public void onTransactionFinished() {
                        stop();
                    }
                });
    }

    @Override
    public void restart() {
        if (isDone()) {
            if (mTimeout != null) {
                mTimeout.cancel(false);
            }
            mException = null;
            synchronized (mFunctionalEndpoints) {
                mFunctionalEndpoints.clear();
            }
            mTransaction.restart();
            mTimeout = getExecutor().schedule(this::stop, mTimeoutInMs, TimeUnit.MILLISECONDS);
        }
    }

    private void onTransactionResponse(LocalEndpoint endpoint, Message response) {
        if (isDone()) {
            // If the discovery query is stopped, do nothing.
            return;
        }

        if (response.getCode() != Code.RESPONSE_CONTENT) {
            LOGGER.warning("Got error response during discovery (ignoring): " + response);
            return;
        }

        Integer contentFormat = response.getOptionSet().getContentFormat();

        if (contentFormat != null && ContentFormat.APPLICATION_LINK_FORMAT != contentFormat) {
            LOGGER.warning("Got bad content format during discovery (ignoring): " + response);
            return;
        }

        if (DEBUG) LOGGER.info("Got discovery response: " + response);

        StringReader reader = new StringReader(response.getPayloadAsString());
        Message request = mTransaction.getRequest();
        Map<String, String> queryFilter = request.getOptionSet().getUriQueriesAsMap();

        SocketAddress remoteSocketAddress = response.getRemoteSocketAddress();

        if (remoteSocketAddress == null) {
            throw new SmcpRuntimeException("Response had null remoteSocketAddress");
        }

        URI baseUri = endpoint.createUriFromSocketAddress(remoteSocketAddress);
        URI relativeUri = request.getOptionSet().getUri();

        if (relativeUri != null && relativeUri.getPath() != null) {
            baseUri = baseUri.resolve(relativeUri.getPath());
        }

        try {
            Map<URI, Map<String, String>> results = LinkFormat.parseLinkFormat(reader, queryFilter);

            if (DEBUG) LOGGER.info("Parsed link format: " + results);

            for (Map.Entry<URI, Map<String, String>> entry : results.entrySet()) {
                final URI anchorUri;
                final Map<String, String> params = entry.getValue();

                if (params.containsKey(LinkFormat.PARAM_ANCHOR)) {
                    anchorUri = baseUri.resolve(params.get(LinkFormat.PARAM_ANCHOR));
                } else {
                    anchorUri = baseUri;
                }

                if (DEBUG) LOGGER.info("Found " + anchorUri.resolve(entry.getKey()));

                FunctionalEndpoint fe =
                        mTechnology.getFunctionalEndpointForNativeUri(
                                anchorUri.resolve(entry.getKey()));

                if (fe instanceof SmcpFunctionalEndpoint) {
                    SmcpFunctionalEndpoint sfe = ((SmcpFunctionalEndpoint) fe);
                    if (params.containsKey(LinkFormat.PARAM_ENDPOINT_NAME)) {
                        sfe.updateCachedPropertyValue(
                                BaseTrait.META_UID, params.get(LinkFormat.PARAM_ENDPOINT_NAME));
                    }
                    if (params.containsKey(LinkFormat.PARAM_TITLE)) {
                        sfe.updateCachedPropertyValue(
                                BaseTrait.META_NAME, params.get(LinkFormat.PARAM_TITLE));
                    }
                }

                if (fe != null) {
                    synchronized (mFunctionalEndpoints) {
                        mFunctionalEndpoints.add(fe);

                        if (mListener != null) {
                            final Listener listener = mListener;
                            mListenerExecutor.execute(
                                    () -> listener.onDiscoveryQueryFoundFunctionalEndpoint(fe));
                        }
                        if (mMaxResults > 0 && mFunctionalEndpoints.size() >= mMaxResults) {
                            if (DEBUG) LOGGER.warning("Got maximum result count " + mMaxResults);
                            stop();
                            break;
                        }
                    }
                }
            }
        } catch (IOException | LinkFormatParseException x) {
            if (DEBUG) LOGGER.log(Level.INFO, "Unable to handle response" + response, x);
        }
    }

    @Override
    public Set<FunctionalEndpoint> get() throws InterruptedException, TechnologyException {
        if (!mTimeout.isCancelled()) {
            try {
                mTimeout.get(mTimeoutInMs, TimeUnit.MILLISECONDS);

            } catch (CancellationException | TimeoutException ignored) {
                // Ignored.

            } catch (ExecutionException x) {
                // This shouldn't happen.
                throw new TechnologyRuntimeException(x);
            }
        }

        if (mException instanceof TechnologyException) {
            throw (TechnologyException) mException;

        } else if (mException instanceof RuntimeException) {
            throw (RuntimeException) mException;

        } else if (mException != null) {
            throw new TechnologyException(mException);
        }

        synchronized (mFunctionalEndpoints) {
            return new HashSet<>(mFunctionalEndpoints);
        }
    }

    @Override
    public void stop() {
        mTimeout.cancel(false);
        synchronized (mFunctionalEndpoints) {
            if (mListener != null) {
                final Listener listener = mListener;
                mListenerExecutor.execute(listener::onDiscoveryQueryIsDone);
            }
        }
    }

    @Override
    public boolean isDone() {
        return mTimeout == null || mTimeout.isDone();
    }

    @Override
    public void setListener(Executor executor, @Nullable Listener listener) {
        boolean isDone = isDone();
        synchronized (mFunctionalEndpoints) {
            mListenerExecutor = executor;
            mListener = listener;
            if (listener != null) {
                mFunctionalEndpoints.forEach(
                        (fe) ->
                                mListenerExecutor.execute(
                                        () ->
                                                listener.onDiscoveryQueryFoundFunctionalEndpoint(
                                                        fe)));
                if (isDone) {
                    mListenerExecutor.execute(listener::onDiscoveryQueryIsDone);
                }
            }
        }
    }
}
