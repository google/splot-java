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
import com.google.iot.coap.*;
import com.google.iot.m2m.base.FunctionalEndpoint;
import com.google.iot.m2m.base.Section;
import com.google.iot.m2m.base.Splot;
import com.google.iot.m2m.trait.BaseTrait;
import java.net.URI;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.logging.Logger;

@SuppressWarnings("Convert2Lambda")
class HostedFunctionalEndpointAdapter extends Resource<InboundRequestHandler>
        implements LinkFormat.Provider {
    @SuppressWarnings("unused")
    private static final boolean DEBUG = false;

    @SuppressWarnings("unused")
    private static final Logger LOGGER =
            Logger.getLogger(HostedFunctionalEndpointAdapter.class.getCanonicalName());

    @SuppressWarnings("FieldCanBeLocal")
    private final SmcpTechnology mTechnology;

    private final FunctionalEndpoint mFe;
    private final Executor mExecutor;

    HostedFunctionalEndpointAdapter(SmcpTechnology technology, FunctionalEndpoint fe) {
        mFe = fe;
        mTechnology = technology;
        mExecutor = mTechnology.getExecutor();

        for (Section section : Section.values()) {
            addChild(section.id, new SectionResource(mFe, section, mExecutor));
        }

        addChild(Splot.SECTION_FUNC, new FuncResource(mTechnology, mFe));
    }

    FunctionalEndpoint getFunctionalEndpoint() {
        return mFe;
    }

    @Override
    public void onParentMethodDelete(InboundRequest inboundRequest, boolean trailingSlash) {
        if (checkForUnsupportedOptions(inboundRequest)) {
            return;
        }

        inboundRequest.responsePending();

        ListenableFuture<Boolean> future = mFe.delete();
        future.addListener(
                new Runnable() {
                    @Override
                    public void run() {
                        Boolean result;

                        try {
                            result = future.get();

                        } catch (InterruptedException e) {
                            inboundRequest.sendSimpleResponse(Code.RESPONSE_SERVICE_UNAVAILABLE);
                            return;

                        } catch (ExecutionException e) {
                            inboundRequest.sendSimpleResponse(
                                    Code.RESPONSE_INTERNAL_SERVER_ERROR, e.getMessage());
                            return;
                        }

                        if (Boolean.TRUE.equals(result)) {
                            inboundRequest.sendSimpleResponse(Code.RESPONSE_DELETED);
                        } else {
                            inboundRequest.sendSimpleResponse(Code.RESPONSE_METHOD_NOT_ALLOWED);
                        }
                    }
                },
                mExecutor);
    }

    @Override
    public void onBuildLinkFormat(LinkFormat.Builder builder) {
        // The root of a functional endpoint is required
        // to include an entry for itself as the first item in
        // the link format.
        onBuildLinkParams(builder.addLink(URI.create(".")));
        super.onBuildLinkFormat(builder);
    }

    @Override
    public void onBuildLinkParams(LinkFormat.LinkBuilder builder) {
        super.onBuildLinkParams(builder);

        Map<String, Object> metadata = mFe.copyCachedSection(Section.METADATA);

        String uid = BaseTrait.META_UID.getFromMapNoThrow(metadata);

        if (uid != null) {
            builder.put(LinkFormat.PARAM_ENDPOINT_NAME, uid);
        }

        builder.addInterfaceDescription(Smcp.IF_DESC_FE_FULL);

        if (mFe instanceof SmcpGroup && mTechnology.isAssociatedWith(mFe)) {
            builder.addInterfaceDescription(Smcp.IF_DESC_GROUP_FE);
        }

        String name = BaseTrait.META_NAME.getFromMapNoThrow(metadata);

        if (name != null) {
            builder.setTitle(name);

        } else {
            Map<String, String> productName =
                    BaseTrait.META_PRODUCT_NAME.getFromMapNoThrow(metadata);
            if (productName != null && !productName.isEmpty()) {
                builder.setTitle(productName.values().iterator().next());
            }
        }

        // If the performance of this method ever becomes an
        // issue, the following data could be easily cached.
        Set<String> traitSet = new LinkedHashSet<>();
        metadata.forEach(
                (k, v) -> {
                    String[] parts = k.split("/");
                    if (parts.length >= 2) {
                        traitSet.add(Smcp.RESOURCE_TYPE_PREFIX + parts[1]);
                    }
                });
        traitSet.forEach(builder::addResourceType);
    }
}
