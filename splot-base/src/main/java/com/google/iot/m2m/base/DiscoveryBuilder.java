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

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.CheckReturnValue;
import java.util.concurrent.TimeUnit;

/**
 * Builder class for constructing a new {@link DiscoveryQuery} object.
 *
 * @see Technology#createDiscoveryQueryBuilder()
 */
@CanIgnoreReturnValue
@SuppressWarnings("unused")
public abstract class DiscoveryBuilder {
    /**
     * Determines if things that are hosted by this {@link Technology} should be
     * included in the results. Default value is <code>false</code>. If the technology does not
     * support hosting, then this method does nothing.
     *
     * @param includeHosted true if hosted thing should be included in discovery
     *     results, false otherwise.
     * @return a convenience reference to this discovery builder object.
     */
    public abstract DiscoveryBuilder includeHosted(boolean includeHosted);

    /**
     * Specifies that the discovery results should only include groups. Default is to return all
     * discovered things, including groups. If the technology does not support groups,
     * this method will cause the resulting {@link DiscoveryQuery} to return nothing.
     *
     * @return a convenience reference to this discovery builder object.
     */
    public abstract DiscoveryBuilder mustBeGroup();

    /**
     * Specifies that the discovery results should only include groups. Default is to return all
     * discovered things, including groups. If the technology does not support groups,
     * then this method does nothing.
     *
     * @return a convenience reference to this discovery builder object.
     */
    public abstract DiscoveryBuilder mustNotBeGroup();

    /**
     * Determines the maximum amount of time that should be spent discovering things.
     * The default value is technology-specific, but generally not longer than 10 seconds.
     *
     * @param timeout the duration of the discovery operation, measured in <code>units</code>.
     * @param units the units of <code>timeout</code>.
     * @return a convenience reference to this discovery builder object.
     */
    public abstract DiscoveryBuilder setTimeout(long timeout, TimeUnit units);

    /**
     * Indicates that this discovery operation is to be limited to returning things
     * which have the specified traits, as identified by the trait's short name. This method may be
     * called multiple times to require multiple traits to be present in the discovered functional
     * endpoints.
     *
     * @param traitShortName the trait short name to match on discovered things.
     * @return a convenience reference to this discovery builder object.
     */
    public abstract DiscoveryBuilder mustHaveTrait(String traitShortName);

    /**
     * Indicates that this discovery operation is to be limited to returning things
     * which have the specified UID.
     *
     * <p>If this criteria is specified, there is generally only a single {@link Thing}
     * (or no things) returned. However, more may be returned if there is a UID
     * conflict.
     *
     * @param uid the uid string to match for discovered things.
     * @return a convenience reference to this discovery builder object.
     * @throws IllegalStateException if this method has been previously called.
     */
    public abstract DiscoveryBuilder mustHaveUid(String uid);

    /**
     * Determines the maximum number of things which can be discovered before the
     * query automatically terminates. A value of zero (<code>0</code>) indicates that there is no
     * explicitly defined maximum. The default value is zero.
     *
     * @param count The maximum number of things to discover, or zero if there is no
     *     maximum.
     * @return a convenience reference to this discovery builder object.
     */
    public abstract DiscoveryBuilder setMaxResults(int count);

    /**
     * Constructs and executes a discovery query based on the specified criteria.
     *
     * @return the resulting {@link DiscoveryQuery} object.
     */
    @CheckReturnValue
    public abstract DiscoveryQuery buildAndRun();
}
