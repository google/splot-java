/*
 * Copyright (C) 2019 Google Inc.
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
package com.google.iot.m2m.base;

import com.google.common.util.concurrent.ListenableFuture;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * These test cases cover the default method implementations.
 */
public class TechnologyTest {

    volatile URI nativeUri;
    volatile boolean isHosted;

    FunctionalEndpoint fe = new FunctionalEndpoint() {
        @Override
        public boolean isLocal() {
            return false;
        }

        @Override
        public <T> ListenableFuture<?> setProperty(PropertyKey<T> key, @Nullable T value, Modifier... modifiers) {
            return null;
        }

        @Override
        public <T extends Number> ListenableFuture<?> incrementProperty(PropertyKey<T> key, T value, Modifier... modifiers) {
            return null;
        }

        @Override
        public <T> ListenableFuture<?> insertValueIntoProperty(PropertyKey<T[]> key, T value, Modifier... modifiers) {
            return null;
        }

        @Override
        public <T> ListenableFuture<?> removeValueFromProperty(PropertyKey<T[]> key, T value, Modifier... modifiers) {
            return null;
        }

        @Override
        public ListenableFuture<?> toggleProperty(PropertyKey<Boolean> key, Modifier... modifiers) {
            return null;
        }

        @Override
        public <T> ListenableFuture<T> fetchProperty(PropertyKey<T> key, Modifier... modifiers) {
            return null;
        }

        @Override
        public <T> @Nullable T getCachedProperty(PropertyKey<T> key) {
            return null;
        }

        @Override
        public ListenableFuture<Set<PropertyKey<?>>> fetchSupportedPropertyKeys() {
            return null;
        }

        @Override
        public ListenableFuture<Map<String, Object>> fetchSection(Section section, Modifier... mods) {
            return null;
        }

        @Override
        public Map<String, Object> copyCachedSection(Section section) {
            return null;
        }

        @Override
        public ListenableFuture<?> applyProperties(Map<String, Object> properties) {
            return null;
        }

        @Override
        public ListenableFuture<Boolean> delete() {
            return null;
        }

        @Override
        public <T> ListenableFuture<T> invokeMethod(MethodKey<T> methodKey, Map<String, Object> arguments) {
            return null;
        }

        @Override
        public ListenableFuture<Collection<FunctionalEndpoint>> fetchChildrenForTrait(String traitShortId) {
            return null;
        }

        @Override
        public @Nullable String getTraitForChild(FunctionalEndpoint child) {
            return null;
        }

        @Override
        public @Nullable String getIdForChild(FunctionalEndpoint child) {
            return null;
        }

        @Override
        public @Nullable FunctionalEndpoint getChild(String traitShortId, String childId) {
            return null;
        }

        @Override
        public @Nullable FunctionalEndpoint getParentFunctionalEndpoint() {
            return null;
        }

        @Override
        public <T> void registerPropertyListener(Executor executor, PropertyKey<T> key, PropertyListener<T> listener) {

        }

        @Override
        public <T> void unregisterPropertyListener(PropertyKey<T> key, PropertyListener<T> listener) {

        }

        @Override
        public void registerSectionListener(Executor executor, Section section, SectionListener listener) {

        }

        @Override
        public void unregisterSectionListener(SectionListener listener) {

        }

        @Override
        public void registerChildListener(Executor executor, ChildListener listener, String traitId) {

        }

        @Override
        public void unregisterChildListener(ChildListener listener, String traitId) {

        }

        @Override
        public void unregisterAllListeners() {

        }
    };

    Technology technology = new Technology() {
        @Override
        public void prepareToHost() throws IOException, TechnologyCannotHostException {

        }

        @Override
        public void host(FunctionalEndpoint fe) throws UnacceptableFunctionalEndpointException, TechnologyCannotHostException {

        }

        @Override
        public void unhost(FunctionalEndpoint fe) {

        }

        @Override
        public URI getNativeUriForProperty(FunctionalEndpoint fe, PropertyKey<?> propertyKey, Operation op, Modifier... modifiers) throws UnassociatedResourceException {
            return null;
        }

        @Override
        public URI getNativeUriForSection(FunctionalEndpoint fe, Section section, Modifier... modifiers) throws UnassociatedResourceException {
            return null;
        }

        @Override
        public DiscoveryBuilder createDiscoveryQueryBuilder() {
            return null;
        }

        @Override
        public FunctionalEndpoint getFunctionalEndpointForNativeUri(URI uri) throws UnknownResourceException {
            return null;
        }

        @Override
        public URI getNativeUriForFunctionalEndpoint(FunctionalEndpoint fe) throws UnassociatedResourceException {
            return nativeUri;
        }

        @Override
        public boolean isHosted(FunctionalEndpoint fe) {
            return isHosted;
        }
    };

    @Test
    public void getRelativeUriForFunctionalEndpoint() throws Exception {

        nativeUri = URI.create("coap://1.2.3.4/1/");
        isHosted = false;
        assertEquals(
                URI.create("/2/?blah#frag"),
                technology.getRelativeUriForFunctionalEndpoint(fe,
                        URI.create("coap://1.2.3.4/2/?blah#frag"))
        );

        assertEquals(
                URI.create("coap://4.5.6.7/2/?blah#frag"),
                technology.getRelativeUriForFunctionalEndpoint(fe,
                        URI.create("coap://4.5.6.7/2/?blah#frag"))
        );

        assertEquals(
                URI.create("uid://abcdefg/10/"),
                technology.getRelativeUriForFunctionalEndpoint(fe,
                        URI.create("uid://abcdefg/10/"))
        );

        assertThrows(UnassociatedResourceException.class,
                ()->technology.getRelativeUriForFunctionalEndpoint(fe,
                        URI.create("/2/?blah#frag")));

        assertEquals(
                URI.create("/"),
                technology.getRelativeUriForFunctionalEndpoint(fe,
                        URI.create("coap://1.2.3.4/"))
        );

        assertEquals(
                URI.create(""),
                technology.getRelativeUriForFunctionalEndpoint(fe,
                        URI.create("coap://1.2.3.4"))
        );

        assertEquals(
                URI.create("coap://4.5.6.7"),
                technology.getRelativeUriForFunctionalEndpoint(fe,
                        URI.create("coap://4.5.6.7"))
        );

        nativeUri = URI.create("/1/");
        isHosted = true;
        assertEquals(
                URI.create("coap://1.2.3.4/2/?blah#frag"),
                technology.getRelativeUriForFunctionalEndpoint(fe,
                        URI.create("coap://1.2.3.4/2/?blah#frag"))
        );

        assertEquals(
                URI.create("uid://abcdefg/10/"),
                technology.getRelativeUriForFunctionalEndpoint(fe,
                        URI.create("uid://abcdefg/10/"))
        );

        assertEquals(
                URI.create("/2/?blah#frag"),
                technology.getRelativeUriForFunctionalEndpoint(fe,
                        URI.create("/2/?blah#frag"))
        );
    }
}
