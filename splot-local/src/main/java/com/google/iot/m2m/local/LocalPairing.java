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
package com.google.iot.m2m.local;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.iot.m2m.base.*;
import com.google.iot.m2m.local.rpn.RPNContext;
import com.google.iot.m2m.local.rpn.RPNException;
import com.google.iot.m2m.trait.AutomationPairingTrait;
import com.google.iot.m2m.trait.BaseTrait;
import com.google.iot.m2m.trait.EnabledDisabledTrait;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.net.URI;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.logging.Logger;

public class LocalPairing extends LocalFunctionalEndpoint {
    private static final boolean DEBUG = false;
    private static final Logger LOGGER = Logger.getLogger(LocalPairing.class.getCanonicalName());

    private static final Object EMPTY = new Object();

    private RPNContext mSharedRPNContext = new RPNContext();
    private RPNContext mForwardRPNContext = new RPNContext(mSharedRPNContext);
    private RPNContext mReverseRPNContext = new RPNContext(mSharedRPNContext);

    // Resource link for the source
    private ResourceLink<Object> mSource = null;
    private final ResourceLink.Listener<Object> mSourceListener = (rl, value) -> handleSourceChange(value);
    private Object mSourceLastValue = EMPTY;

    // Resource link for the destination
    private ResourceLink<Object> mDestination = null;
    private final ResourceLink.Listener<Object> mDestinationListener = (rl, value) -> handleDestinationChange(value);
    private Object mDestinationLastValue = EMPTY;

    // Technology backing this pairing
    private final ResourceLinkManager mTechnology;

    private Executor mExecutor = Runnable::run;

    // True if changes to source should be applied to the destination
    private boolean mPush = true;

    // True if changes to destination should be applied to the source
    private boolean mPull = false;

    private String mPushTrap = null;

    private String mPullTrap = null;

    // True if this pairing is enabled.
    private boolean mEnabled = true;

    // Number of times this automation pairing has fired.
    private int mCount = 0;

    // Timestamp of last change.
    private long mTimestamp = 0;

    // Forward Transform
    private Function<Object, Object> mForwardTransform = (x) -> x;
    private String mForwardTransformRecipe = "";

    // Reverse Transform
    private Function<Object, Object> mReverseTransform = (x) -> x;
    private String mReverseTransformRecipe = "";

    // Minimum source difference required for change to propagate to the destination.
    private double mSourceEpsilon = 0.0001;

    // Minimum destination difference required for change to propagate to the source.
    private double mDestinationEpsilon = 0.0001;

    public LocalPairing(ResourceLinkManager technology) {
        mTechnology = technology;
        registerTrait(mBaseTrait);
        registerTrait(mPairingTrait);
        registerTrait(mEnabledDisabledTrait);
    }

    void handleSourceChange(@Nullable Object value) {
        ResourceLink<Object> rl = mDestination;

        if (rl == null || !mPush || Objects.equals(value, mSourceLastValue)) {
            return;
        }

        if (DEBUG) LOGGER.info("handleSourceChange: " + value);

        if (value instanceof Number && mSourceLastValue instanceof Number) {
            final double diff = Math.abs(((Number)value).doubleValue()
                    - ((Number)mSourceLastValue).doubleValue());
            if (diff < mSourceEpsilon) {
                return;
            }
        }

        mForwardRPNContext.setVariable("v", value);
        mForwardRPNContext.setVariable("v_l", mSourceLastValue);

        try {
            Object oldValue = value;
            value = mForwardTransform.apply(oldValue);
            mSourceLastValue = oldValue;
        } catch (RPNException x) {
            mPushTrap = x.toString();
            mBaseTrait.didChangeTrap(getTrapString());
            return;
        }

        if (RPNContext.isStopSignal(value) || Objects.equals(value, mDestinationLastValue)) {
            return;
        }

        if (value instanceof Number && mDestinationLastValue instanceof Number) {
            final double diff = Math.abs(((Number)value).doubleValue()
                    - ((Number)mDestinationLastValue).doubleValue());
            if (diff < mDestinationEpsilon) {
                return;
            }
        }

        mDestinationLastValue = value;

        if (mPushTrap != null) {
            // Clear the trap.
            mPushTrap = null;
            mBaseTrait.didChangeTrap(getTrapString());
        }

        ListenableFuture<?> invokedFuture = rl.invoke(value);

        invokedFuture.addListener(()->{
            try {
                invokedFuture.get();

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();

            } catch (ExecutionException e) {
                LOGGER.warning(AutomationPairingTrait.TRAP_DESTINATION_WRITE_FAIL + " " + e);
                mPushTrap = AutomationPairingTrait.TRAP_DESTINATION_WRITE_FAIL;
                mBaseTrait.didChangeTrap(getTrapString());
            }
        }, mExecutor);

        mCount++;
        mTimestamp = System.nanoTime();
        mPairingTrait.didChangeCount(mCount);
        mPairingTrait.didChangeLast(0);
    }

    void handleDestinationChange(@Nullable Object value) {
        ResourceLink<Object> rl = mSource;

        if (rl == null || !mPull || Objects.equals(value, mDestinationLastValue)) {
            return;
        }
        if (DEBUG) LOGGER.info("handleDestinationChange: " + value);

        if (value instanceof Number && mDestinationLastValue instanceof Number) {
            final double diff = Math.abs(((Number)value).doubleValue()
                    - ((Number)mDestinationLastValue).doubleValue());
            if (diff < mDestinationEpsilon) {
                return;
            }
        }

        mReverseRPNContext.setVariable("v", value);
        mReverseRPNContext.setVariable("v_l", mDestinationLastValue);

        try {
            Object oldValue = value;
            value = mReverseTransform.apply(oldValue);
            mDestinationLastValue = oldValue;
        } catch (RPNException x) {
            mPullTrap = x.toString();
            mBaseTrait.didChangeTrap(getTrapString());
            return;
        }

        if (RPNContext.isStopSignal(value) || Objects.equals(value, mSourceLastValue)) {
            return;
        }

        if (value instanceof Number && mSourceLastValue instanceof Number) {
            final double diff = Math.abs(((Number)value).doubleValue()
                    - ((Number)mSourceLastValue).doubleValue());
            if (diff < mSourceEpsilon) {
                return;
            }
        }

        mSourceLastValue = value;

        if (mPullTrap != null) {
            // Clear the trap.
            mPullTrap = null;
            mBaseTrait.didChangeTrap(getTrapString());
        }

        ListenableFuture<?> invokedFuture = rl.invoke(value);

        invokedFuture.addListener(()->{
            try {
                invokedFuture.get();

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();

            } catch (ExecutionException|CancellationException e) {
                LOGGER.warning(AutomationPairingTrait.TRAP_SOURCE_WRITE_FAIL + " " + e);
                mPullTrap = AutomationPairingTrait.TRAP_SOURCE_WRITE_FAIL;
                mBaseTrait.didChangeTrap(getTrapString());
            }
        }, mExecutor);

        mCount++;
        mTimestamp = System.nanoTime();
        mPairingTrait.didChangeCount(mCount);
        mPairingTrait.didChangeLast(0);
    }

    @Nullable String getTrapString() {
        String trap = "";

        if (mPushTrap != null) {
            trap += mPushTrap;
            if (mPullTrap != null) {
                trap += " ";
            }
        }

        if (mPullTrap != null) {
            trap += mPullTrap;
        }

        if (trap.isEmpty()) {
            return null;
        }

        return trap;
    }

    void enablePush() {
        if (mSource == null || mDestination == null) {
            return;
        }

        if (DEBUG) LOGGER.info("enablePush");

        if (mPushTrap != null) {
            // Clear the trap.
            mPushTrap = null;
            mBaseTrait.didChangeTrap(getTrapString());
        }

        mSourceLastValue = EMPTY;
        mDestinationLastValue = EMPTY;

        mSource.registerListener(mExecutor, mSourceListener);
    }

    void disablePush() {
        if (mSource == null) {
            return;
        }

        if (DEBUG) LOGGER.info("disablePush");

        mSource.unregisterListener(mSourceListener);
    }

    void enablePull() {
        if (mSource == null || mDestination == null) {
            return;
        }

        if (DEBUG) LOGGER.info("enablePull");

        if (mPullTrap != null) {
            // Clear the trap.
            mPullTrap = null;
            mBaseTrait.didChangeTrap(getTrapString());
        }

        mSourceLastValue = EMPTY;
        mDestinationLastValue = EMPTY;

        mDestination.registerListener(mExecutor, mDestinationListener);
    }

    void disablePull() {
        if (mDestination == null) {
            return;
        }

        if (DEBUG) LOGGER.info("disablePull");

        mDestination.unregisterListener(mDestinationListener);
    }

    BaseTrait.AbstractLocalTrait mBaseTrait = new BaseTrait.AbstractLocalTrait() {
        @Override
        public String onGetModel() throws TechnologyException {
            return "Pairing";
        }

        @Override
        public String onGetNameDefault() {
            return "Pairing";
        }

        @Override
        public String onGetManufacturer() throws TechnologyException {
            return "Splot for Java";
        }

        @Override
        public @Nullable String onGetTrap() {
            return getTrapString();
        }

        @Override
        public Boolean onGetPermanent()  {
            return getPermanent();
        }
    };

    protected boolean getPermanent() {
        return true;
    }

    EnabledDisabledTrait.AbstractLocalTrait mEnabledDisabledTrait = new EnabledDisabledTrait.AbstractLocalTrait() {
        @Override
        public Boolean onGetValue() {
            return mEnabled;
        }

        @Override
        public void onSetValue(@Nullable Boolean value) throws PropertyReadOnlyException, InvalidPropertyValueException, TechnologyException {
            if (value == null || value == mEnabled) {
                return;
            }

            if (value) {
                mEnabled = true;

                if (mPush) {
                    enablePush();
                }

                if (mPull) {
                    enablePull();
                }

            } else {
                mEnabled = false;

                if (mPush) {
                    disablePush();
                }

                if (mPull) {
                    disablePull();
                }
            }

            didChangeValue(value);
            changedPersistentState();
        }
    };

    AutomationPairingTrait.AbstractLocalTrait mPairingTrait = new AutomationPairingTrait.AbstractLocalTrait() {
        @Override
        public @Nullable URI onGetSource() throws TechnologyException {
            if (mSource == null) {
                return null;
            }
            return mSource.getUri();
        }

        @Override
        public void onSetSource(@Nullable URI value) throws PropertyReadOnlyException, InvalidPropertyValueException, TechnologyException {
            final ResourceLink<Object> newSource;

            if (value == null) {
                newSource = null;
            } else {
                try {
                    newSource = mTechnology.getResourceLinkForNativeUri(value);
                } catch (UnknownResourceException x) {
                    if (DEBUG) LOGGER.warning("onSetSource: " + x);
                    throw new InvalidPropertyValueException("Can't resolve src " + value, x);
                }
                if (DEBUG) LOGGER.info("onSetSource: newSource = " + newSource);
            }

            if (!Objects.equals(newSource, mSource)) {
                disablePull();
                disablePush();

                mSource = newSource;

                if (mEnabled) {
                    if (mPush) enablePush();
                    if (mPull) enablePull();
                }

                didChangeSource(onGetSource());
            }
        }

        @Override
        public @Nullable URI onGetDestination() throws TechnologyException {
            if (mDestination == null) {
                return null;
            }
            return mDestination.getUri();
        }

        @Override
        public void onSetDestination(@Nullable URI value) throws PropertyReadOnlyException, InvalidPropertyValueException, TechnologyException {
            final ResourceLink<Object> newDestination;

            if (value == null) {
                newDestination = null;
            } else {
                try {
                    newDestination = mTechnology.getResourceLinkForNativeUri(value);
                } catch (UnknownResourceException x) {
                    if (DEBUG) LOGGER.warning("onSetDestination: " + x);
                    throw new InvalidPropertyValueException("Can't resolve dst " + value, x);
                }
                if (DEBUG) LOGGER.info("onSetDestination: newDestination = " + newDestination);
            }

            if (!Objects.equals(newDestination, mDestination)) {
                disablePull();
                disablePush();

                mDestination = newDestination;

                if (mEnabled) {
                    if (mPush) enablePush();
                    if (mPull) enablePull();
                }

                didChangeDestination(onGetDestination());
            }
        }

        @Override
        public Boolean onGetPush() {
            return mPush;
        }

        @Override
        public Boolean onGetPull() {
            return mPull;
        }

        @Override
        public Integer onGetCount() {
            return mCount;
        }

        @Override
        public @Nullable String onGetForwardTransform() {
            return mForwardTransformRecipe;
        }

        @Override
        public @Nullable String onGetReverseTransform() {
            return mReverseTransformRecipe;
        }

        @Override
        public void onSetForwardTransform(@Nullable String value) throws PropertyReadOnlyException, InvalidPropertyValueException, TechnologyException {
            if (value == null) {
                value = "";
            }

            try {
                mForwardTransform = mForwardRPNContext.compile(value);
                mForwardTransformRecipe = value;
            } catch(RPNException e) {
                throw new InvalidPropertyValueException(e);
            }

            Object last = mSourceLastValue;
            mSourceLastValue = EMPTY;
            mDestinationLastValue = EMPTY;
            didChangeForwardTransform(value);
            if (mEnabled && mPush) {
                handleSourceChange(last);
            }
        }

        @Override
        public void onSetReverseTransform(@Nullable String value) throws PropertyReadOnlyException, InvalidPropertyValueException, TechnologyException {
            if (value == null) {
                value = "";
            }

            try {
                mReverseTransform = mReverseRPNContext.compile(value);
                mReverseTransformRecipe = value;
            } catch(RPNException e) {
                throw new InvalidPropertyValueException(e);
            }

            Object last = mDestinationLastValue;
            mSourceLastValue = EMPTY;
            mDestinationLastValue = EMPTY;
            didChangeReverseTransform(value);
            if (mEnabled && mPull) {
                handleDestinationChange(last);
            }
        }

        @Override
        public void onSetPush(@Nullable Boolean value) throws PropertyReadOnlyException, InvalidPropertyValueException, TechnologyException {
            if (value == null || Boolean.TRUE.equals(value) == mPush) {
                // We just set the same value. Do nothing.
                return;
            }

            if (mPush) {
                mPush = false;
                disablePush();
            } else {
                mPush = true;
                if (mEnabled) {
                    enablePush();
                }
            }

            didChangePush(mPush);
        }

        @Override
        public void onSetPull(@Nullable Boolean value) throws PropertyReadOnlyException, InvalidPropertyValueException, TechnologyException {
            if (value == null || Boolean.TRUE.equals(value) == mPull) {
                // We just set the same value. Do nothing.
                return;
            }

            if (mPull) {
                mPull = false;
                disablePull();
            } else {
                mPull = true;
                if (mEnabled) {
                    enablePull();
                }
            }

            didChangePull(mPull);
        }

        @Override
        @Nullable
        public Integer onGetLast() throws TechnologyException {
            long last = System.nanoTime() - mTimestamp;

            if (mTimestamp == 0 || last < 0) {
                return null;
            }

            return (int)TimeUnit.NANOSECONDS.toSeconds(last);
        }
    };
}
