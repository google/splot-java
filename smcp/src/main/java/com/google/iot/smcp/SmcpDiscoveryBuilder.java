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
import com.google.iot.m2m.base.DiscoveryBuilder;
import com.google.iot.m2m.base.DiscoveryQuery;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * SMCP-specific subclass of {@link DiscoveryBuilder} which adds some CoAP-specific methods.
 *
 * <p>Instances of this class are created via {@link SmcpTechnology#createDiscoveryQueryBuilder()}.
 *
 * @see SmcpTechnology
 */
public class SmcpDiscoveryBuilder extends DiscoveryBuilder {

    @SuppressWarnings("unused")
    private static final boolean DEBUG = false;

    @SuppressWarnings("unused")
    private static final Logger LOGGER =
            Logger.getLogger(SmcpDiscoveryBuilder.class.getCanonicalName());

    private final SmcpTechnology mTechnology;
    private long mTimeoutInMs = 3000;
    private final Set<String> mRequiredTraits = new HashSet<>();
    private String mRequiredUid = null;
    private LocalEndpoint mLocalEndpoint;
    private int mMaxResults = -1;
    private URI mBaseUri = null;

    @SuppressWarnings({"CanBeFinal", "FieldCanBeLocal"})
    private boolean mIncludeHosted = false;

    @SuppressWarnings({"CanBeFinal", "FieldCanBeLocal"})
    private boolean mIncludeGroups = true;

    @SuppressWarnings({"CanBeFinal", "FieldCanBeLocal"})
    private boolean mIncludeNormal = true;

    SmcpDiscoveryBuilder(SmcpTechnology technology) {
        mTechnology = technology;
        mLocalEndpoint = technology.mDefaultLocalEndpoint;
    }

    public SmcpDiscoveryBuilder setLocalEndpoint(LocalEndpoint localEndpoint) {
        mLocalEndpoint = localEndpoint;
        return this;
    }

    public SmcpDiscoveryBuilder setBaseUri(URI baseUri) {
        mBaseUri = baseUri;
        return this;
    }

    @Override
    public SmcpDiscoveryBuilder includeHosted(boolean includeHosted) {
        mIncludeHosted = includeHosted;
        return this;
    }

    @Override
    public DiscoveryBuilder mustBeGroup() {
        mIncludeGroups = true;
        mIncludeNormal = false;
        return this;
    }

    @Override
    public DiscoveryBuilder mustNotBeGroup() {
        mIncludeGroups = false;
        mIncludeNormal = true;
        return this;
    }

    @Override
    public SmcpDiscoveryBuilder setTimeout(long timeout, TimeUnit units) {
        if (timeout < 0) {
            throw new IllegalArgumentException("Timeout cannot be negative");
        }
        mTimeoutInMs = units.toMillis(timeout);
        return this;
    }

    @Override
    public SmcpDiscoveryBuilder mustHaveTrait(String traitShortName) {
        mRequiredTraits.add(Smcp.RESOURCE_TYPE_PREFIX + traitShortName);
        return this;
    }

    @Override
    public SmcpDiscoveryBuilder mustHaveUid(String uid) {
        if (mRequiredUid != null) {
            throw new IllegalStateException("Already set mustHaveUid");
        }
        mRequiredUid = uid;
        return this;
    }

    @Override
    public SmcpDiscoveryBuilder setMaxResults(int count) {
        if (count <= 0) {
            mMaxResults = 0;
        } else {
            mMaxResults = count;
        }

        return this;
    }

    private DiscoveryQuery buildAndRunInternal(Client client) {
        RequestBuilder requestBuilder = client.newRequestBuilder();

        if (!mRequiredTraits.isEmpty()) {
            requestBuilder.addOption(Option.URI_QUERY, "rt=" + String.join(" ", mRequiredTraits));
        }

        if (mRequiredUid != null) {
            requestBuilder.addOption(Option.URI_QUERY, "ep=" + mRequiredUid);
        }

        requestBuilder.addOption(Option.ACCEPT, ContentFormat.APPLICATION_LINK_FORMAT);
        requestBuilder.setConfirmable(false);
        requestBuilder.setOmitUriHostPortOptions(true);

        SmcpDiscoveryQuery ret =
                new SmcpDiscoveryQuery(
                        mTechnology, requestBuilder.prepare(), mTimeoutInMs, mMaxResults);
        ret.restart();
        return ret;
    }

    @Override
    public DiscoveryQuery buildAndRun() {
        Client client;

        URI baseUri = mBaseUri;

        if (baseUri == null) {
            String hostname = Coap.ALL_NODES_MCAST_HOSTNAME;

            if (hostname.contains(":")) {
                hostname = "[" + hostname + "]";
            }

            if (mLocalEndpoint != null) {
                baseUri = URI.create(mLocalEndpoint.getScheme() + "://" + hostname + "/");
            } else {
                baseUri = URI.create("coap://" + hostname + "/");
            }
        }

        client = new Client(mTechnology.getLocalEndpointManager(), baseUri, mLocalEndpoint, null);

        client.changeUri(URI.create(Smcp.DISCOVERY_QUERY_URI));

        return buildAndRunInternal(client);
    }
}
