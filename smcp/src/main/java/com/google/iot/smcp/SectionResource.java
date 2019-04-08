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
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.google.iot.cbor.*;
import com.google.iot.coap.*;
import com.google.iot.m2m.base.*;
import com.google.iot.m2m.local.ResourceLink;
import com.google.iot.m2m.local.SectionResourceLink;
import com.google.iot.m2m.trait.TransitionTrait;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.logging.Logger;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.json.JSONException;
import org.json.JSONObject;

import static com.google.iot.m2m.base.Modifier.convertFromQuery;

/* TODO: Lambdas cause compilation problems in Android for this file. Figure out why. */

@SuppressWarnings("Convert2Lambda")
class SectionResource extends Resource<InboundRequestHandler> {
    private static final boolean DEBUG = false;
    private static final Logger LOGGER = Logger.getLogger(SectionResource.class.getCanonicalName());

    private final FunctionalEndpoint mFe;
    private final Section mSection;
    private final ResourceLink<Map<String,Map<String,Object>>> mResourceLink;
    private final Executor mExecutor;
    private int mMaxAge;

    SectionResource(FunctionalEndpoint fe, Section section, Executor executor) {
        mFe = fe;
        mSection = section;
        mExecutor = executor;
        mResourceLink = SectionResourceLink.createForSection(fe, section);

        final Observable observable = getObservable();

        mResourceLink.registerListener(mExecutor,
                new ResourceLink.Listener<Map<String,Map<String,Object>>>() {
                    @Override
                    public void onResourceLinkChanged(
                            ResourceLink<Map<String, Map<String, Object>>> rl,
                            @Nullable Map<String, Map<String, Object>> value) {
                        observable.trigger();
                    }
        });

        switch (mSection) {
            case STATE:
                mMaxAge = 30;
                break;
            case METADATA:
                mMaxAge = 60 * 10;
                break;
            case CONFIG:
                mMaxAge = 60 * 60;
                break;
        }
    }

    class EmptyResponseHandler implements Runnable {
        final ListenableFuture<?> mFuture;
        final InboundRequest mInboundRequest;

        EmptyResponseHandler(InboundRequest inboundRequest, ListenableFuture<?> future) {
            mInboundRequest = inboundRequest;
            mFuture = future;
            mInboundRequest.responsePending();
        }

        @Override
        public void run() {
            try {
                mFuture.get();

            } catch (InterruptedException x) {
                mInboundRequest.sendSimpleResponse(Code.RESPONSE_SERVICE_UNAVAILABLE);
                Thread.currentThread().interrupt();
                return;

            } catch (ExecutionException x) {
                if (x.getCause() instanceof PropertyNotFoundException) {
                    mInboundRequest.sendSimpleResponse(Code.RESPONSE_NOT_FOUND);
                } else if (x.getCause() instanceof PropertyException) {
                    String message = x.getCause().getMessage();
                    if (message == null || message.isEmpty()) {
                        message = x.getCause().toString();
                    }
                    mInboundRequest.sendSimpleResponse(Code.RESPONSE_FORBIDDEN, message);
                    LOGGER.info("Property exception: " + x.getCause());
                } else {
                    LOGGER.warning("Unhandled exception: " + x);
                    mInboundRequest.sendSimpleResponse(
                            Code.RESPONSE_INTERNAL_SERVER_ERROR, x.getCause().toString());
                }
                return;

            } catch (RuntimeException x) {
                LOGGER.warning("Uncaught runtime exception: " + x);
                x.printStackTrace();
                mInboundRequest.sendSimpleResponse(
                        Code.RESPONSE_INTERNAL_SERVER_ERROR, x.toString());
                return;
            }

            mInboundRequest.success();
        }
    }

    @Override
    public void onParentRequest(InboundRequest inboundRequest, boolean trailingSlash) {
        Message request = inboundRequest.getMessage();

        if (DEBUG) LOGGER.info("onParentRequest " + request);

        if (checkForUnsupportedOptions(inboundRequest)) {
            return;
        }

        if (request.getCode() == Code.METHOD_GET) {
            onSpecificGetRequest(inboundRequest, trailingSlash);
        } else if (request.getCode() == Code.METHOD_POST) {
            onSpecificPostRequest(inboundRequest);
        } else {
            inboundRequest.sendSimpleResponse(Code.RESPONSE_METHOD_NOT_ALLOWED);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Map<String, Object>> castToMapOfMaps(Map<String, ?> payload)
            throws IllegalArgumentException {
        for (Map.Entry<String, ?> entry : payload.entrySet()) {
            String k = entry.getKey();
            Object v = entry.getValue();
            if (!(v instanceof Map)) {
                throw new IllegalArgumentException("Unexpected type for trait " + k);
            }
        }

        return (Map<String, Map<String, Object>>)payload;
    }

    private void onSpecificPostRequest(InboundRequest inboundRequest) {
        Message request = inboundRequest.getMessage();

        if (DEBUG) LOGGER.info("onSpecificPostRequest " + request);

        Map<String, Map<String, Object>> content;

        try {
            Integer contentFormat = request.getOptionSet().getContentFormat();

            if (contentFormat == null) {
                contentFormat = ContentFormat.TEXT_PLAIN_UTF8;
            }

            if (!request.hasPayload()) {
                inboundRequest.sendSimpleResponse(Code.RESPONSE_BAD_REQUEST, "No payload");
            }

            Map<String, Object> payload;

            switch (contentFormat) {
                case ContentFormat.APPLICATION_CBOR:
                    CborMap cborPayload = CborMap.createFromCborByteArray(request.getPayload());
                    payload = cborPayload.toNormalMap();
                    break;

                case ContentFormat.APPLICATION_JSON:
                case ContentFormat.TEXT_PLAIN_UTF8:
                    JSONObject jsonPayload = new JSONObject(request.getPayloadAsString());

                    // JSONObject.toMap() is unavailable on Android, so we use
                    // CBOR instead to create the map:
                    payload = CborMap.createFromJSONObject(jsonPayload).toNormalMap();
                    break;

                default:
                    inboundRequest.sendSimpleResponse(Code.RESPONSE_UNSUPPORTED_CONTENT_FORMAT);
                    return;
            }

            content = castToMapOfMaps(payload);

        } catch (CborConversionException | CborParseException
                | JSONException | IllegalArgumentException x) {
            if (DEBUG) LOGGER.warning("Parsing exception: " + x);
            inboundRequest.sendSimpleResponse(Code.RESPONSE_BAD_REQUEST, x.toString());
            return;

        } catch (RuntimeException x) {
            LOGGER.warning("Uncaught runtime exception: " + x);
            x.printStackTrace();
            inboundRequest.sendSimpleResponse(Code.RESPONSE_INTERNAL_SERVER_ERROR, x.toString());
            throw x;
        }

        URI uri = request.getOptionSet().getUri();

        // Handle any modifiers
        if (uri != null) {
            Modifier[] modifierList;

            try {
                modifierList = convertFromQuery(uri.getQuery());
            } catch (InvalidModifierListException x) {
                inboundRequest.sendSimpleResponse(
                        Code.RESPONSE_BAD_REQUEST, x.getMessage());
                return;
            }

            for (Modifier modifier : modifierList) {
                if ((modifier instanceof Modifier.Duration)
                        && Section.STATE.equals(mSection)) {
                    // Duration modifier basically collapses to an additional parameter
                    // if we are the state section. It is ignored (like all other modifiers)
                    // if this is applied to any other section.
                    double dur = ((Modifier.Duration)modifier).getDuration();
                    content.computeIfAbsent(
                            TransitionTrait.TRAIT_ID,
                            new Function<String, Map<String, Object>>() {
                                @Override
                                public Map<String, Object> apply(String ignore) {
                                    return new HashMap<>();
                                }
                            }).put(Splot.PARAM_DURATION, dur);
                }
            }
        }

        if (DEBUG) LOGGER.info("onSpecificPostRequest Applying " + content);

        inboundRequest.responsePending();

        ListenableFuture<?> future = mResourceLink.invoke(content);
        future.addListener(new EmptyResponseHandler(inboundRequest, future), mExecutor);
    }

    private void onSpecificGetRequest(InboundRequest inboundRequest, boolean trailingSlash) {

        if (DEBUG) LOGGER.info("onSpecificGetRequest " + inboundRequest.getMessage());

        final ListenableFuture<Map<String, Map<String, Object>>> future;

        future = mResourceLink.fetchValue();

        inboundRequest.responsePending();

        future.addListener(
                new Runnable() {
                    @Override
                    public void run() {
                        Message request = inboundRequest.getMessage();

                        final OptionSet optionSet = request.getOptionSet();
                        final CborMap value;

                        try {
                            value = CborMap.createFromJavaObject(future.get());
                            MutableMessage response = request.createResponse(Code.RESPONSE_CONTENT);
                            int accept = -1;
                            if (optionSet.hasAccept()) {
                                //noinspection ConstantConditions
                                accept = optionSet.getAccept();
                            }
                            switch (accept) {
                                case ContentFormat.APPLICATION_CBOR:
                                    response.getOptionSet()
                                            .setContentFormat(ContentFormat.APPLICATION_CBOR)
                                            .addEtag(Etag.createFromInteger(value.hashCode()));
                                    response.setPayload(value.toCborByteArray());
                                    break;

                                case -1:
                                case ContentFormat.TEXT_PLAIN_UTF8:
                                    response.getOptionSet()
                                            .setContentFormat(ContentFormat.TEXT_PLAIN_UTF8)
                                            .addEtag(Etag.createFromInteger(value.hashCode()));
                                    response.setPayload(value.toJsonString());
                                    break;

                                case ContentFormat.APPLICATION_JSON:
                                    response.getOptionSet()
                                            .setContentFormat(ContentFormat.APPLICATION_JSON)
                                            .addEtag(Etag.createFromInteger(value.hashCode()));
                                    response.setPayload(value.toJsonString());
                                    break;

                                case ContentFormat.APPLICATION_LINK_FORMAT:
                                    response.getOptionSet()
                                            .setContentFormat(ContentFormat.APPLICATION_LINK_FORMAT)
                                            .addEtag(Etag.createFromInteger(value.hashCode()));
                                    LinkFormat.Builder builder = new LinkFormat.Builder();

                                    if (DEBUG) {
                                        builder.setAddNewlines(true);
                                    }

                                    final String prefix;

                                    if (trailingSlash) {
                                        prefix = "";
                                    } else {
                                        prefix = mSection.id + "/";
                                    }

                                    for (String traitKey : value.keySetAsStrings()) {
                                        CborObject traitObj = value.get(traitKey);

                                        if (!(traitObj instanceof CborMap)) {
                                            builder.addLink(URI.create(prefix + traitKey))
                                                    .setValue(traitObj.toString());
                                        } else {
                                            CborMap traitMap = (CborMap) traitObj;
                                            for (String propKey : traitMap.keySetAsStrings()) {
                                                CborObject propValue = traitMap.get(propKey);
                                                String propValueString;
                                                if (propValue instanceof CborTextString) {
                                                    propValueString =
                                                            ((CborTextString) propValue)
                                                                    .stringValue();
                                                } else {
                                                    propValueString = propValue.toJsonString();
                                                }
                                                LinkFormat.LinkBuilder linkBuilder =
                                                        builder.addLink(
                                                                URI.create(
                                                                        prefix + traitKey + "/"
                                                                                + propKey));
                                                if (propValueString.length() < 16) {
                                                    // Only include the value in the link format
                                                    // if it is short.
                                                    linkBuilder.setValue(propValue.toString());
                                                }
                                                linkBuilder.addInterfaceDescription(
                                                        Smcp.IF_DESC_PROP);
                                                linkBuilder.addContentFormat(
                                                        ContentFormat.APPLICATION_CBOR);
                                                linkBuilder.addContentFormat(
                                                        ContentFormat.APPLICATION_JSON);
                                                linkBuilder.addContentFormat(
                                                        ContentFormat.TEXT_PLAIN_UTF8);
                                            }
                                        }
                                    }
                                    response.setPayload(builder.toString());
                                    break;
                                default:
                                    response.setCode(Code.RESPONSE_NOT_ACCEPTABLE);
                                    break;
                            }

                            response.addOption(Option.MAX_AGE, mMaxAge);

                            inboundRequest.sendResponse(response);

                        } catch (InterruptedException x) {
                            inboundRequest.sendSimpleResponse(Code.RESPONSE_SERVICE_UNAVAILABLE);
                            Thread.currentThread().interrupt();

                        } catch (ExecutionException x) {
                            if (x.getCause() instanceof PropertyNotFoundException) {
                                inboundRequest.sendSimpleResponse(Code.RESPONSE_NOT_FOUND);
                            } else if (x.getCause() instanceof PropertyException) {
                                inboundRequest.sendSimpleResponse(
                                        Code.RESPONSE_FORBIDDEN, x.getCause().toString());

                            } else if (x.getCause() instanceof TechnologyException) {
                                inboundRequest.sendSimpleResponse(
                                        Code.RESPONSE_INTERNAL_SERVER_ERROR, x.toString());

                            } else if (x.getCause() instanceof RuntimeException) {
                                LOGGER.warning("Unhandled runtime exception: " + x);
                                throw new UncheckedExecutionException(x);

                            } else {
                                inboundRequest.sendSimpleResponse(
                                        Code.RESPONSE_INTERNAL_SERVER_ERROR,
                                        x.getCause().toString());
                            }

                        } catch (CborConversionException x) {
                            LOGGER.warning("CBOR encoding exception: " + x);
                            x.printStackTrace();
                            inboundRequest.sendSimpleResponse(
                                    Code.RESPONSE_INTERNAL_SERVER_ERROR, x.toString());

                        }
                    }
                },
                mExecutor);
    }

    private void onTraitRequest(
            InboundRequest inboundRequest,
            @SuppressWarnings("unused") Section section,
            @SuppressWarnings("unused") String trait) {
        if (DEBUG) LOGGER.info("onTraitRequest " + inboundRequest.getMessage());
        // TODO: Writeme!
        inboundRequest.sendSimpleResponse(Code.RESPONSE_NOT_IMPLEMENTED);
    }

    private final ConcurrentMap<String, Observable> mPropertyObservables =
            new ConcurrentHashMap<>();

    private Observable getObservableForProp(Section section, String trait, String prop) {
        String propString = section.id + "/" + trait + "/" + prop;

        return mPropertyObservables.computeIfAbsent(
                propString,
                new Function<String, Observable>() {
                    @Override
                    public Observable apply(String k) {
                        final Observable obs = new Observable();

                        final PropertyKey<Object> propKey = new PropertyKey<>(k, Object.class);

                        final PropertyListener<Object> listener =
                                (fe, property, value) -> obs.trigger();

                        obs.registerCallback(
                                new Observable.Callback() {
                                    @Override
                                    public void onHasRemoteObservers(Observable observable) {
                                        if (DEBUG) LOGGER.info("onHasRemoteObservers: " + k);
                                        mFe.registerPropertyListener(mExecutor, propKey, listener);
                                    }

                                    @Override
                                    public void onNoRemoteObservers(Observable observable) {
                                        if (DEBUG) LOGGER.info("onNoRemoteObservers: " + k);
                                        mFe.unregisterPropertyListener(propKey, listener);
                                    }
                                });

                        return obs;
                    }
                });
    }

    private boolean handleObservable(
            InboundRequest inboundRequest, Section section, String trait, String prop) {
        return getObservableForProp(section, trait, prop).handleInboundRequest(inboundRequest);
    }

    private void onPropRequest(
            InboundRequest inboundRequest, Section section, String trait, String prop) {
        Message request = inboundRequest.getMessage();
        if (DEBUG) LOGGER.info("onPropRequest " + request);

        if (checkForUnsupportedOptions(inboundRequest)) {
            return;
        }

        if (request.getCode() == Code.METHOD_GET) {
            onPropGetRequest(inboundRequest, section, trait, prop);
        } else if (request.getCode() == Code.METHOD_POST) {
            onPropPostRequest(inboundRequest, section, trait, prop);
        } else if (request.getCode() == Code.METHOD_PUT) {
            onPropPutRequest(inboundRequest, section, trait, prop);
        } else {
            inboundRequest.sendSimpleResponse(Code.RESPONSE_METHOD_NOT_ALLOWED);
        }
    }

    private void onPropPostRequest(
            InboundRequest inboundRequest, Section section, String trait, String prop) {
        Message request = inboundRequest.getMessage();
        if (DEBUG) LOGGER.info("onPropPostRequest " + request);

        Object content;

        try {
            content = Utils.getObjectFromPayload(request);
        } catch (ResponseException x) {
            inboundRequest.sendSimpleResponse(x.getCode());
            return;
        }

        URI uri = request.getOptionSet().getUri();
        ListenableFuture<?> future;
        Modifier.Mutation method = null;
        Modifier[] modifierList = Modifier.EMPTY_LIST;

        if (uri != null) {
            try {
                modifierList = convertFromQuery(uri.getQuery());
                method = Modifier.getMutation(modifierList);

            } catch (InvalidModifierListException x) {
                inboundRequest.sendSimpleResponse(
                        Code.RESPONSE_BAD_REQUEST, x.getMessage());
                return;
            }
        }

        if (method instanceof Modifier.Increment) {
            PropertyKey<Number> key = new PropertyKey<>(section, trait, prop, Number.class);

            if (content == null) {
                future = mFe.incrementProperty(key, 1, modifierList);
            } else if (content instanceof Number) {
                future = mFe.incrementProperty(key, (Number) content, modifierList);
            } else {
                inboundRequest.sendSimpleResponse(
                        Code.RESPONSE_BAD_REQUEST, "Increment value not a number");
                return;
            }

        } else if (method instanceof Modifier.Toggle) {
            PropertyKey<Boolean> key = new PropertyKey<>(section, trait, prop, Boolean.class);
            future = mFe.toggleProperty(key, modifierList);

        } else if (method instanceof Modifier.Insert) {
            PropertyKey<Object[]> key = new PropertyKey<>(section, trait, prop, Object[].class);

            if (content == null) {
                inboundRequest.sendSimpleResponse(
                        Code.RESPONSE_BAD_REQUEST, "Missing value to add");
                return;
            }

            future = mFe.insertValueIntoProperty(key, content, modifierList);

        } else if (method instanceof Modifier.Remove) {
            PropertyKey<Object[]> key = new PropertyKey<>(section, trait, prop, Object[].class);

            if (content == null) {
                inboundRequest.sendSimpleResponse(
                        Code.RESPONSE_BAD_REQUEST, "Missing value to remove");
                return;
            }
            future = mFe.removeValueFromProperty(key, content, modifierList);

        } else {
            PropertyKey<Object> key = new PropertyKey<>(section, trait, prop, Object.class);
            future = mFe.setProperty(key, content, modifierList);

        }

        inboundRequest.responsePending();

        future.addListener(new EmptyResponseHandler(inboundRequest, future), mExecutor);
    }

    private void onPropPutRequest(
            InboundRequest inboundRequest,
            @SuppressWarnings("unused") Section section,
            @SuppressWarnings("unused") String trait,
            @SuppressWarnings("unused") String prop) {
        if (DEBUG) LOGGER.info("onPropPutRequest " + inboundRequest.getMessage());

        // Currently, we do everything with GET, POST, and DELETE.
        inboundRequest.sendSimpleResponse(Code.RESPONSE_METHOD_NOT_ALLOWED,
                "PUT not implemented");
    }

    private void onPropGetRequest(
            InboundRequest inboundRequest, Section section, String trait, String prop) {
        Message request = inboundRequest.getMessage();
        if (DEBUG) LOGGER.info("onPropGetRequest " + request);

        if (handleObservable(inboundRequest, section, trait, prop)) {
            // Handling the observable ended up finishing the request. We are done.
            return;
        }

        inboundRequest.responsePending();

        URI uri = request.getOptionSet().getUri();
        Modifier[] modifierList = Modifier.EMPTY_LIST;

        if (uri != null) {
            try {
                modifierList = convertFromQuery(uri.getQuery());
            } catch (InvalidModifierListException x) {
                inboundRequest.sendSimpleResponse(
                        Code.RESPONSE_BAD_REQUEST, x.getMessage());
                return;
            }
        }

        ListenableFuture<Object> future =
                mFe.fetchProperty(
                        new PropertyKey<>(section, trait, prop, Object.class),
                        modifierList);

        future.addListener(
                new Runnable() {
                    @Override
                    public void run() {
                        final OptionSet optionSet = request.getOptionSet();
                        final CborObject value;

                        // If our inbound request is no longer active, then
                        // we can return early.
                        if (inboundRequest.isDone()) {
                            return;
                        }

                        try {
                            value = CborObject.createFromJavaObject(future.get());

                            MutableMessage response = request.createResponse(Code.RESPONSE_CONTENT);
                            int accept = -1;
                            if (optionSet.hasAccept()) {
                                //noinspection ConstantConditions
                                accept = optionSet.getAccept();
                            }
                            switch (accept) {
                                case ContentFormat.APPLICATION_CBOR:
                                    response.getOptionSet()
                                            .setContentFormat(ContentFormat.APPLICATION_CBOR)
                                            .addEtag(Etag.createFromInteger(value.hashCode()));
                                    response.setPayload(value.toCborByteArray());
                                    break;
                                case -1:
                                case ContentFormat.TEXT_PLAIN_UTF8:
                                    response.getOptionSet()
                                            .setContentFormat(ContentFormat.TEXT_PLAIN_UTF8)
                                            .addEtag(Etag.createFromInteger(value.hashCode()));
                                    response.setPayload(value.toJsonString());
                                    break;
                                case ContentFormat.APPLICATION_JSON:
                                    response.getOptionSet()
                                            .setContentFormat(ContentFormat.APPLICATION_JSON)
                                            .addEtag(Etag.createFromInteger(value.hashCode()));
                                    response.setPayload("{\"v\":" + value.toJsonString() + "}");
                                    break;
                                default:
                                    response.setCode(Code.RESPONSE_NOT_ACCEPTABLE);
                                    break;
                            }

                            response.addOption(Option.MAX_AGE, mMaxAge);

                            inboundRequest.sendResponse(response);

                        } catch (InterruptedException x) {
                            inboundRequest.sendSimpleResponse(Code.RESPONSE_SERVICE_UNAVAILABLE);
                            Thread.currentThread().interrupt();

                        } catch (ExecutionException x) {
                            if (x.getCause() instanceof PropertyNotFoundException) {
                                inboundRequest.sendSimpleResponse(Code.RESPONSE_NOT_FOUND);
                            } else if (x.getCause() instanceof PropertyException) {
                                inboundRequest.sendSimpleResponse(
                                        Code.RESPONSE_FORBIDDEN, x.getCause().toString());
                            } else {
                                LOGGER.warning("Unhandled exception: " + x);
                                x.printStackTrace();
                                inboundRequest.sendSimpleResponse(
                                        Code.RESPONSE_INTERNAL_SERVER_ERROR,
                                        x.getCause().toString());
                            }
                        } catch (CborConversionException x) {
                            LOGGER.warning("Unable to convert property value to CBOR object: " + x);
                            inboundRequest.sendSimpleResponse(
                                    Code.RESPONSE_INTERNAL_SERVER_ERROR, x.toString());
                        } catch (RuntimeException x) {
                            LOGGER.warning("Uncaught runtime exception: " + x);
                            x.printStackTrace();
                            inboundRequest.sendSimpleResponse(
                                    Code.RESPONSE_INTERNAL_SERVER_ERROR, x.toString());
                        }
                    }
                },
                mExecutor);
    }

    @Override
    public void onUnknownChildRequest(InboundRequest inboundRequest, String childName) {
        if (DEBUG) LOGGER.info("onUnknownChildRequest " + inboundRequest.getMessage());

        final int originalOptionIndex = inboundRequest.getCurrentOptionIndex();

        inboundRequest.rewindOneOption();

        @Nullable Option traitOption = inboundRequest.nextOptionWithNumber(Option.URI_PATH);
        @Nullable Option propOption = inboundRequest.nextOptionWithNumber(Option.URI_PATH);

        if ((traitOption == null)
                || (inboundRequest.nextOptionWithNumber(Option.URI_PATH) != null)) {
            inboundRequest.setCurrentOptionIndex(originalOptionIndex);
            super.onUnknownChildRequest(inboundRequest, childName);
        } else if (propOption == null) {
            onTraitRequest(inboundRequest, mSection, traitOption.stringValue());
        } else {
            onPropRequest(
                    inboundRequest, mSection, traitOption.stringValue(), propOption.stringValue());
        }
    }

    @Override
    public void onUnknownChildRequestCheck(InboundRequest inboundRequest, String childName) {
        /* Do nothing */
    }

    @Override
    public void onBuildLinkParams(LinkFormat.LinkBuilder builder) {
        super.onBuildLinkParams(builder);
        builder.addInterfaceDescription(Smcp.IF_DESC_SECT)
                .addContentFormat(ContentFormat.APPLICATION_JSON)
                .addContentFormat(ContentFormat.APPLICATION_CBOR);
    }
}
