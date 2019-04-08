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

import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.iot.cbor.CborConversionException;
import com.google.iot.cbor.CborObject;
import com.google.iot.coap.*;
import com.google.iot.m2m.base.*;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.checkerframework.checker.nullness.qual.Nullable;

import static com.google.iot.m2m.base.Splot.*;

class SmcpFunctionalEndpoint implements FunctionalEndpoint {
    private static final boolean DEBUG = false;
    private static final Logger LOGGER =
            Logger.getLogger(SmcpFunctionalEndpoint.class.getCanonicalName());

    private final Client mClient;
    final SmcpTechnology mTechnology;
    private final Map<SectionListener, Executor> mStateListenerMap = new ConcurrentHashMap<>();
    private final Map<SectionListener, Executor> mConfigListenerMap = new ConcurrentHashMap<>();
    private final Map<SectionListener, Executor> mMetadataListenerMap = new ConcurrentHashMap<>();
    private final Map<PropertyKey, Transaction> mPropertyObserverMap = new ConcurrentHashMap<>();
    private final Map<PropertyKey, Set<PropertyListenerEntry>> mPropertyListenerMap =
            new ConcurrentHashMap<>();
    private final Map<String, Set<ChildListenerEntry>> mChildListenerMap =
            new ConcurrentHashMap<>();
    private final Map<String, Transaction> mChildObserverMap = new ConcurrentHashMap<>();
    private volatile HashMap<String, Object> mStateCache = new HashMap<>();
    private Transaction mStateObserver = null;
    private volatile HashMap<String, Object> mConfigCache = new HashMap<>();
    private Transaction mConfigObserver = null;
    private volatile HashMap<String, Object> mMetadataCache = new HashMap<>();
    private Transaction mMetadataObserver = null;
    private SmcpFunctionalEndpoint mParent = null;

    private final HashMap<String, Set<FunctionalEndpoint>> mChildrenCache = new HashMap<>();

    SmcpFunctionalEndpoint(Client client, SmcpTechnology technology) {
        mClient = client;
        mTechnology = technology;
    }

    URI getUri() {
        return mClient.getUri();
    }

    @Override
    public boolean isLocal() {
        return false;
    }

    private Executor getExecutor() {
        return mTechnology.getExecutor();
    }

    /**
     * Performs a CoAP POST operation to the given path with the given value as the content. If
     * there is no content intended, then null may be passed as the value.
     *
     * @param path relative path for POST operation
     * @param value value to be encoded as CBOR content for POST operation. This object MUST either
     *     be null or be an object supported by {@link CborObject#createFromJavaObject(Object)}.
     * @return future used for canceling or determining completion
     */
    private ListenableFuture<Void> doPost(String path, @Nullable Object value) {
        final Transaction transaction;
        try {
            RequestBuilder requestBuilder =
                    mClient.newRequestBuilder().setCode(Code.METHOD_POST).changePath(path);

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

    @Override
    public <T> ListenableFuture<?> setProperty(PropertyKey<T> key, @Nullable T value, Modifier ... modifiers) {
        String path = key.getName();
        if (modifiers.length > 0) {
            path += "?" + Modifier.convertToQuery(modifiers);
        }
        return doPost(path, value);
    }

    @Override
    public <T extends Number> ListenableFuture<?> incrementProperty(PropertyKey<T> key, T value, Modifier ... modifiers) {
        String path = key.getName() + "?" + PROP_METHOD_INCREMENT;
        if (modifiers.length > 0) {
            path += "&" + Modifier.convertToQuery(modifiers);
        }
        return doPost(path, value);
    }

    @Override
    public <T> ListenableFuture<?> insertValueIntoProperty(PropertyKey<T[]> key, T value, Modifier ... modifiers) {
        String path = key.getName()+ "?" + PROP_METHOD_INSERT;
        if (modifiers.length > 0) {
            path += "&" + Modifier.convertToQuery(modifiers);
        }
        return doPost(path, value);
    }

    @Override
    public <T> ListenableFuture<?> removeValueFromProperty(PropertyKey<T[]> key, T value, Modifier ... modifiers) {
        String path = key.getName()+ "?" + PROP_METHOD_REMOVE;
        if (modifiers.length > 0) {
            path += "&" + Modifier.convertToQuery(modifiers);
        }
        return doPost(path, value);
    }

    @Override
    public ListenableFuture<?> toggleProperty(PropertyKey<Boolean> key, Modifier ... modifiers) {
        String path = key.getName() + "?" + PROP_METHOD_TOGGLE;
        if (modifiers.length > 0) {
            path += "&" + Modifier.convertToQuery(modifiers);
        }
        return doPost(path, null);
    }

    @Override
    public <T> ListenableFuture<T> fetchProperty(PropertyKey<T> key, Modifier ... modifiers) {
        String path = key.getName();
        if (modifiers.length > 0) {
            path += "?" + Modifier.convertToQuery(modifiers);
        }
        final Transaction transaction =
                mClient.newRequestBuilder()
                        .changePath(path)
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
                    T value;

                    try {
                        value = key.coerce(Utils.getObjectFromPayload(response));

                    } catch (BadRequestException e) {
                        throw new SmcpException("Invalid response: " + response, e.getCause());

                    } catch (UnsupportedContentFormatException e) {
                        throw new InvalidPropertyValueException(
                                "Unexpected Content-format in response: " + response, e);

                    } catch (InvalidValueException e) {
                        throw new InvalidPropertyValueException(
                                "Unexpected object type in response: " + response, e);
                    }

                    updateCachedPropertyValue(key, value);
                    set(value);

                } else {
                    String content = Code.toString(response.getCode());
                    if (response.isPayloadAscii()) {
                        content += ": " + response.getPayloadAsString();
                    }

                    if (response.getCode() == Code.RESPONSE_NOT_FOUND) {
                        throw new PropertyNotFoundException(content + " " + key);
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
    public ListenableFuture<Set<PropertyKey<?>>> fetchSupportedPropertyKeys() {
        /* This is tricky to implement for a few reasons. In the most straightforward
         * way to implement this we would just fetch each section and use the keys/types
         * from that information. However, we aren't guaranteed to get a non-null value
         * for a key (making it's type unknown), and we are also not guaranteed to get
         * the type right (List<String> vs String[]). We also may miss some keys because
         * some properties may not be passively readable (via fetching a section) under certain
         * circumstances. We could get around that later point by adding a query parameter
         * which would coax the properties to be present (and null), but there is no
         * getting around the container-vs-array type problem. It seems the best solution there is to just
         * use Object.class as the type, since most people are largely going to ignore
         * the type in these instances anyway---they will just compare known keys with types
         * to the returned set to see if it is in there. Types only affect equality when
         * there isn't an inheritance relationship, and that's not a problem with Object.
         */
        /* TODO: Not sure what to do here. */
        return Futures.immediateFailedFuture(new SmcpRuntimeException("Method not implemented"));
    }

    @Override
    public ListenableFuture<Map<String, Object>> fetchSection(Section section, Modifier... mods) {
        final Transaction transaction;
        String path = section.id + "/";
        if (mods.length > 0) {
            path += "?" + Modifier.convertToQuery(mods);
        }
        RequestBuilder requestBuilder =
                mClient.newRequestBuilder()
                        .changePath(path)
                        .setOmitUriHostPortOptions(true)
                        .addOptions(new OptionSet().setAccept(ContentFormat.APPLICATION_CBOR));

        transaction = requestBuilder.send();

        return new TransactionFuture<Map<String, Object>>(transaction) {
            @Override
            protected void onTransactionResponse(LocalEndpoint endpoint, Message response)
                    throws SmcpException {
                if (response.getCode() == Code.RESPONSE_CONTENT) {
                    final Map<String, Object> collapsed;

                    try {
                        collapsed =
                                Utils.collapseSectionToOneLevelMap(
                                        Utils.getMapFromPayload(response), section.id);

                    } catch (BadRequestException e) {
                        throw new SmcpException("Invalid response", e.getCause());

                    } catch (UnsupportedContentFormatException e) {
                        throw new SmcpException("Unexpected ContentType in response", e);
                    }

                    receivedUpdateForSection(section, collapsed);
                    set(collapsed);

                } else {
                    String content = Code.toString(response.getCode());
                    throw new SmcpException("Unexpected response: " + content);
                }
            }
        };
    }

    <T> void updateCachedPropertyValue(PropertyKey<T> key, @Nullable T value) {
        if (key.isSectionState()) {
            final HashMap<String, Object> map = mStateCache;
            // We synchronize on map here because mStateCache is not final.
            //noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized (map) {
                key.putInMap(map, value);
            }
        } else if (key.isSectionMetadata()) {
            final HashMap<String, Object> map = mMetadataCache;
            // We synchronize on map here because mMetadataCache is not final.
            //noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized (map) {
                key.putInMap(map, value);
            }
        } else if (key.isSectionConfig()) {
            final HashMap<String, Object> map = mConfigCache;
            // We synchronize on map here because mConfigCache is not final.
            //noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized (map) {
                key.putInMap(map, value);
            }
        }
    }

    @Override
    @Nullable
    public <T> T getCachedProperty(PropertyKey<T> key) {
        if (key.isSectionState()) {
            final HashMap<String, Object> map = mStateCache;
            // We synchronize on map here because mStateCache is not final.
            //noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized (map) {
                return key.coerceFromMapNoThrow(map);
            }
        } else if (key.isSectionMetadata()) {
            final HashMap<String, Object> map = mMetadataCache;
            // We synchronize on map here because mMetadataCache is not final.
            //noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized (map) {
                return key.coerceFromMapNoThrow(map);
            }
        } else if (key.isSectionConfig()) {
            final HashMap<String, Object> map = mConfigCache;
            // We synchronize on map here because mConfigCache is not final.
            //noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized (map) {
                return key.coerceFromMapNoThrow(map);
            }
        }
        return null;
    }

    @Override
    public Map<String, Object> copyCachedSection(Section section) {
        final HashMap<String, Object> map;

        switch (section) {
            case STATE:
                map = mStateCache;
                break;

            case CONFIG:
                map = mConfigCache;
                break;

            case METADATA:
                map = mMetadataCache;
                break;

            default:
                throw new AssertionError("Bad section " + section);
        }

        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (map) {
            // We synchronize here because there may be smaller
            // updates to the cache that don't replace the entire
            // map.
            return new HashMap<>(map);
        }
    }

    private String getSectionIdFromKeyString(String key) {
        if (key.startsWith(Splot.SECTION_STATE + "/")) {
            return Splot.SECTION_STATE;
        }
        if (key.startsWith(Splot.SECTION_METADATA + "/")) {
            return Splot.SECTION_METADATA;
        }
        if (key.startsWith(Splot.SECTION_CONFIG + "/")) {
            return Splot.SECTION_CONFIG;
        }
        if (key.startsWith(Splot.SECTION_FUNC + "/")) {
            return Splot.SECTION_FUNC;
        }
        return null;
    }

    @Override
    public ListenableFuture<?> applyProperties(Map<String, Object> properties) {
        /* All of these properties must be in the same section */
        if (properties.isEmpty()) {
            return Futures.immediateFuture(null);
        }

        String firstKey = properties.keySet().iterator().next();
        String sectionId = getSectionIdFromKeyString(firstKey);

        if (sectionId == null) {
            return Futures.immediateFailedFuture(
                    new SmcpException("Invalid key \"" + firstKey + "\""));
        }

        // Convert the properties to a hierarchical format
        try {
            Map<String, Map<String, Object>> sectionValue =
                    Utils.uncollapseSectionFromOneLevelMap(properties, sectionId);

            return doPost(sectionId + "/", sectionValue);
        } catch (SmcpException e) {
            return Futures.immediateFailedFuture(e);
        }
    }

    private <T> void changedPropertyKey(PropertyKey<T> key, @Nullable T value) {
        synchronized (mPropertyListenerMap) {
            if (mPropertyListenerMap.containsKey(key)) {
                for (PropertyListenerEntry entry : mPropertyListenerMap.get(key)) {
                    @SuppressWarnings("unchecked")
                    PropertyListener<T> listener = ((PropertyListener<T>) entry.mListener);
                    entry.mExecutor.execute(() -> listener.onPropertyChanged(this, key, value));
                }
            }
        }

        // Now refresh the value in the cache.
        final Map<String, Object> map;

        if (key.isSectionState()) {
            map = mStateCache;
        } else if (key.isSectionConfig()) {
            map = mConfigCache;
        } else if (key.isSectionMetadata()) {
            map = mMetadataCache;
        } else {
            map = null;
        }

        if (map != null) {
            getExecutor()
                    .execute(
                            () -> {
                                // The caches are volatile, but they only get replaced
                                // when the associated section observer is triggered.
                                // Thus, to avoid unnecessary object creation, we just
                                // update the map in place here. There is a possibility
                                // that we will be updating a map that has already been
                                // invalidated, but in that case it doesn't really matter.
                                synchronized (map) {
                                    key.putInMap(map, value);
                                }
                            });
        }
    }

    private <T> void receivedUpdateForPropertyKey(PropertyKey<T> key, Message response) {
        if (response.getCode() != Code.RESPONSE_CONTENT) {
            // This isn't the response we expected.
            LOGGER.warning("While observing " + key + ", got unexpected message code: " + response);
            return;
        }
        try {
            changedPropertyKey(key, key.coerce(Utils.getObjectFromPayload(response)));

        } catch (ResponseException | InvalidValueException e) {
            // This technically is a technology exception, but there is no one checking us.
            // This is a remote error, so we shouldn't throw a runtime exception. Logging
            // the issue seems to be the most reasonable thing. We may need to implement
            // some sort of asynchronous background technology error callback that we can
            // feed these errors into.
            LOGGER.log(
                    Level.WARNING,
                    "Failed to parse payload while observing " + key + " (" + response + ")",
                    e);
        }
    }

    private <T> void addPropertyObserver(PropertyKey<T> key) {
        mPropertyObserverMap.computeIfAbsent(
                key,
                k -> {
                    Transaction ret =
                            mClient.newRequestBuilder()
                                    .changePath(k.getName())
                                    .addOption(Option.OBSERVE)
                                    .setOmitUriHostPortOptions(true)
                                    .addOption(Option.ACCEPT, ContentFormat.APPLICATION_CBOR)
                                    .send();
                    ret.registerCallback(
                            new Transaction.Callback() {
                                @Override
                                public void onTransactionResponse(
                                        LocalEndpoint endpoint, Message response) {
                                    receivedUpdateForPropertyKey(key, response);
                                }
                            });
                    return ret;
                });
    }

    private <T> void removePropertyObserver(PropertyKey<T> key) {
        synchronized (mPropertyObserverMap) {
            Transaction transaction = mPropertyObserverMap.get(key);
            mPropertyObserverMap.remove(key);
            if (transaction != null) {
                transaction.cancel();
            }
        }
    }

    private void addChildObserver(String traitShortName) {
        mChildObserverMap.computeIfAbsent(
                traitShortName,
                k -> {
                    Transaction ret =
                            mClient.newRequestBuilder()
                                    .changePath(Splot.SECTION_FUNC + "/" + traitShortName + "/")
                                    .addOption(Option.OBSERVE)
                                    .setOmitUriHostPortOptions(true)
                                    .addOption(Option.ACCEPT, ContentFormat.APPLICATION_LINK_FORMAT)
                                    .send();
                    ret.registerCallback(
                            new Transaction.Callback() {
                                @Override
                                public void onTransactionResponse(
                                        LocalEndpoint endpoint, Message response) {
                                    receivedUpdateForTraitChildren(ret, traitShortName, response);
                                }
                            });
                    return ret;
                });
    }

    private void removeChildObserver(String traitShortName) {
        synchronized (mChildObserverMap) {
            Transaction transaction = mChildObserverMap.get(traitShortName);
            mChildObserverMap.remove(traitShortName);
            if (transaction != null) {
                transaction.cancel();
            }
        }
    }

    private void receivedUpdateForTraitChildren(
            Transaction transaction, String traitShortName, Message response) {
        if (response.getCode() != Code.RESPONSE_CONTENT) {
            // This isn't the response we expected.
            // TODO: How to handle this?
            LOGGER.warning(
                    "While observing f/"
                            + traitShortName
                            + "/, got unexpected message code: "
                            + response.toShortString());
            return;
        }

        if (DEBUG) LOGGER.info("Got " + traitShortName + " child update: " + response.toString());

        URI baseUri = mClient.getUri();
        Message request = transaction.getRequest();
        URI requestUri = request.getOptionSet().getUri();

        if (requestUri != null && requestUri.getPath() != null) {
            baseUri = baseUri.resolve(requestUri.getPath());
        }

        Set<FunctionalEndpoint> children;

        try {
            children = getChildrenFromResponse(baseUri, response);

        } catch (TechnologyException x) {
            LOGGER.log(Level.WARNING, "Checked exception in receivedUpdateForTraitChildren", x);
            return;
        }

        if (DEBUG) LOGGER.info("Children for " + traitShortName + ": " + children);

        handleTraitChildrenSet(traitShortName, children);
    }

    private void handleTraitChildrenSet(String trait, Set<FunctionalEndpoint> children) {
        final Set<FunctionalEndpoint> curr = Sets.newHashSet(children);
        Set<FunctionalEndpoint> prev;

        synchronized (mChildrenCache) {
            prev = mChildrenCache.get(trait);
            mChildrenCache.put(trait, curr);
        }

        Collection<ChildListenerEntry> childListenerEntrySet = null;

        synchronized (mChildListenerMap) {
            if (mChildListenerMap.containsKey(trait)) {
                childListenerEntrySet = new ArrayList<>(mChildListenerMap.get(trait));
            }
        }

        if (DEBUG) LOGGER.info("handleTraitChildrenSet: value = " + Sets.newHashSet(children));

        if (childListenerEntrySet != null && !childListenerEntrySet.isEmpty()) {
            if (prev == null) {
                prev = new HashSet<>();
            } else {
                prev = Sets.newHashSet(prev);
            }

            final Set<FunctionalEndpoint> toRemove = Sets.difference(prev, curr);
            final Set<FunctionalEndpoint> toAdd =
                    Sets.newHashSet(Sets.difference(curr, prev).iterator());

            if (DEBUG) {
                LOGGER.info("handleTraitChildrenSet: toRemove = " + toRemove);
                LOGGER.info("handleTraitChildrenSet: toAdd = " + toAdd);
            }

            if (toRemove.isEmpty() && toAdd.isEmpty()) {
                // No changes.
                return;
            }

            for (ChildListenerEntry entry : childListenerEntrySet) {
                entry.announceChanges(trait, toAdd, toRemove);
            }
        }
    }

    @Override
    public <T> void registerPropertyListener(
            Executor executor, PropertyKey<T> key, PropertyListener<T> listener) {
        final PropertyListenerEntry entry = new PropertyListenerEntry(executor, listener);
        synchronized (mPropertyListenerMap) {
            final Set<PropertyListenerEntry> keySet =
                    mPropertyListenerMap.computeIfAbsent(key, ignored -> new HashSet<>());
            if (keySet.isEmpty()) {
                addPropertyObserver(key);
            }
            keySet.add(entry);
        }
    }

    @Override
    public <T> void unregisterPropertyListener(PropertyKey<T> key, PropertyListener<T> listener) {
        synchronized (mPropertyListenerMap) {
            final Set<PropertyListenerEntry> keySet = mPropertyListenerMap.get(key);
            if (keySet != null) {
                keySet.remove(new PropertyListenerEntry(listener));

                if (keySet.isEmpty()) {
                    removePropertyObserver(key);
                }
            }
        }
    }

    @Override
    public void registerChildListener(
            Executor executor, ChildListener listener, String traitShortName) {
        final ChildListenerEntry entry =
                new ChildListenerEntry(
                        Objects.requireNonNull(executor), Objects.requireNonNull(listener));

        synchronized (mChildListenerMap) {
            final Set<ChildListenerEntry> keySet =
                    mChildListenerMap.computeIfAbsent(
                            Objects.requireNonNull(traitShortName), ignored -> new HashSet<>());

            if (keySet.isEmpty()) {
                addChildObserver(traitShortName);
            }

            keySet.add(entry);
        }

        // Pre-announce existing known children
        Set<FunctionalEndpoint> toAdd;
        synchronized (mChildrenCache) {
            toAdd = mChildrenCache.get(traitShortName);
            if (toAdd != null) {
                toAdd = new HashSet<>(toAdd);
            }
        }

        if (toAdd != null && !toAdd.isEmpty()) {
            entry.announceChanges(traitShortName, toAdd, null);
        }
    }

    @Override
    public void unregisterChildListener(ChildListener listener, String traitShortName) {
        synchronized (mChildListenerMap) {
            final Set<ChildListenerEntry> keySet = mChildListenerMap.get(traitShortName);
            if (keySet != null) {
                keySet.remove(new ChildListenerEntry(listener));

                if (keySet.isEmpty()) {
                    removeChildObserver(traitShortName);
                }
            }
        }
    }

    private void receivedUpdateForSection(Section section, Map<String, Object> collapsed) {
        switch (section) {
            case STATE:
                mStateCache = new HashMap<>(collapsed);

                mStateListenerMap.forEach(
                        (listener, exec) -> exec.execute(() -> listener.onSectionChanged(this, collapsed)));
                break;

            case CONFIG:
                mConfigCache = new HashMap<>(collapsed);

                mConfigListenerMap.forEach(
                        (listener, exec) -> exec.execute(() -> listener.onSectionChanged(this, collapsed)));
                break;

            case METADATA:
                mMetadataCache = new HashMap<>(collapsed);

                mMetadataListenerMap.forEach(
                        (listener, exec) ->
                                exec.execute(() -> listener.onSectionChanged(this, collapsed)));
                break;
        }
    }

    @Override
    public void registerSectionListener(Executor executor, Section section, SectionListener listener) {
        final Map<SectionListener, Executor> listenerMap;
        Transaction observer;

        switch (section) {
            case STATE:
                listenerMap = mStateListenerMap;
                observer = mStateObserver;
                break;

            case CONFIG:
                listenerMap = mConfigListenerMap;
                observer = mConfigObserver;
                break;

            case METADATA:
                listenerMap = mMetadataListenerMap;
                observer = mMetadataObserver;
                break;

            default:
                throw new AssertionError("Bad section " + section);
        }

        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (listenerMap) {
            listenerMap.put(listener, executor);

            if (listenerMap.size() == 1) {
                // This was the first state listener, so we need to
                // set up a new observer.

                if (observer != null) {
                    // Cancel any stale observer.
                    observer.cancel();
                    observer = null;
                }

                observer =
                        mClient.newRequestBuilder()
                                .changePath(section.id + "/")
                                .addOption(Option.OBSERVE)
                                .setOmitUriHostPortOptions(true)
                                .addOption(Option.ACCEPT, ContentFormat.APPLICATION_CBOR)
                                .send();

                observer.registerCallback(
                        new Transaction.Callback() {
                            @Override
                            public void onTransactionResponse(
                                    LocalEndpoint endpoint, Message response) {
                                if (response.getCode() != Code.RESPONSE_CONTENT) {
                                    LOGGER.warning("Unexpected message code: " + response);
                                    return;
                                }
                                try {
                                    final Map<String, Object> collapsed;
                                    collapsed =
                                            Utils.collapseSectionToOneLevelMap(
                                                    Utils.getMapFromPayload(response),
                                                    section.id);
                                    receivedUpdateForSection(section, collapsed);

                                } catch (ResponseException | SmcpException e) {
                                    LOGGER.warning(
                                            "Unable to parse message: "
                                                    + response
                                                    + ", threw exception "
                                                    + e);
                                }
                            }
                        });

                switch (section) {
                    case STATE:
                        mStateObserver = observer;
                        break;

                    case CONFIG:
                        mConfigObserver = observer;
                        break;

                    case METADATA:
                        mMetadataObserver = observer;
                        break;

                    default:
                        throw new AssertionError("Bad section " + section);
                }
            }
        }
    }

    @Override
    public void unregisterSectionListener(SectionListener listener) {
        synchronized (mStateListenerMap) {
            mStateListenerMap.remove(listener);

            if (mStateListenerMap.isEmpty() && mStateObserver != null) {
                mStateObserver.cancel();
                mStateObserver = null;
            }
        }
        synchronized (mConfigListenerMap) {
            mConfigListenerMap.remove(listener);

            if (mConfigListenerMap.isEmpty() && mConfigObserver != null) {
                mConfigObserver.cancel();
                mConfigObserver = null;
            }
        }
        synchronized (mMetadataListenerMap) {
            mMetadataListenerMap.remove(listener);

            if (mMetadataListenerMap.isEmpty() && mMetadataObserver != null) {
                mMetadataObserver.cancel();
                mMetadataObserver = null;
            }
        }
    }

    @Override
    public void unregisterAllListeners() {
        mMetadataListenerMap.clear();
        mConfigListenerMap.clear();
        mStateListenerMap.clear();
        mPropertyListenerMap.clear();
        mChildListenerMap.clear();

        mMetadataObserver.cancel();
        mMetadataObserver = null;
        mConfigObserver.cancel();
        mConfigObserver = null;
        mStateObserver.cancel();
        mStateObserver = null;

        synchronized (mPropertyObserverMap) {
            mPropertyObserverMap.forEach((k, trans) -> trans.cancel());
            mPropertyObserverMap.clear();
        }

        synchronized (mChildObserverMap) {
            mChildObserverMap.forEach((k, trans) -> trans.cancel());
            mChildObserverMap.clear();
        }
    }

    @Override
    public ListenableFuture<Boolean> delete() {
        final Transaction transaction =
                mClient.newRequestBuilder()
                        .setOmitUriHostPortOptions(true)
                        .setCode(Code.METHOD_DELETE)
                        .send();

        return new TransactionFuture<Boolean>(transaction) {
            @Override
            protected void onTransactionResponse(LocalEndpoint endpoint, Message response) {
                if (response.getCode() == Code.RESPONSE_DELETED) {
                    set(true);
                    unregisterAllListeners();
                } else {
                    String content = Code.toString(response.getCode());
                    if (response.isPayloadAscii()) {
                        content += ": " + response.getPayloadAsString();
                    }

                    LOGGER.warning(
                            "Unable to delete "
                                    + mClient.getUri()
                                    + ", encountered error: "
                                    + content);

                    set(false);
                }
            }
        };
    }

    @Override
    public <T> ListenableFuture<T> invokeMethod(
            MethodKey<T> methodKey, Map<String, Object> arguments) {

        final Transaction transaction;
        URI requestUri = mClient.getUri().resolve(methodKey.getName());

        try {
            CborObject payload = CborObject.createFromJavaObject(arguments);

            transaction =
                    mClient.newRequestBuilder()
                            .setCode(Code.METHOD_POST)
                            .changePath(methodKey.getName())
                            .addOption(Option.ACCEPT, ContentFormat.APPLICATION_CBOR)
                            .addOption(Option.CONTENT_FORMAT, ContentFormat.APPLICATION_CBOR)
                            .addOption(Option.ETAG, payload.hashCode())
                            .setOmitUriHostPortOptions(true)
                            .setPayload(payload.toCborByteArray())
                            .send();

        } catch (CborConversionException x) {
            return Futures.immediateFailedFuture(new SmcpException(x));
        }

        return new TransactionFuture<T>(transaction) {
            @Override
            protected void onTransactionResponse(LocalEndpoint endpoint, Message response)
                    throws MethodException, TechnologyException {

                if (response.getCode() == Code.RESPONSE_CREATED
                        && FunctionalEndpoint.class.isAssignableFrom(methodKey.getType())) {

                    FunctionalEndpoint childFe = null;

                    URI location = response.getOptionSet().getLocation();

                    if (location != null) {
                        location = requestUri.resolve(location);

                        childFe = mTechnology.getFunctionalEndpointForNativeUri(location);

                        if (childFe == null) {
                            String desc =
                                    "Unable to convert path <"
                                            + location
                                            + "> to a FunctionalEndpoint";
                            throw new SmcpRemoteException(desc + ": " + response);
                        }
                    }

                    // OK to pass null here.
                    set(methodKey.cast(childFe));

                } else if (response.getCode() == Code.RESPONSE_CONTENT) {
                    T value;

                    try {
                        value = methodKey.coerce(Utils.getObjectFromPayload(response));

                    } catch (BadRequestException e) {
                        throw new SmcpRemoteException(
                                "Invalid response: " + response, e.getCause());

                    } catch (UnsupportedContentFormatException e) {
                        throw new SmcpRemoteException(
                                "Unsupported Content-Format in response: " + response, e);

                    } catch (InvalidValueException e) {
                        throw new SmcpRemoteException(
                                "Unexpected Object-Type in response: " + response, e);
                    }

                    set(value);

                } else if (response.getCode() == Code.RESPONSE_CHANGED
                        || response.getCode() == Code.RESPONSE_DELETED
                        || response.getCode() == Code.RESPONSE_CREATED) {
                    super.onTransactionResponse(endpoint, response);

                } else {
                    String content = Code.toString(response.getCode());

                    if (response.isPayloadAscii()) {
                        content += ": " + response.getPayloadAsString();
                    }

                    if (response.getCode() == Code.RESPONSE_NOT_FOUND) {
                        throw new MethodNotFoundException(content);
                    } else if (response.getCode() == Code.RESPONSE_BAD_REQUEST) {
                        throw new InvalidMethodArgumentsException(content);
                    } else {
                        throw new SmcpRemoteException(content);
                    }
                }
            }
        };
    }

    private Set<FunctionalEndpoint> getChildrenFromResponse(URI baseUri, Message response)
            throws TechnologyException {
        if (response.getCode() != Code.RESPONSE_CONTENT) {
            throw new SmcpRemoteException(response.toString());
        }

        Integer contentFormat = response.getOptionSet().getContentFormat();

        if (contentFormat != null && ContentFormat.APPLICATION_LINK_FORMAT != contentFormat) {
            throw new SmcpRemoteException("Bad content format, " + response.toString());
        }

        StringReader reader = new StringReader(response.getPayloadAsString());
        Set<FunctionalEndpoint> ret = new HashSet<>();
        Map<URI, Map<String, String>> results;

        try {
            results = LinkFormat.parseLinkFormat(reader, null);
        } catch (IOException | LinkFormatParseException x) {
            throw new SmcpRemoteException(x);
        }

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
                SmcpFunctionalEndpoint smcpFe = (SmcpFunctionalEndpoint) fe;

                if (smcpFe.mParent == null) {
                    smcpFe.mParent = SmcpFunctionalEndpoint.this;
                }
            }
            ret.add(fe);
        }

        return ret;
    }

    @Override
    public ListenableFuture<Collection<FunctionalEndpoint>> fetchChildrenForTrait(
            String traitShortId) {
        final Transaction transaction =
                mClient.newRequestBuilder()
                        .setCode(Code.METHOD_GET)
                        .changePath(Splot.SECTION_FUNC + "/" + traitShortId + "/")
                        .addOption(Option.ACCEPT, ContentFormat.APPLICATION_LINK_FORMAT)
                        .setOmitUriHostPortOptions(true)
                        .send();

        return new TransactionFuture<Collection<FunctionalEndpoint>>(transaction) {
            @Override
            protected void onTransactionResponse(LocalEndpoint endpoint, Message response)
                    throws TechnologyException {

                URI baseUri = mClient.getUri();
                Message request = transaction.getRequest();
                URI requestUri = request.getOptionSet().getUri();

                if (requestUri != null && requestUri.getPath() != null) {
                    baseUri = baseUri.resolve(requestUri.getPath());
                }

                Set<FunctionalEndpoint> children = getChildrenFromResponse(baseUri, response);
                set(children);
                handleTraitChildrenSet(traitShortId, children);
            }
        };
    }

    @Nullable
    private String[] getChildPathComponents(FunctionalEndpoint child) {
        if (child == this || !(child instanceof SmcpFunctionalEndpoint)) {
            return null;
        }

        SmcpFunctionalEndpoint smcpChild = (SmcpFunctionalEndpoint) child;

        if (smcpChild.mTechnology != mTechnology) {
            return null;
        }

        String childUriString = smcpChild.mClient.getUri().toASCIIString();
        String parentUriString = mClient.getUri().toASCIIString();

        if (!childUriString.startsWith(parentUriString)) {
            return null;
        }

        String relativePath = childUriString.substring(parentUriString.length());

        if (relativePath.length() < 3) {
            return null;
        }

        if (relativePath.charAt(0) == '/') {
            relativePath = relativePath.substring(1);
        }

        String[] components = relativePath.split("/");

        if (components.length < 3
                || !"f".equals(components[0])
                || components[1].isEmpty()
                || components[2].isEmpty()) {
            return null;
        }

        return components;
    }

    @Nullable
    @Override
    public String getTraitForChild(FunctionalEndpoint child) {
        String[] components = getChildPathComponents(child);

        if (components == null) {
            return null;
        }

        return components[1];
    }

    @Nullable
    @Override
    public String getIdForChild(FunctionalEndpoint child) {
        String[] components = getChildPathComponents(child);

        if (components == null) {
            return null;
        }

        return components[2];
    }

    @Nullable
    @Override
    public FunctionalEndpoint getChild(String traitShortId, String childId) {
        String childPath = Splot.SECTION_FUNC + "/" + traitShortId + "/" + childId + "/";

        FunctionalEndpoint ret =
                null;
        try {
            ret = mTechnology.getFunctionalEndpointForNativeUri(mClient.getUri().resolve(childPath));
        } catch (UnknownResourceException e) {
            // Should not happen.
            throw new SmcpRuntimeException(e);
        }

        if (ret instanceof SmcpFunctionalEndpoint) {
            ((SmcpFunctionalEndpoint) ret).mParent = this;
        }
        return ret;
    }

    @Nullable
    @Override
    public FunctionalEndpoint getParentFunctionalEndpoint() {
        return mParent;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" + mClient.getUri() + ">";
    }

    private class PropertyListenerEntry {
        final Executor mExecutor;
        final PropertyListener<?> mListener;

        <T> PropertyListenerEntry(PropertyListener<T> listener) {
            mExecutor =
                    (ignored) -> {
                        throw new AssertionError();
                    };
            mListener = Objects.requireNonNull(listener);
        }

        <T> PropertyListenerEntry(Executor executor, PropertyListener<T> listener) {
            mExecutor = Objects.requireNonNull(executor);
            mListener = Objects.requireNonNull(listener);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }

            if (!(obj instanceof PropertyListenerEntry)) {
                return false;
            }

            PropertyListenerEntry rhs = (PropertyListenerEntry) obj;

            return mListener.equals(rhs.mListener);
        }

        @Override
        public int hashCode() {
            return mListener.hashCode();
        }
    }

    private class ChildListenerEntry {
        final Executor mExecutor;
        final ChildListener mListener;

        ChildListenerEntry(ChildListener listener) {
            mExecutor =
                    (ignored) -> {
                        throw new AssertionError();
                    };
            mListener = Objects.requireNonNull(listener);
        }

        ChildListenerEntry(Executor executor, ChildListener listener) {
            mExecutor = Objects.requireNonNull(executor);
            mListener = Objects.requireNonNull(listener);
        }

        @Override
        public boolean equals(Object o) {
            if (o == null) {
                return false;
            }
            if (!(o instanceof ChildListenerEntry)) {
                return false;
            }
            return mListener.equals(((ChildListenerEntry) o).mListener);
        }

        @Override
        public int hashCode() {
            return mListener.hashCode();
        }

        void announceChanges(
                String trait,
                @Nullable Collection<FunctionalEndpoint> toAdd,
                @Nullable Collection<FunctionalEndpoint> toRemove) {
            mExecutor.execute(
                    () -> {
                        if (toAdd != null) {
                            for (FunctionalEndpoint child : toAdd) {
                                mListener.onChildAdded(SmcpFunctionalEndpoint.this, trait, child);
                            }
                        }
                        if (toRemove != null) {
                            for (FunctionalEndpoint child : toRemove) {
                                mListener.onChildRemoved(SmcpFunctionalEndpoint.this, trait, child);
                            }
                        }
                    });
        }
    }
}
