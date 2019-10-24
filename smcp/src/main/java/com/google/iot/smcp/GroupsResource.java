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

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

class GroupsResource extends Resource<HostedThingAdapter> {
    private static final boolean DEBUG = false;
    private static final Logger LOGGER = Logger.getLogger(GroupsResource.class.getCanonicalName());
    private final SmcpTechnology mTechnology;

    GroupsResource(SmcpTechnology technology) {
        mTechnology = technology;
    }

    @Override
    public void onChildMethodDelete(
            InboundRequest inboundRequest, HostedThingAdapter child) {
        Thing fe = child.getThing();

        if (!(fe instanceof Group)) {
            super.onChildMethodDelete(inboundRequest, child);
            return;
        }

        // Remember, the group held by the adapter is the local group,
        // not the SmcpGroup. We have to look up that group using the
        // groupId.
        Group group = (Group) fe;
        SmcpGroup smcpGroup = mTechnology.findGroupWithId(group.getGroupId());

        if (smcpGroup == null) {
            super.onChildMethodDelete(inboundRequest, child);
            return;
        }

        mTechnology.unhost(smcpGroup);

        inboundRequest.success();
    }

    @Override
    public void onChildMethodDeleteCheck(
            InboundRequest inboundRequest, HostedThingAdapter child) {
        // Do nothing.
    }

    @Override
    public void onParentMethodPost(InboundRequest inboundRequest, boolean trailingSlash) {
        Message request = inboundRequest.getMessage();
        if (DEBUG) LOGGER.info("onParentMethodPost " + request);

        List<String> query = request.getOptionSet().getUriQueries();
        Map<String, String> queryMap = request.getOptionSet().getUriQueriesAsMap();

        Map<String, Object> content = null;

        try {
            if (request.hasPayload()) {
                content = Utils.getMapFromPayload(request);
            }
        } catch (ResponseException x) {
            inboundRequest.sendSimpleResponse(x.getCode());
            return;
        }

        if ((query == null) || query.isEmpty() || query.get(0).contains("=")) {
            inboundRequest.sendSimpleResponse(Code.RESPONSE_BAD_REQUEST, "No action specified");

        } else if ("add".equals(query.get(0))) {

            if (content != null) {
                if (content.containsKey(Splot.SECTION_STATE)) {
                    inboundRequest.sendSimpleResponse(
                            Code.RESPONSE_BAD_REQUEST,
                            "illegal section \"" + Splot.SECTION_STATE + "\"");
                    return;
                }

                try {
                    content = Utils.collapseToOneLevelMap(content);
                } catch (SmcpException e) {
                    inboundRequest.sendSimpleResponse(Code.RESPONSE_BAD_REQUEST, e.getMessage());
                }
            }

            // Add a group
            String groupId = queryMap.get(LinkFormat.PARAM_ENDPOINT_NAME);
            Group group;
            if (groupId != null) {
                group = mTechnology.findOrCreateGroupWithId(groupId);
            } else {
                group = mTechnology.internalCreateNewGroup();
            }

            try {
                mTechnology.host(group);
                inboundRequest.sendCreated("/g/" + group.getGroupId() + "/");

                if (content != null) {
                    group.applyProperties(content);
                }

            } catch (UnacceptableThingException e) {
                inboundRequest.sendSimpleResponse(
                        Code.RESPONSE_INTERNAL_SERVER_ERROR, e.getMessage());
                e.printStackTrace();
            }

        } else {
            inboundRequest.sendSimpleResponse(
                    Code.RESPONSE_BAD_REQUEST, "Unknown query action \"" + query.get(0) + "\"");
        }
    }

    @Override
    public void onParentMethodPostCheck(InboundRequest inboundRequest, boolean trailingSlash) {
        // Do nothing
    }

    @Override
    public void onBuildLinkParams(LinkFormat.LinkBuilder builder) {
        super.onBuildLinkParams(builder);
        builder.addInterfaceDescription(Smcp.IF_DESC_GROUPS);
    }
}
