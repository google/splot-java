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

import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.Executor;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Interface for a <i>Functional Endpoint.</i>
 *
 * <p>A Functional Endpoint (FE) can be thought of as an object that represents a set of
 * functionally related properties and methods. A physical device hosts one or more FEs.
 *
 * <p>Examples of the relationship between physical devices and FEs:
 *
 * <ul>
 *   <li>A smart light bulb might host a single FE that can be used to control the state of the
 *       light bulb.
 *   <li>A smart power strip might host one FE for every power outlet on the strip.
 *   <li>A proprietary wireless sensor gateway might host one (or more) FEs for every wireless
 *       sensor.
 * </ul>
 *
 * <p>All class methods that act on individual properties are type-safe because the value type is
 * baked into the {@link PropertyKey}: using the wrong class will cause a compiler error.
 *
 * <h2>Implementation Guidance</h2>
 *
 * <p>If you need to implement a functional endpoint, you have two options:
 *
 * <ul>
 *   <li><b>Subclass {@link com.google.iot.m2m.local.LocalFunctionalEndpoint} (of one of {@link
 *       com.google.iot.m2m.local.LocalSceneFunctionalEndpoint its} {@link
 *       com.google.iot.m2m.local.LocalTransitioningFunctionalEndpoint subclasses}).</b> This is
 *       useful if you need to control something local. It is straightforward to use and can give
 *       you a lot of additional functionality (like scenes and transitions) for free. However, it
 *       is (usually) not appropriate to use these abstract classes to implement non-local
 *       functionality, such as for a wireless sensor — for that consider the next option.
 *   <li><b>Implement the {@link FunctionalEndpoint} interface directly.</b> This might be
 *       reasonable if your functional endpoint is very simple, or if you want to expose
 *       functionality provided off device such as a wireless sensor or a Zigbee light. This option
 *       gives you the most implementation flexibility but is significantly more work to implement
 *       and test than the previous option: so if you are implementing a functional endpoint which
 *       controls local behaviors, consider the previous option instead.
 * </ul>
 *
 * <p>Once an instance of a functional endpoint is available, you can then host that functional
 * endpoint using a {@link Technology}, allowing other devices to use it.
 *
 * <h2>Example Usage</h2>
 *
 * <h3>Fetching a property value</h3>
 *
 * <p>To fetch the value of a property on a FunctionalEndpoint instance, you use the {@link
 * #fetchProperty(PropertyKey)} method. The most simple case would look like this:
 *
 * <pre>{@code
 * Boolean onOffValue = fe.fetchProperty(OnOffTrait.STAT_VALUE).get();
 * }</pre>
 *
 * <p>In this case we are immediately calling {@link ListenableFuture#get()} in order to make the
 * call synchronous and immediately return a value. However, we can make this call asynchronous by
 * simply adding a listener to the returned future:
 *
 * <pre><code>
 * Executor executor = ...;
 * ListenableFuture&lt;Boolean&gt; future = fe.fetchProperty(OnOffTrait.STAT_VALUE);
 * future.addListener(() -> {
 *     Boolean onOffValue = future.get();
 *
 *     /* do stuff with onOffValue here &#x2A;/
 * }, executor);
 * </code></pre>
 *
 * <h3>Setting a property value</h3>
 *
 * <p>Setting (and incrementing, toggling, etc.) a property value follows the same pattern as
 * fetching a property value: the method returns a {@link ListenableFuture}. However, in these cases
 * the ListenableFuture doesn't wrap around a value: its purpose is simple to provide a way to
 * synchronize on the completion of the operation or to cancel the operation. For example, to
 * synchronously turn {@code fe} on we could do the following:
 *
 * <pre>{@code
 * fe.setProperty(OnOffTrait.STAT_VALUE, true).get();
 * }</pre>
 *
 * <p>If we don't care about synchronizing to the value being committed, we could even omit the call
 * to {@link ListenableFuture#get()}:
 *
 * <pre>{@code
 * fe.setProperty(OnOffTrait.STAT_VALUE, true);
 * }</pre>
 *
 * <p>However, in this case we would have no way of knowing if the operation was successful or not.
 * This is why if you want to change a value asynchronously you would normally add a listener to the
 * future to handle any exceptions:
 *
 * <pre><code>
 * Executor executor = ...;
 *
 * ListenableFuture&lt;?&gt; future = fe.setProperty(OnOffTrait.STAT_VALUE, true);
 *
 * future.addListener(() -> {
 *      try {
 *          future.get();
 *
 *      } catch ({@link InterruptedException} x) {
 *          /* See https://goo.gl/ZNZVem &#x2A;/
 *          Thread.currentThread().interrupt();
 *          return;
 *
 *      } catch ({@link java.util.concurrent.CancellationException} x) {
 *          /* Handle cancellation here &#x2A;/
 *          return;
 *
 *      } catch ({@link java.util.concurrent.ExecutionException} x) {
 *          /* Handle the exception here &#x2A;/
 *          return;
 *      }
 *
 *      /* Success, do stuff here &#x2A;/
 * }, executor);
 * </code></pre>
 *
 * <h3>Getting notifications about individual property value changes</h3>
 *
 * <p>You an use {@link #registerPropertyListener(Executor, PropertyKey, PropertyListener)} to
 * register a {@link PropertyListener} instance to be invoked whenever a property changes value.
 *
 * <pre><code>
 * Executor executor = ...;
 *
 * PropertyListener&lt;Boolean&gt; listener = (fe, key, value) -> {
 *      System.out.println(key + " on " + fe + " just changed to " + value);
 * };
 *
 * fe.registerPropertyListener(executor, OnOffTrait.STAT_VALUE, listener);
 *
 * /* do stuff here &#x2A;/
 *
 * fe.unregisterPropertyListener(OnOffTrait.STAT_VALUE, listener);
 * </code></pre>
 *
 * @see Technology
 * @see com.google.iot.m2m.local.LocalFunctionalEndpoint
 * @see com.google.iot.m2m.local.LocalSceneFunctionalEndpoint
 * @see com.google.iot.m2m.local.LocalTransitioningFunctionalEndpoint
 * @see com.google.iot.m2m.trait.BaseTrait
 */
@SuppressWarnings("unused")
public interface FunctionalEndpoint {

    /**
     * Indicates if this functional endpoint is local or not.
     *
     * @return true if this FunctionalEndpoint is hosted locally, false otherwise.
     */
    boolean isLocal();

    /**
     * Asynchronously changes a specific property to the given value, using given modifiers.
     *
     * <p>The caller can optionally block until completion by calling the {@link
     * ListenableFuture#get()} method, or can be notified of completion by adding a listener via
     * {@link ListenableFuture#addListener(Runnable, Executor)}.
     *
     * <p>The returned future may throw one of the following checked exceptions (as the cause to a
     * {@link java.util.concurrent.ExecutionException}) when completed:
     *
     * <ul>
     *   <li>{@link PropertyException}, or one of its subclasses:
     *       <ul>
     *         <li>{@link PropertyReadOnlyException} if the property is read-only on this fe
     *         <li>{@link PropertyNotFoundException} if the property isn't present on this fe
     *         <li>{@link InvalidPropertyValueException} if the property value was rejected
     *       </ul>
     *   <li>{@link TechnologyException} if there was a technology-specific problem
     * </ul>
     *
     * @param key the key identifying the property and its type
     * @param value the new value to set the property to
     * @param modifiers The modifiers to apply to this operation, like {@link Modifier#duration(double)}.
     * @return a future that can be cancelled or monitored asynchronously for completion
     */
    @CanIgnoreReturnValue
    <T> ListenableFuture<?> setProperty(PropertyKey<T> key, @Nullable T value, Modifier ... modifiers);

    /**
     * Asynchronously increments a specific property by the given amount.
     *
     * <p>The caller can optionally block until completion by calling the {@link
     * ListenableFuture#get()} method, or can be notified of completion by adding a listener via
     * {@link ListenableFuture#addListener(Runnable, Executor)}.
     *
     * <p>The returned future may throw one of the following checked exceptions (as the cause to a
     * {@link java.util.concurrent.ExecutionException}) when completed:
     *
     * <ul>
     *   <li>{@link PropertyException}, or one of its subclasses:
     *       <ul>
     *         <li>{@link PropertyReadOnlyException} if the property is read-only (increment
     *             requires read/write)
     *         <li>{@link PropertyWriteOnlyException} if the property is write-only (increment
     *             requires read/write)
     *         <li>{@link PropertyNotFoundException} if the property isn't present on this fe
     *         <li>{@link InvalidPropertyValueException} if the increment value was rejected
     *         <li>{@link PropertyOperationUnsupportedException} if increment is not allowed on this
     *             property
     *       </ul>
     *   <li>{@link TechnologyException} if there was a technology-specific problem
     * </ul>
     *
     * @param key the key identifying the property and its type
     * @param value the amount to increment the value of the property by
     * @param modifiers The modifiers to apply to this operation, like {@link Modifier#duration(double)}.
     * @return a future that can be cancelled or monitored asynchronously for completion
     */
    @CanIgnoreReturnValue
    <T extends Number> ListenableFuture<?> incrementProperty(PropertyKey<T> key, T value, Modifier ... modifiers);

    /**
     * Asynchronously adds the given value to a list/set property
     *
     * <p>The caller can optionally block until completion by calling the {@link
     * ListenableFuture#get()} method, or can be notified of completion by adding a listener via
     * {@link ListenableFuture#addListener(Runnable, Executor)}.
     *
     * <p>Note that the order of the resulting value is specified by the property itself (if at
     * all).
     *
     * <p>The returned future may throw one of the following checked exceptions (as the cause to a
     * {@link java.util.concurrent.ExecutionException}) when completed:
     *
     * <ul>
     *   <li>{@link PropertyException}, or one of its subclasses:
     *       <ul>
     *         <li>{@link PropertyReadOnlyException} if the property is read-only (add value
     *             requires read/write)
     *         <li>{@link PropertyWriteOnlyException} if the property is write-only (add value
     *             requires read/write)
     *         <li>{@link PropertyNotFoundException} if the property isn't present on this fe
     *         <li>{@link PropertyOperationUnsupportedException} if add value is not allowed on this
     *             property
     *         <li>{@link InvalidPropertyValueException} if the value being added was rejected
     *       </ul>
     *   <li>{@link TechnologyException} if there was a technology-specific problem
     * </ul>
     *
     * @param key the key identifying the property and its type
     * @param value the value to be added to the list or set
     * @param modifiers The modifiers to apply to this operation, like {@link Modifier#duration(double)}.
     * @return a future that can be cancelled or monitored asynchronously for completion
     */
    @CanIgnoreReturnValue
    <T> ListenableFuture<?> addValueToProperty(PropertyKey<T[]> key, T value, Modifier ... modifiers);

    /**
     * Asynchronously removes the given value from a list/set property. If the value is not in the
     * property value then no action is taken.
     *
     * <p>The caller can optionally block until completion by calling the {@link
     * ListenableFuture#get()} method, or can be notified of completion by adding a listener via
     * {@link ListenableFuture#addListener(Runnable, Executor)}.
     *
     * <p>Note that the order of the resulting value is specified by the property itself (if at
     * all).
     *
     * <p>The returned future may throw one of the following checked exceptions (as the cause to a
     * {@link java.util.concurrent.ExecutionException}) when completed:
     *
     * <ul>
     *   <li>{@link PropertyException}, or one of its subclasses:
     *       <ul>
     *         <li>{@link PropertyReadOnlyException} if the property is read-only (remove value
     *             requires read/write)
     *         <li>{@link PropertyWriteOnlyException} if the property is write-only (remove value
     *             requires read/write)
     *         <li>{@link PropertyNotFoundException} if the property isn't present on this fe
     *         <li>{@link PropertyOperationUnsupportedException} if remove value is not allowed on
     *             this property
     *         <li>{@link InvalidPropertyValueException} if the value being removed was rejected
     *       </ul>
     *   <li>{@link TechnologyException} if there was a technology-specific problem
     * </ul>
     *
     * @param key the key identifying the property and its type
     * @param value the value to be added to the list or set
     * @param modifiers The modifiers to apply to this operation, like {@link Modifier#duration(double)}.
     * @return a future that can be cancelled or monitored asynchronously for completion
     */
    @CanIgnoreReturnValue
    <T> ListenableFuture<?> removeValueFromProperty(PropertyKey<T[]> key, T value, Modifier ... modifiers);

    /**
     * Asynchronously toggle a specific property. This only works on boolean properties.
     *
     * <p>The caller can optionally block until completion by calling the {@link
     * ListenableFuture#get()} method, or can be notified of completion by adding a listener via
     * {@link ListenableFuture#addListener(Runnable, Executor)}.
     *
     * <p>The returned future may throw one of the following checked exceptions (as the cause to a
     * {@link java.util.concurrent.ExecutionException}) when completed:
     *
     * <ul>
     *   <li>{@link PropertyException}, or one of its subclasses:
     *       <ul>
     *         <li>{@link PropertyReadOnlyException} if the property is read-only (toggle requires
     *             read/write)
     *         <li>{@link PropertyWriteOnlyException} if the property is write-only (toggle requires
     *             read/write)
     *         <li>{@link PropertyNotFoundException} if the property isn't present on this fe
     *         <li>{@link PropertyOperationUnsupportedException} if toggle is not allowed on this
     *             property
     *       </ul>
     *   <li>{@link TechnologyException} if there was a technology-specific problem
     * </ul>
     *
     * @param key the key identifying the property to toggle
     * @param modifiers The modifiers to apply to this operation, like {@link Modifier#duration(double)}.
     * @return a future that can be used to block execution until the action is complete, cancel the
     *     action, or monitor the action asynchronously for completion
     */
    @CanIgnoreReturnValue
    ListenableFuture<?> toggleProperty(PropertyKey<Boolean> key, Modifier ... modifiers);

    /**
     * Asynchronously fetch the value of a specific property.
     *
     * <p>The fetched value of the property is obtained by calling {@link ListenableFuture#get()} on
     * the returned {@link ListenableFuture}, which will block execution until the value is obtained
     * or an exception is thrown. The caller can be notified when the value has been successfully
     * obtained (and, thus, the call to {@link ListenableFuture#get()} is then guaranteed not to
     * block) by adding a listener via {@link ListenableFuture#addListener(Runnable, Executor)}.
     *
     * <p>The returned future may throw one of the following checked exceptions (as the cause to a
     * {@link java.util.concurrent.ExecutionException}) when completed:
     *
     * <ul>
     *   <li>{@link PropertyException}, or one of its subclasses:
     *       <ul>
     *         <li>{@link PropertyWriteOnlyException} if the property is write-only
     *         <li>{@link PropertyNotFoundException} if the property isn't present on this fe
     *       </ul>
     *   <li>{@link TechnologyException} if there was a technology-specific problem
     * </ul>
     *
     * <p>
     *
     * @param key the key for the property to fetch
     * @param modifiers The modifiers to apply to this operation, like
     *                  {@link Modifier#transitionTarget()}.
     * @return a future to access the fetched value (Can also be cancelled or monitored
     *     asynchronously for completion)
     * @see #getCachedProperty(PropertyKey)
     */
    @CanIgnoreReturnValue
    <T> ListenableFuture<T> fetchProperty(PropertyKey<T> key, Modifier ... modifiers);

    /**
     * Asynchronously fetch the set property keys that are supported by this functional endpoint.
     * Note that this method will only work reliably if {@link #isLocal()} returns {@code true}.
     * Otherwise, it makes a best-effort but may not list all supported properties.
     *
     * <p>The returned future may throw one of the following checked exceptions (as the cause to a
     * {@link java.util.concurrent.ExecutionException}) when completed:
     *
     * <ul>
     *   <li>{@link TechnologyException} if there was a technology-specific problem
     * </ul>
     *
     * @return a future to retrieve fetched set of property keys (Can also be cancelled or monitored
     *     asynchronously for completion)
     */
    @CanIgnoreReturnValue
    ListenableFuture<Set<PropertyKey<?>>> fetchSupportedPropertyKeys();

    /**
     * Asynchronously fetch a Map containing the values of all of the "state" properties on this
     * functional endpoint.
     *
     * <p>The key for the returned {@link Map} is the name of the property as returned by {@link
     * PropertyKey#getName()}. To fetch individual properties from the map in a type-safe way, use
     * {@link PropertyKey#coerceFromMap(Map)}.
     *
     * <p>The returned future may throw one of the following checked exceptions (as the cause to a
     * {@link java.util.concurrent.ExecutionException}) when completed:
     *
     * <ul>
     *   <li>{@link TechnologyException} if there was a technology-specific problem
     * </ul>
     *
     * @param modifiers The modifiers to apply to this operation, like
     *                  {@link Modifier#transitionTarget()}.
     * @return a future to retrieve a map containing all "state" property values (Can also be
     *     cancelled or monitored asynchronously for completion)
     */
    @CanIgnoreReturnValue
    ListenableFuture<Map<String, Object>> fetchState(Modifier ... modifiers);

    /**
     * Asynchronously fetch a Map containing the values of all of the "config" properties on this
     * functional endpoint.
     *
     * <p>The key for the returned {@link Map} is the name of the property as returned by {@link
     * PropertyKey#getName()}. To fetch individual properties from the map in a type-safe way, use
     * {@link PropertyKey#coerceFromMap(Map)}.
     *
     * <p>The returned future may throw one of the following checked exceptions (as the cause to a
     * {@link java.util.concurrent.ExecutionException}) when completed:
     *
     * <ul>
     *   <li>{@link TechnologyException} if there was a technology-specific problem
     * </ul>
     *
     * @return a future to retrieve a map containing all "config" property values (Can also be
     *     cancelled or monitored asynchronously for completion)
     */
    @CanIgnoreReturnValue
    ListenableFuture<Map<String, Object>> fetchConfig();

    /**
     * Asynchronously fetch a Map containing the values of all of the "metadata" properties on this
     * functional endpoint.
     *
     * <p>The key for the returned {@link Map} is the name of the property as returned by {@link
     * PropertyKey#getName()}. To fetch individual properties from the map in a type-safe way, use
     * {@link PropertyKey#coerceFromMap(Map)}.
     *
     * <p>The returned future may throw one of the following checked exceptions (as the cause to a
     * {@link java.util.concurrent.ExecutionException}) when completed:
     *
     * <ul>
     *   <li>{@link TechnologyException} if there was a technology-specific problem
     * </ul>
     *
     * @return a future to retrieve a map containing all "metadata" property values (Can also be
     *     cancelled or monitored asynchronously for completion)
     */
    @CanIgnoreReturnValue
    ListenableFuture<Map<String, Object>> fetchMetadata();

    /**
     * Synchronously return the most recent cached value of a specific property.
     *
     * <p>Note that if this is a "local" Functional Endpoint ({@link #isLocal()} returns <code>true
     * </code>), then the returned values are the actual values of the properties instead of cached
     * values.
     *
     * <p>Note that this method offers no way to differentiate the following cases where {@code
     * null} is returned:
     *
     * <ul>
     *   <li>The property hasn't been cached yet.
     *   <li>The property isn't supported by this functional endpoint.
     *   <li>The value of the property is actually {@code null}.
     * </ul>
     *
     * If such ambiguity is unacceptable, you will need to either use the {@link
     * #fetchProperty} method or inspect the maps returned from {@link
     * #copyCachedState()}, {@link #copyCachedConfig()}, and/or {@link #copyCachedMetadata()}.
     *
     * @param key the key for the property to fetch
     * @param <T> the type of the property (inferred from the key)
     * @return the cached value of the property, or null if the property is not cached.
     */
    @Nullable
    <T> T getCachedProperty(PropertyKey<T> key);

    /**
     * Synchronously return a {@link Map} containing the cached values of all of the "state"
     * properties on this functional endpoint.
     *
     * <p>Note that if this is a "local" Functional Endpoint ({@link #isLocal()} returns <code>true
     * </code>), then the returned values are the actual values of the properties instead of cached
     * values.
     *
     * <p>The key for the returned {@link Map} is the name of the property as returned by {@link
     * PropertyKey#getName()}. To fetch individual properties from the map in a type-safe way, use
     * {@link PropertyKey#coerceFromMap(Map)}.
     *
     * @return a {@link Map} containing the cached values of all of the "state" properties
     */
    Map<String, Object> copyCachedState();

    /**
     * Synchronously return a {@link Map} containing the cached values of all of the "config"
     * properties on this functional endpoint.
     *
     * <p>Note that if this is a "local" Functional Endpoint ({@link #isLocal()} returns <code>true
     * </code>), then the returned values are the actual values of the properties instead of cached
     * values.
     *
     * <p>The key for the returned {@link Map} is the name of the property as returned by {@link
     * PropertyKey#getName()}. To fetch individual properties from the map in a type-safe way, use
     * {@link PropertyKey#coerceFromMap(Map)}.
     *
     * @return a {@link Map} containing the cached values of all of the "config" properties
     */
    Map<String, Object> copyCachedConfig();

    /**
     * Synchronously return a {@link Map} containing the cached values of all of the "metadata"
     * properties on this functional endpoint.
     *
     * <p>Note that if this is a "local" Functional Endpoint ({@link #isLocal()} returns <code>true
     * </code>), then the returned values are the actual values of the properties instead of cached
     * values.
     *
     * <p>The key for the returned {@link Map} is the name of the property as returned by {@link
     * PropertyKey#getName()}. To fetch individual properties from the map in a type-safe way, use
     * {@link PropertyKey#coerceFromMap(Map)}.
     *
     * @return a {@link Map} containing the cached values of all of the "metadata" properties
     */
    Map<String, Object> copyCachedMetadata();

    /**
     * Asynchronously change the value of multiple properties at once.
     *
     * <p>To add properties to a {@link Map} in a type-safe way, use the {@link
     * PropertyKey#putInMap} method.
     *
     * <p>The caller can optionally block until completion by calling the {@link
     * ListenableFuture#get()} method, or can be notified of completion by adding a listener via
     * {@link ListenableFuture#addListener(Runnable, Executor)}.
     *
     * <p>The applied properties must all be from the same section. The behavior of passing
     * properties from different sections is undefined.
     *
     * <p>The returned future may throw one of the following checked exceptions (as the cause to a
     * {@link java.util.concurrent.ExecutionException}) when completed:
     *
     * <ul>
     *   <li>{@link PropertyException}, or one of its subclasses:
     *       <ul>
     *         <li>{@link PropertyWriteOnlyException}
     *         <li>{@link PropertyNotFoundException}
     *         <li>{@link PropertyOperationUnsupportedException}
     *         <li>{@link InvalidPropertyValueException}
     *       </ul>
     *   <li>{@link TechnologyException} if there was a technology-specific problem
     * </ul>
     *
     * However, it this functional endpoint is a group, then this method's future will generally not
     * throw {@link PropertyException} for state section properties.
     *
     * <p>
     *
     * @param properties a map keyed by the property name containing the new values for those
     *     properties.
     * @return a future that can be used to block execution until the action is complete, cancel the
     *     action, or monitor the action asynchronously for completion
     */
    @CanIgnoreReturnValue
    ListenableFuture<?> applyProperties(Map<String, Object> properties);

    /**
     * Deletes this Functional Endpoint, if possible. Note that not all functional endpoints can be
     * deleted. If the delete operation cannot be performed, the future will indicate an exception.
     * If this operation completes successfully, this {@link FunctionalEndpoint} interface becomes
     * invalid and should be discarded.
     *
     * <p>The returned future may throw one of the following checked exceptions (as the cause to a
     * {@link java.util.concurrent.ExecutionException}) when completed:
     *
     * <ul>
     *   <li>{@link TechnologyException} if there was a technology-specific problem
     * </ul>
     *
     * @return a future object returning a boolean. If the boolean is true, the object was
     *     successfully deleted. If the boolean is false, the object is not deletable.
     */
    @CanIgnoreReturnValue
    ListenableFuture<Boolean> delete();

    /**
     * Invokes a given method without arguments. Note to implementors: This method has a default
     * convenience implementation that invokes {@link #invokeMethod(MethodKey, Map)} with a null
     * argument map.
     *
     * <p>The returned future may throw one of the following checked exceptions (as the cause to a
     * {@link java.util.concurrent.ExecutionException}) when completed:
     *
     * <ul>
     *   <li>{@link MethodException}, or one of its subclasses:
     *       <ul>
     *         <li>{@link InvalidMethodArgumentsException}: The supplied arguments are invalid. This
     *             could be due to a missing required argument or an argument containing an illegal
     *             value.
     *         <li>{@link MethodNotFoundException}: This method is not supported on this functional
     *             endpoint.
     *       </ul>
     *   <li>{@link TechnologyException} if there was a technology-specific problem
     * </ul>
     *
     * <p>
     *
     * @param methodKey The {@link MethodKey} object associated with the method to invoke.
     * @return A future capable of retrieving the return value of the method, if any.
     */
    @CanIgnoreReturnValue
    default <T> ListenableFuture<T> invokeMethod(MethodKey<T> methodKey) {
        return invokeMethod(methodKey, ImmutableMap.of());
    }

    /**
     * Invokes a given method with named arguments specified in a {@link Map}.
     *
     * <p>The returned future may throw one of the following checked exceptions (as the cause to a
     * {@link java.util.concurrent.ExecutionException}) when completed:
     *
     * <ul>
     *   <li>{@link MethodException}, or one of its subclasses:
     *       <ul>
     *         <li>{@link InvalidMethodArgumentsException}: The supplied arguments are invalid. This
     *             could be due to a missing required argument or an argument containing an illegal
     *             value.
     *         <li>{@link MethodNotFoundException}: This method is not supported on this functional
     *             endpoint.
     *       </ul>
     *   <li>{@link TechnologyException} if there was a technology-specific problem
     * </ul>
     *
     * <p>
     *
     * @param methodKey The {@link MethodKey} object associated with the method to invoke.
     * @param arguments Map of named arguments. May be null.
     * @return A future capable of retrieving the return value of the method, if any.
     */
    @CanIgnoreReturnValue
    <T> ListenableFuture<T> invokeMethod(MethodKey<T> methodKey, Map<String, Object> arguments);

    /**
     * Invokes a given method with named arguments specified in-line as key-value pairs. Note to
     * implementors: This method has a default convenience implementation that invokes {@link
     * #invokeMethod(MethodKey, Map)}.
     *
     * <p>The returned future may throw one of the following checked exceptions (as the cause to a
     * {@link java.util.concurrent.ExecutionException}) when completed:
     *
     * <ul>
     *   <li>{@link MethodException}, or one of its subclasses:
     *       <ul>
     *         <li>{@link InvalidMethodArgumentsException}: The supplied arguments are invalid. This
     *             could be due to a missing required argument or an argument containing an illegal
     *             value.
     *         <li>{@link MethodNotFoundException}: This method is not supported on this functional
     *             endpoint.
     *       </ul>
     *   <li>{@link TechnologyException} if there was a technology-specific problem
     * </ul>
     *
     * <p>
     *
     * @param methodKey The {@link MethodKey} object associated with the method to invoke.
     * @param params the parameters to the method, created using {@link ParamKey#with(Object)}.
     * @return A future capable of retrieving the return value of the method, if any.
     */
    @CanIgnoreReturnValue
    default <T> ListenableFuture<T> invokeMethod(MethodKey<T> methodKey,
                                                 TypedKeyValue<?> ... params) {
        return invokeMethod(methodKey, TypedKeyValue.asMap(params));
    }

    /**
     * Fetches the list of child FunctionalEndpoints associated with a specific trait, identified by
     * its short id.
     *
     * <p>The returned future may throw one of the following checked exceptions (as the cause to a
     * {@link java.util.concurrent.ExecutionException}) when completed:
     *
     * <ul>
     *   <li>{@link TechnologyException} if there was a technology-specific problem
     * </ul>
     *
     * <p>
     *
     * @param traitShortId The short id of the trait
     * @return A future returning a collection of functional endpoints. The future will return null
     *     if the trait doesn't exist or doesn't support children.
     */
    ListenableFuture<Collection<FunctionalEndpoint>> fetchChildrenForTrait(String traitShortId);

    /**
     * Returns the short name of the trait that is providing the given child. Will return null if
     * this functional endpoint is not the parent of child.
     *
     * @see #getIdForChild(FunctionalEndpoint)
     */
    @Nullable
    String getTraitForChild(FunctionalEndpoint child);

    /**
     * Returns the identifier of the given child functional endpoint. Will return null if this
     * functional endpoint is not the parent of child.
     *
     * @see #getTraitForChild(FunctionalEndpoint)
     */
    @Nullable
    String getIdForChild(FunctionalEndpoint child);

    /**
     * Returns the functional endpoint for a child identified by a trait short name and child id.
     *
     * @see #getTraitForChild(FunctionalEndpoint)
     * @see #getIdForChild(FunctionalEndpoint)
     */
    @Nullable
    FunctionalEndpoint getChild(String traitShortId, String childId);

    /**
     * Returns the parent functional endpoint, if any. Will return null if this functional endpoint
     * has no parent.
     */
    @Nullable
    FunctionalEndpoint getParentFunctionalEndpoint();

    /**
     * Registers a {@link PropertyListener} to receive asynchronous notifications when the value of
     * a property has changed.
     *
     * @param executor the executor to use when making calls to the listener
     * @param key the key of the property to receive change notifications for
     * @param listener the listener to call when the value of the given property has changed
     * @see #unregisterPropertyListener(PropertyKey, PropertyListener)
     * @see #unregisterAllListeners()
     */
    <T> void registerPropertyListener(
            Executor executor, PropertyKey<T> key, PropertyListener<T> listener);

    /**
     * Unregisters a previously registered {@link PropertyListener}. Once unregistered, changes to
     * the property will no longer result in calls to the given listener.
     *
     * <p>If the listener is not currently registered for the given key or the listener is <code>
     * null</code>, then calling this method will do nothing.
     *
     * @param key the key of the property to no longer receive change notifications for
     * @param listener the listener to unregister
     * @see #unregisterAllListeners()
     */
    <T> void unregisterPropertyListener(PropertyKey<T> key, PropertyListener<T> listener);

    /**
     * Registers a {@link StateListener} to receive asynchronous notifications when any "state"
     * property has changed.
     *
     * @param executor the executor to use when making calls to the listener
     * @param listener the listener to call when the "state" has changed
     * @see #unregisterStateListener(StateListener)
     * @see #unregisterAllListeners()
     */
    void registerStateListener(Executor executor, StateListener listener);

    /**
     * Unregisters a previously registered {@link StateListener}. Once unregistered, changes to
     * "state" property values will no longer result in calls to the given listener.
     *
     * <p>If the listener is not currently registered for the given key or the listener is <code>
     * null</code>, then calling this method will do nothing.
     *
     * @param listener the listener to unregister
     * @see #registerStateListener(Executor, StateListener)
     * @see #unregisterAllListeners()
     */
    void unregisterStateListener(StateListener listener);

    /**
     * Registers a {@link ConfigListener} to receive asynchronous notifications when any "config"
     * property has changed.
     *
     * @param executor the executor to use when making calls to the listener
     * @param listener the listener to call when the "config" has changed
     * @see #unregisterConfigListener(ConfigListener)
     * @see #unregisterAllListeners()
     */
    void registerConfigListener(Executor executor, ConfigListener listener);

    /**
     * Unregisters a previously registered {@link ConfigListener}. Once unregistered, changes to
     * "config" property values will no longer result in calls to the given listener.
     *
     * <p>If the listener is not currently registered for the given key or the listener is <code>
     * null</code>, then calling this method will do nothing.
     *
     * @param listener the listener to unregister
     * @see #registerConfigListener(Executor, ConfigListener)
     * @see #unregisterAllListeners()
     */
    void unregisterConfigListener(ConfigListener listener);

    /**
     * Registers a {@link MetadataListener} to receive asynchronous notifications when any
     * "metadata" property has changed.
     *
     * @param executor the executor to use when making calls to the listener
     * @param listener the listener to call when the "metadata" has changed
     * @see #unregisterMetadataListener(MetadataListener)
     * @see #unregisterAllListeners()
     */
    void registerMetadataListener(Executor executor, MetadataListener listener);

    /**
     * Unregisters a previously registered {@link MetadataListener}. Once unregistered, changes to
     * "metadata" property values will no longer result in calls to the given listener.
     *
     * <p>If the listener is not currently registered for the given key or the listener is <code>
     * null</code>, then calling this method will do nothing.
     *
     * @param listener the listener to unregister
     * @see #registerMetadataListener(Executor,MetadataListener)
     * @see #unregisterAllListeners()
     */
    void unregisterMetadataListener(MetadataListener listener);

    /**
     * Registers a listener for children being added or removed.
     *
     * @param executor The executor to use for executing the callback on the listener.
     * @param listener The listener to call into when chindren are added or removed.
     * @param traitId The trait id of the trait to listen for changes to.
     *
     * @see #unregisterChildListener(ChildListener, String)
     * @see #unregisterAllListeners()
     */
    void registerChildListener(Executor executor, ChildListener listener, String traitId);

    /**
     * Unregisters a {@link ChildListener} that was previously registered with
     * {@link #registerChildListener(Executor, ChildListener, String)}.
     *
     * @param listener The listener to unregister. If the given listener was never
     *                 registered, then this method does nothing.
     * @param traitId The traitId to unregister from.
     *
     * @see #registerChildListener(Executor, ChildListener, String)
     * @see #unregisterAllListeners()
     */
    void unregisterChildListener(ChildListener listener, String traitId);

    /**
     * Unregisters all listeners from this functional endpoint.
     *
     * @see #registerChildListener(Executor, ChildListener, String)
     * @see #registerPropertyListener(Executor, PropertyKey, PropertyListener)
     * @see #registerMetadataListener(Executor, MetadataListener)
     * @see #registerStateListener(Executor, StateListener)
     * @see #registerConfigListener(Executor, ConfigListener)
     */
    void unregisterAllListeners();

    /**
     * {@hide} Generates a new 10-character random UID to be used with {@link
     * com.google.iot.m2m.trait.BaseTrait#META_UID}.
     */
    static String generateNewUid() {
        String uid;
        SecureRandom random = new SecureRandom();

        /* This loop just keeps calculating random base-64 strings
         * until it finds one that doesn't include "+" or "/".
         */
        do {
            byte[] bytes = new byte[8];
            random.nextBytes(bytes);
            uid = Base64.getEncoder().encodeToString(bytes).substring(0, 10);
        } while (uid.contains("+") || uid.contains("/"));

        return uid;
    }
}
