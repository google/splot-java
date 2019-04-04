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

import com.google.iot.coap.ContentFormat;
import com.google.iot.coap.LinkFormat;
import com.google.iot.coap.Resource;
import com.google.iot.m2m.base.ChildListener;
import com.google.iot.m2m.base.FunctionalEndpoint;
import java.net.URI;
import java.util.*;
import java.util.logging.Logger;

class TraitChildrenResource extends Resource<HostedFunctionalEndpointAdapter>
        implements ChildListener {
    private static final boolean DEBUG = false;
    private static final Logger LOGGER =
            Logger.getLogger(TraitChildrenResource.class.getCanonicalName());

    private final SmcpTechnology mTechnology;

    @SuppressWarnings("FieldCanBeLocal")
    private final FunctionalEndpoint mFe;

    @SuppressWarnings("FieldCanBeLocal")
    private final String mTrait;

    private final Map<FunctionalEndpoint, String> mChildIdLookup = new HashMap<>();

    private final Set<FunctionalEndpoint> mIndependentChildren = new HashSet<>();

    TraitChildrenResource(SmcpTechnology technology, FunctionalEndpoint fe, String trait) {
        mTechnology = technology;
        mFe = fe;
        mTrait = trait;
        mFe.registerChildListener(technology.getExecutor(), this, mTrait);
    }

    @SuppressWarnings("EmptyMethod")
    @Override
    public void onBuildLinkFormat(LinkFormat.Builder builder) {
        super.onBuildLinkFormat(builder);

        for (FunctionalEndpoint childFe : mIndependentChildren) {
            final URI uri = mTechnology.getNativeUriForFunctionalEndpoint(childFe);

            if (uri == null) {
                continue;
            }

            builder.addLink(uri)
                    .addInterfaceDescription("tag:google.com,2018:m2m#r1")
                    .addContentFormat(ContentFormat.APPLICATION_LINK_FORMAT);
        }
    }

    @Override
    public void onChildAdded(
            FunctionalEndpoint parent, String traitShortName, FunctionalEndpoint child) {
        String childId = parent.getIdForChild(child);

        if (DEBUG)
            LOGGER.info("Child " + child + " added to trait " + traitShortName + " on " + parent);

        if (childId != null) {
            mChildIdLookup.put(child, childId);
            addChild(childId, new HostedFunctionalEndpointAdapter(mTechnology, child));
        } else {
            LOGGER.warning("onChildAdded was called, but the FE wasn't a child: " + child);
            if (mIndependentChildren.add(child)) {
                getObservable().trigger();
            }
        }
    }

    @Override
    public void onChildRemoved(
            FunctionalEndpoint parent, String traitShortName, FunctionalEndpoint child) {
        String childId = mChildIdLookup.get(child);

        if (DEBUG)
            LOGGER.info(
                    "Child " + child + " removed from trait " + traitShortName + " on " + parent);

        if (childId != null) {
            child.unregisterAllListeners();
            removeChild(childId);
        }

        mChildIdLookup.remove(child);
        if (mIndependentChildren.remove(child)) {
            getObservable().trigger();
        }
    }
}
