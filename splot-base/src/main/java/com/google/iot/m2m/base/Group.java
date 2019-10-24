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

import com.google.common.util.concurrent.ListenableFuture;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.Set;

/**
 * Interface for managing a group. A group is a collection of {@link Thing
 * Things} which can be controlled as a single Thing.
 *
 * <p>The state section of a group thing is special in that any operation performed on
 * properties in that section will be applied to those properties on all of the group's members.
 *
 * <p>By comparison, the configuration and metadata sections belong to the group object itself.
 * Thus, it is not possible to change a configuration or metadata property across the members of a
 * group in a single operation like you can with state properties.
 *
 * <p>The behavior of reading state properties on a group is currently undefined by the splot object
 * model and thus the behavior is technology-specific. This may change at some point.
 *
 * <p>All groups must implement at least the following traits:
 *
 * <ul>
 *   <li>{@link com.google.iot.m2m.trait.BaseTrait}, for name, uid, etc.
 *   <li>{@link com.google.iot.m2m.trait.GroupTrait}, to provide an in-band interface for
 *       adding/removing members.
 * </ul>
 *
 * <p>Groups are referenced by their {@link com.google.iot.m2m.trait.BaseTrait#META_UID UID}, which
 * for groups is called a GroupId (or GID). Unlike UIDs on other {@link Thing
 * Things}, the GroupID is immutable and thus cannot be changed after group creation.
 * Use the {@link com.google.iot.m2m.trait.BaseTrait#META_NAME} property to associate a
 * human-readable name with a group.
 *
 * @see Technology
 * @see Thing
 */
@SuppressWarnings("unused")
public interface Group extends Thing {
    /**
     * The group identifier string. This is usually a randomly generated 10-12 character string.
     * This is defined to be identical to {@link com.google.iot.m2m.trait.BaseTrait#META_UID}.
     */
    String getGroupId();

    /** The technology that is hosting this group. */
    Technology getTechnology();

    /**
     * Indicates this device has local things which are participating as members of
     * this group. In other words, it indicates if this device is in the group.
     *
     * @return true if this device has local funcitonal endpoints that are members of this group,
     *     false otherwise.
     */
    boolean hasLocalMembers();

    /**
     * Indicates if this device can add/remove members to/from this group.
     *
     * @return true if this device can add/remove members to/from this group, false otherwise.
     */
    boolean canAdministerGroup();

    /**
     * Indicates if this object is allowed to change or query the state of the group.
     *
     * @return true if this device can change or query the state of the group, false otherwise.
     */
    boolean canUseGroup();

    /**
     * Indicates if this group can reliably apply changes to all members. Non-reliable groups make a
     * best-effort to ensure that all members have received any changes, but dropped packets can
     * lead to some devices not receiving the changes.
     *
     * <p>When a group is reliable, it may multicast the change initially but MUST soon follow up
     * with each member individually to ensure that the change was received.
     *
     * @return true if this group can be considered reliable, false otherwise.
     */
    boolean isReliable();

    /**
     * Fetches the member things of the group. This is not always possible, in those
     * cases (assuming there was no underlying exception at play) the returned future may return
     * 'null' from {@link ListenableFuture#get()}.
     *
     * <p>If {@link #isReliable()} and either {@link #canUseGroup()} or {@link
     * #canAdministerGroup()} return {@code true}, then this method is guaranteed to successfully
     * return all of the members of the group.
     *
     * <p>The returned future will throw {@link GroupNotAvailableException} if this group has been
     * successfully deleted (for example, by a call to {@link #delete()}). if this group has been
     * deleted or is otherwise no longer available.
     */
    ListenableFuture<Set<Thing>> fetchMembers();

    /**
     * Adds a member to the Group.
     *
     * <p>The added member must be associated (either hosted or native) with this group's
     * technology, otherwise the returned future will fail with {@link
     * UnacceptableThingException}. Some technologies cannot add arbitrary
     * Things to a group. In those cases, this operation will also fail with {@link
     * UnacceptableThingException}.
     *
     * <p>The returned future will throw {@link GroupNotAvailableException} if this group has been
     * deleted or is otherwise no longer available.
     *
     * <p>Adding groups to other groups that are native to the same {@link Technology} instance is
     * prohibited. Doing so will immediately throw {@link IllegalArgumentException}. Adding groups
     * to groups hosted on different technologies is not prohibited per-se but may be restricted by
     * a particular technology.
     *
     * @param fe the thing to add to this Group
     * @return a {@link ListenableFuture} used to cancel, track completion, or retrieve an exception
     *     thrown while processing. The exact status of the membership addition will be undefined if
     *     canceled before complete.
     * @throws IllegalArgumentException if {@code fe} is a Group that is native to this Group's
     *     Technology
     * @see #removeMember(Thing)
     * @see #fetchMembers()
     */
    @CanIgnoreReturnValue
    ListenableFuture<Void> addMember(Thing fe);

    /**
     * Removes a member of the Group. In the case where the given thing is not a
     * member fo the group, this method does nothing.
     *
     * <p>The returned future will throw {@link GroupNotAvailableException} if this Group has been
     * deleted or is otherwise no longer available.
     *
     * @param fe the thing to remove from the group.
     * @return a {@link ListenableFuture} used to cancel, track completion, or retrieve an exception
     *     thrown while processing. The exact status of the membership removal will be undefined if
     *     canceled before complete.
     * @see #addMember(Thing)
     */
    @CanIgnoreReturnValue
    ListenableFuture<Void> removeMember(Thing fe);
}
