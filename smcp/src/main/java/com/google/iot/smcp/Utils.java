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

import com.google.iot.cbor.*;
import com.google.iot.coap.BadRequestException;
import com.google.iot.coap.ContentFormat;
import com.google.iot.coap.Message;
import com.google.iot.coap.UnsupportedContentFormatException;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

class Utils {

    static Object getObjectFromPayload(Message msg)
            throws UnsupportedContentFormatException, BadRequestException {
        Object content = null;
        Integer contentFormat = msg.getOptionSet().getContentFormat();

        if (contentFormat == null) {
            contentFormat = ContentFormat.TEXT_PLAIN_UTF8;
        }

        if (msg.hasPayload()) {
            try {
                switch (contentFormat) {
                    case ContentFormat.APPLICATION_CBOR:
                        CborObject cborPayload =
                                CborObject.createFromCborByteArray(msg.getPayload());
                        content = cborPayload.toJavaObject();
                        break;

                    case ContentFormat.APPLICATION_JSON:
                        JSONObject jsonPayload = new JSONObject(msg.getPayloadAsString());
                        // JSONObject.toMap() is unavailable on Android, so we use
                        // CBOR instead to create the map:
                        content = CborMap.createFromJSONObject(jsonPayload).toNormalMap();
                        break;

                    case ContentFormat.TEXT_PLAIN_UTF8:
                        content = new JSONTokener(msg.getPayloadAsString()).nextValue();

                        if (content instanceof JSONArray) {
                            JSONArray jsonArray = (JSONArray) content;
                            content = CborArray.createFromJSONArray(jsonArray).toJavaObject();

                        } else if (content instanceof JSONObject) {
                            JSONObject jsonObject = (JSONObject) content;
                            content = CborMap.createFromJSONObject(jsonObject).toNormalMap();

                        } else if (JSONObject.NULL.equals(content)) {
                            content = null;
                        }
                        break;

                    default:
                        throw new UnsupportedContentFormatException();
                }
            } catch (CborConversionException | CborParseException | JSONException x) {
                throw new BadRequestException(x);
            }
        }
        return content;
    }

    static Map<String, Object> getMapFromPayload(Message message)
            throws UnsupportedContentFormatException, BadRequestException {
        Map<String, Object> content;
        Integer contentFormat = message.getOptionSet().getContentFormat();

        if (contentFormat == null) {
            contentFormat = ContentFormat.APPLICATION_CBOR;
        }

        try {
            switch (contentFormat) {
                case ContentFormat.APPLICATION_CBOR:
                    CborMap cborPayload = CborMap.createFromCborByteArray(message.getPayload());
                    content = cborPayload.toNormalMap();
                    break;
                case ContentFormat.APPLICATION_JSON:
                case ContentFormat.TEXT_PLAIN_UTF8:
                    JSONObject jsonPayload = new JSONObject(message.getPayloadAsString());
                    content = jsonPayload.toMap();
                    break;
                default:
                    throw new UnsupportedContentFormatException();
            }

        } catch (CborConversionException | CborParseException | JSONException x) {
            throw new BadRequestException(x);
        }

        return content;
    }

    static Map<String, Object> collapseToOneLevelMap(Map<String, ?> payload) throws SmcpException {
        final HashMap<String, Object> converted = new HashMap<>();

        for (Map.Entry<String, ?> entry : payload.entrySet()) {
            if (!(entry.getValue() instanceof Map)) {
                throw new SmcpException("Unexpected type for section " + entry.getKey());
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> sectionMap = (Map<String, Object>) entry.getValue();
            converted.putAll(collapseSectionToOneLevelMap(sectionMap, entry.getKey()));
        }

        return converted;
    }

    static Map<String, Object> collapseSectionToOneLevelMap(Map<String, ?> payload, String sectionId)
            throws SmcpException {
        final HashMap<String, Object> converted = new HashMap<>();

        for (Map.Entry<String, ?> entry : payload.entrySet()) {
            String k = entry.getKey();
            Object v = entry.getValue();
            if (!(v instanceof Map)) {
                throw new SmcpException("Unexpected type for trait " + k);
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> traitMap = (Map<String, Object>) v;
            traitMap.forEach((k2, v2) -> converted.put(sectionId + "/" + k + "/" + k2, v2));
        }

        return converted;
    }

    static Map<String, Map<String, Object>> uncollapseSectionFromOneLevelMap(
            Map<String, Object> properties, String sectionId) throws SmcpException {
        Map<String, Map<String, Object>> ret = new HashMap<>();

        for (Map.Entry<String, ?> entry : properties.entrySet()) {
            String k = entry.getKey();
            Object v = entry.getValue();
            String[] components = k.split("/");

            // Make sure the key is well-formed.
            if (components.length != 3) {
                throw new SmcpException("Key \"" + k + "\" is not properly formatted");
            }

            // Make sure the key is in the same section.
            if (!sectionId.equals(components[0])) {
                throw new SmcpException("Key \"" + k + "\" is in the wrong section");
            }

            Map<String, Object> traitMap =
                    ret.computeIfAbsent(components[1], k1 -> new HashMap<>());
            traitMap.put(components[2], v);
        }

        return ret;
    }
}
