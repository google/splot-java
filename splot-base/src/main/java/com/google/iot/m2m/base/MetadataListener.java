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
package com.google.iot.m2m.base;

import java.util.Map;
import java.util.concurrent.Executor;

/**
 * Interface for handling changes to a {@link FunctionalEndpoint}'s metadata properties.
 *
 * @see FunctionalEndpoint#registerMetadataListener(Executor, MetadataListener)
 * @see FunctionalEndpoint#unregisterMetadataListener(MetadataListener)
 */
public interface MetadataListener {
    /**
     * Called whenever the "metadata" of a {@link FunctionalEndpoint} has changed.
     *
     * @param fe the functional endpoint that is reporting the change of metadata
     * @param metadata the current values of all of the properties in the metadata
     */
    void onMetadataChanged(FunctionalEndpoint fe, Map<String, Object> metadata);
}
