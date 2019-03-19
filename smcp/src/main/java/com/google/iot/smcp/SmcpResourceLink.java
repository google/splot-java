package com.google.iot.smcp;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.iot.cbor.CborConversionException;
import com.google.iot.cbor.CborObject;
import com.google.iot.coap.*;
import com.google.iot.m2m.base.*;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SmcpResourceLink<T> extends AbstractResourceLink<T> {
    private static final boolean DEBUG = false;
    private static final Logger LOGGER =
            Logger.getLogger(SmcpFunctionalEndpoint.class.getCanonicalName());

    private final Client mClient;
    private final Class<T> mClass;
    private Transaction mObserver = null;

    SmcpResourceLink(Client client, Class<T> clazz) {
        mClient = client;
        mClass = clazz;
    }

    public URI getUri() {
        return mClient.getUri();
    }

    @Override
    public ListenableFuture<T> fetchValue() {
        final Transaction transaction =
                mClient.newRequestBuilder()
                        .setOmitUriHostPortOptions(true)
                        .addOption(Option.ACCEPT, ContentFormat.APPLICATION_CBOR)
                        .send();

        return new TransactionFuture<T>(transaction) {
            @Override
            protected void onTransactionResponse(LocalEndpoint endpoint, Message response)
                    throws PropertyException, TechnologyException {

                if (response.getCode() == Code.RESPONSE_CHANGED) {
                    super.onTransactionResponse(endpoint, response);

                } else if (response.getCode() == Code.RESPONSE_CONTENT) {
                    Object value;

                    try {
                        value = Utils.getObjectFromPayload(response);

                    } catch (BadRequestException e) {
                        throw new SmcpException("Invalid response: " + response, e.getCause());

                    } catch (UnsupportedContentFormatException e) {
                        throw new InvalidPropertyValueException(
                                "Unexpected Content-format in response: " + response, e);
                    }

                    try {
                        set(mClass.cast(value));

                    } catch (ClassCastException e) {
                        throw new InvalidPropertyValueException(
                                "Unexpected object type in response: " + response, e);
                    }

                } else {
                    String content = Code.toString(response.getCode());
                    if (response.isPayloadAscii()) {
                        content += ": " + response.getPayloadAsString();
                    }

                    if (response.getCode() == Code.RESPONSE_NOT_FOUND) {
                        throw new PropertyNotFoundException(content);
                    } else if (response.getCode() == Code.RESPONSE_METHOD_NOT_ALLOWED) {
                        throw new PropertyReadOnlyException(content);
                    } else {
                        throw new SmcpException(content);
                    }
                }
            }
        };
    }

    @Override
    public ListenableFuture<?> invoke(@Nullable T value) {
        final Transaction transaction;
        try {
            RequestBuilder requestBuilder =
                    mClient.newRequestBuilder().setCode(Code.METHOD_POST);

            // Reduces packet sizes, but thwarts virtual hosting.
            // In the future we may only want to do this if we are
            // using a direct IP address or a host that is link-local.
            requestBuilder.setOmitUriHostPortOptions(true);

            if (value != null) {
                CborObject payload = CborObject.createFromJavaObject(value);
                requestBuilder
                        .addOptions(
                                new OptionSet()
                                        .setContentFormat(ContentFormat.APPLICATION_CBOR)
                                        .addEtag(Etag.createFromInteger(payload.hashCode())))
                        .setPayload(payload.toCborByteArray());
            }

            transaction = requestBuilder.send();

        } catch (CborConversionException x) {
            return Futures.immediateFailedFuture(x);
        }

        return new TransactionFuture<Void>(transaction) {
            @Override
            protected void onTransactionResponse(LocalEndpoint endpoint, Message response)
                    throws PropertyException, TechnologyException {
                if (response.getCode() == Code.RESPONSE_CHANGED) {
                    super.onTransactionResponse(endpoint, response);
                } else {
                    String content = Code.toString(response.getCode());
                    if (response.isPayloadAscii()) {
                        content += ": " + response.getPayloadAsString();
                    }

                    if (response.getCode() == Code.RESPONSE_NOT_FOUND) {
                        throw new PropertyNotFoundException(content);
                    } else if (response.getCode() == Code.RESPONSE_METHOD_NOT_ALLOWED) {
                        throw new PropertyReadOnlyException(content);
                    } else {
                        throw new SmcpException(content);
                    }
                }
            }
        };
    }

    private void receivedUpdate(Message response) {
        if (response.getCode() != Code.RESPONSE_CONTENT) {
            // This isn't the response we expected.
            LOGGER.warning("While observing " + mClient.getUri() + ", got unexpected message code: " + response);
            return;
        }

        try {
            didChangeValue(mClass.cast(Utils.getObjectFromPayload(response)));

        } catch (ResponseException | ClassCastException e) {
            // This technically is a technology exception, but there is no one checking us.
            // This is a remote error, so we shouldn't throw a runtime exception. Logging
            // the issue seems to be the most reasonable thing. We may need to implement
            // some sort of asynchronous background technology error callback that we can
            // feed these errors into.
            LOGGER.log(
                    Level.WARNING,
                    "Failed to parse payload while observing " + mClient.getUri() + " (" + response + ")",
                    e);
        }
    }

    @Override
    protected void onListenerCountChanged(int listeners) {
        if (listeners == 0) {
            Transaction observer = mObserver;

            if (observer != null) {
                mObserver = null;
                observer.cancel();
            }

        } else if (listeners >= 1 && mObserver == null) {
            mObserver =
                    mClient.newRequestBuilder()
                            .addOption(Option.OBSERVE)
                            .setOmitUriHostPortOptions(true)
                            .addOption(Option.ACCEPT, ContentFormat.APPLICATION_CBOR)
                            .send();

            mObserver.registerCallback(
                    new Transaction.Callback() {
                        @Override
                        public void onTransactionResponse(
                                LocalEndpoint endpoint, Message response) {
                            receivedUpdate(response);
                        }
                    });
        }
    }
}
