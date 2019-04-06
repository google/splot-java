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

import com.google.common.util.concurrent.ExecutionError;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.google.iot.cbor.*;
import com.google.iot.coap.*;
import com.google.iot.m2m.base.*;

import java.net.URI;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * This class handles everything in "f/*".
 *
 * <p>Children of this resource are TraitChildrenResource, which only handles the child functional
 * endpoints for the trait. Method invocations for the mTraits are intercepted and handled by this
 * class.
 */
@SuppressWarnings("Convert2Lambda")
class FuncResource extends Resource<TraitChildrenResource> {
    private static final boolean DEBUG = false;
    private static final Logger LOGGER = Logger.getLogger(FuncResource.class.getCanonicalName());

    private final SmcpTechnology mTechnology;
    private final FunctionalEndpoint mFe;
    private final Set<String> mTraits = new HashSet<>();

    FuncResource(SmcpTechnology technology, FunctionalEndpoint fe) {
        mFe = fe;
        mTechnology = technology;
        ListenableFuture<Map<String, Object>> future = mFe.fetchSection(Splot.Section.METADATA);

        future.addListener(
                new Runnable() {
                    @Override
                    public void run() {
                        Map<String, Object> metadata;

                        try {
                            metadata = Futures.getChecked(future, SmcpException.class);

                        } catch (SmcpException e) {
                            LOGGER.log(Level.WARNING, "Exception", e);
                            return;
                        }

                        for (String key : metadata.keySet()) {
                            String[] components = key.split("/");

                            if (components.length != 3) {
                                continue;
                            }

                            if (mTraits.add(components[1])) {
                                if (DEBUG) LOGGER.info(mFe + " supports trait: " + components[1]);
                                initTrait(components[1]);
                            }
                        }
                    }
                },
                mTechnology.getExecutor());
    }

    private void initTrait(String trait) {

        ListenableFuture<Collection<FunctionalEndpoint>> future;

        future = mFe.fetchChildrenForTrait(trait);

        future.addListener(
                new Runnable() {
                    @Override
                    public void run() {
                        Collection<FunctionalEndpoint> children;
                        try {
                            children = Futures.getChecked(future, SmcpException.class);

                        } catch (SmcpException e) {
                            LOGGER.log(Level.WARNING, "Exception", e);
                            return;
                        }

                        if (children != null) {
                            initTraitWithChildren(trait, children);
                        } else {
                            if (DEBUG)
                                LOGGER.info("Trait " + trait + " does not support children.");
                        }
                    }
                },
                mTechnology.getExecutor());
    }

    private void initTraitWithChildren(String trait, Collection<FunctionalEndpoint> children) {
        TraitChildrenResource resource;

        if (DEBUG) LOGGER.info("Adding children to trait " + trait + ": " + children);

        resource = new TraitChildrenResource(mTechnology, mFe, trait);

        FuncResource.this.addChild(trait, resource);

        for (FunctionalEndpoint cfe : children) {
            String childId = mFe.getIdForChild(cfe);

            if (childId != null) {
                resource.addChild(childId, new HostedFunctionalEndpointAdapter(mTechnology, cfe));

            } else {
                if (DEBUG)
                    LOGGER.info(
                            "Skipping child "
                                    + cfe
                                    + " from trait "
                                    + trait
                                    + " because it has no childId");
            }
        }
    }

    @Override
    public void onBuildLinkParams(LinkFormat.LinkBuilder builder) {
        super.onBuildLinkParams(builder);
        builder.addInterfaceDescription(Smcp.IF_DESC_SECT);
    }

    private void onInvokeMethod(InboundRequest inboundRequest, MethodKey<Object> methodKey) {
        Message request = inboundRequest.getMessage();

        if (DEBUG) LOGGER.info("onInvokeMethod " + request);

        Map<String, Object> content = new HashMap<>();

        if (checkForUnsupportedOptions(inboundRequest)) {
            return;
        }

        if (request.hasPayload()) {
            try {
                Integer contentFormat = request.getOptionSet().getContentFormat();

                if (contentFormat == null) {
                    contentFormat = ContentFormat.TEXT_PLAIN_UTF8;
                }

                switch (contentFormat) {
                    case ContentFormat.APPLICATION_CBOR:
                        CborMap cborPayload = CborMap.createFromCborByteArray(request.getPayload());
                        content.putAll(cborPayload.toNormalMap());
                        break;

                    case ContentFormat.APPLICATION_JSON:
                    case ContentFormat.TEXT_PLAIN_UTF8:
                        JSONObject jsonPayload = new JSONObject(request.getPayloadAsString());

                        // JSONObject.toMap() is unavailable on Android, so we use
                        // CBOR instead to create the map:
                        content.putAll(CborMap.createFromJSONObject(jsonPayload).toNormalMap());
                        break;
                    default:
                        inboundRequest.sendSimpleResponse(Code.RESPONSE_UNSUPPORTED_CONTENT_FORMAT);
                        return;
                }

            } catch (CborConversionException | CborParseException | JSONException x) {
                if (DEBUG) LOGGER.warning("Parsing exception: " + x);
                inboundRequest.sendSimpleResponse(Code.RESPONSE_BAD_REQUEST, x.getMessage() + " " + x);
                return;
            }
        }

        content.putAll(request.getOptionSet().getUriQueriesAsMap());

        if (DEBUG) LOGGER.info("Invoking " + methodKey + " with " + content);

        inboundRequest.responsePending();

        ListenableFuture<Object> future = mFe.invokeMethod(methodKey, content);
        future.addListener(
                new Runnable() {
                    @Override
                    public void run() {
                        Message request = inboundRequest.getMessage();

                        final OptionSet optionSet = request.getOptionSet();
                        Object value;

                        try {
                            value = future.get();

                        } catch (InterruptedException x) {
                            Thread.currentThread().interrupt();
                            inboundRequest.sendSimpleResponse(Code.RESPONSE_SERVICE_UNAVAILABLE);
                            return;

                        } catch (ExecutionException x) {
                            if (x.getCause() instanceof MethodNotFoundException) {
                                inboundRequest.sendSimpleResponse(Code.RESPONSE_NOT_FOUND);

                            } else if (x.getCause() instanceof InvalidMethodArgumentsException) {
                                inboundRequest.sendSimpleResponse(
                                        Code.RESPONSE_BAD_REQUEST, x.getCause().getMessage() + " " + x.getCause());

                            } else if (x.getCause() instanceof MethodException) {
                                inboundRequest.sendSimpleResponse(
                                        Code.RESPONSE_BAD_REQUEST, x.getCause().getMessage() + " " + x.getCause());

                            } else if (x.getCause() instanceof Exception
                                    && !(x.getCause() instanceof RuntimeException)) {
                                inboundRequest.sendSimpleResponse(
                                        Code.RESPONSE_INTERNAL_SERVER_ERROR,
                                        x.getCause().toString());

                            } else if (x.getCause() instanceof Error) {
                                throw new ExecutionError((Error) x.getCause());

                            } else {
                                throw new UncheckedExecutionException(x.getCause());
                            }
                            return;
                        }

                        MutableMessage response = request.createResponse(Code.RESPONSE_CONTENT);

                        CborObject cborValue;

                        if (value instanceof FunctionalEndpoint) {
                            FunctionalEndpoint fe = (FunctionalEndpoint) value;
                            URI uri = mTechnology.getNativeUriForFunctionalEndpoint(fe);

                            if (fe.getParentFunctionalEndpoint() == mFe) {
                                response.setCode(Code.RESPONSE_CREATED);
                            }

                            if (uri != null && uri.getPath() != null) {
                                response.getOptionSet().setLocation(uri.getPath());
                                cborValue = CborTextString.create(uri.toASCIIString(), CborTag.URI);
                            } else {
                                inboundRequest.sendSimpleResponse(
                                        Code.RESPONSE_INTERNAL_SERVER_ERROR,
                                        "Unable to get path to created resource");
                                return;
                            }

                        } else {
                            try {
                                cborValue = CborObject.createFromJavaObject(value);

                            } catch (CborConversionException x) {
                                LOGGER.warning("CBOR encoding exception: " + x);
                                x.printStackTrace();
                                inboundRequest.sendSimpleResponse(
                                        Code.RESPONSE_INTERNAL_SERVER_ERROR, x.toString());
                                return;
                            }
                        }

                        int accept = -1;
                        if (optionSet.hasAccept()) {
                            //noinspection ConstantConditions
                            accept = optionSet.getAccept();
                        }

                        switch (accept) {
                            case ContentFormat.APPLICATION_CBOR:
                                response.getOptionSet()
                                        .setContentFormat(ContentFormat.APPLICATION_CBOR)
                                        .addEtag(Etag.createFromInteger(cborValue.hashCode()));
                                response.setPayload(cborValue.toCborByteArray());
                                break;
                            case -1:
                            case ContentFormat.TEXT_PLAIN_UTF8:
                                response.getOptionSet()
                                        .setContentFormat(ContentFormat.TEXT_PLAIN_UTF8)
                                        .addEtag(Etag.createFromInteger(cborValue.hashCode()));
                                response.setPayload(cborValue.toJsonString());
                                break;
                            case ContentFormat.APPLICATION_JSON:
                                response.getOptionSet()
                                        .setContentFormat(ContentFormat.APPLICATION_JSON)
                                        .addEtag(Etag.createFromInteger(cborValue.hashCode()));
                                response.setPayload(cborValue.toJsonString());
                                break;
                            default:
                                response.setCode(Code.RESPONSE_NOT_ACCEPTABLE);
                                break;
                        }

                        inboundRequest.sendResponse(response);
                    }
                },
                mTechnology.getExecutor());
    }

    @Override
    public void onChildRequest(InboundRequest inboundRequest, String childName) {
        Message msg = inboundRequest.getMessage();

        // In this method we are trying to intercept method queries before they
        // the request gets punted over to TraitChildrenResource.

        if (msg.getCode() != Code.METHOD_POST || !msg.getOptionSet().hasUriQuery()) {
            // Punt to default handler.
            super.onChildRequest(inboundRequest, childName);
            return;
        }

        final int originalOptionIndex = inboundRequest.getCurrentOptionIndex();
        Option nextOption = inboundRequest.nextOption();

        if (nextOption == null) {
            // Punt to default handler.
            super.onChildRequest(inboundRequest, childName);
            return;
        }

        if (nextOption.getNumber() == Option.URI_PATH) {
            if (nextOption.byteArrayLength() != 0) {
                // Restore the next option
                inboundRequest.setCurrentOptionIndex(originalOptionIndex);
                // Punt to default handler.
                super.onChildRequest(inboundRequest, childName);
                return;

            } else {
                nextOption = inboundRequest.nextOption();

                if (nextOption == null) {
                    // Restore the next option
                    inboundRequest.setCurrentOptionIndex(originalOptionIndex);

                    // Punt to default handler.
                    super.onChildRequest(inboundRequest, childName);
                    return;
                }
            }
        }

        List<String> queries = msg.getOptionSet().getUriQueries();

        if (nextOption.getNumber() != Option.URI_PATH && queries != null && !queries.isEmpty()) {
            String query = queries.get(0);

            // Hey! This is a method invocation! Let's handle this ourselves.
            MethodKey<Object> methodKey = new MethodKey<>(childName, query, Object.class);
            onInvokeMethod(inboundRequest, methodKey);

        } else {
            // Restore the next option
            inboundRequest.setCurrentOptionIndex(originalOptionIndex);

            // Punt to default handler.
            super.onChildRequest(inboundRequest, childName);
        }
    }

    @Override
    public void onChildRequestCheck(InboundRequest inboundRequest, String childName) {
        Message msg = inboundRequest.getMessage();

        if (msg.getCode() != Code.METHOD_POST || !msg.getOptionSet().hasUriQuery()) {
            super.onChildRequestCheck(inboundRequest, childName);
            return;
        }

        final int originalOptionIndex = inboundRequest.getCurrentOptionIndex();
        Option nextOption = inboundRequest.nextOption();

        if (nextOption == null) {
            super.onChildRequestCheck(inboundRequest, childName);
            return;
        }

        if (nextOption.getNumber() == Option.URI_PATH) {
            if (nextOption.byteArrayLength() != 0) {
                inboundRequest.setCurrentOptionIndex(originalOptionIndex);
                super.onChildRequestCheck(inboundRequest, childName);
                return;

            } else {
                nextOption = inboundRequest.nextOption();

                if (nextOption == null) {
                    inboundRequest.setCurrentOptionIndex(originalOptionIndex);
                    super.onChildRequestCheck(inboundRequest, childName);
                    return;
                }
            }
        }

        List<String> queries = msg.getOptionSet().getUriQueries();

        if (nextOption.getNumber() == Option.URI_PATH || queries == null || queries.isEmpty()) {
            inboundRequest.setCurrentOptionIndex(originalOptionIndex);
            super.onChildRequest(inboundRequest, childName);
        }
    }
}
