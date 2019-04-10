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

import com.google.iot.m2m.base.*;
import com.google.iot.m2m.local.rpn.RPNContext;
import com.google.iot.m2m.local.rpn.RPNException;
import com.google.iot.m2m.trait.*;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.logging.Logger;

/**
 * {@link LocalFunctionalEndpoint} that implements {@link AutomationTimerTrait} and
 * {@link ActionsTrait}. These are typically created automatically by
 * {@link LocalAutomationManager}/{@link LocalTimerManagerTrait}, but can be created
 * individually if needed.
 */
public class LocalTimer extends LocalActions {
    private static final boolean DEBUG = false;
    private static final Logger LOGGER = Logger.getLogger(LocalTimer.class.getCanonicalName());

    private RPNContext mSharedRPNContext = new RPNContext();
    private RPNContext mScheduleRPNContext = new RPNContext(mSharedRPNContext);
    private RPNContext mPredicateRPNContext = new RPNContext(mSharedRPNContext);

    private long mFireTime = 0;

    private ScheduledExecutorService mExecutor = Utils.getDefaultExecutor();

    private Future<?> mTimer = null;

    private boolean mAutoReset = false;

    private boolean mAutoDelete = false;

    // True if this timer is enabled.
    private boolean mEnabled = false;

    // Schedule Program
    private Function<Object, Object> mScheduleProgram = (x) -> x;
    private String mScheduleProgramRecipe = "";

    // Predicate Program
    private Function<Object, Object> mPredicateProgram = (x) -> true;
    private String mPredicateProgramRecipe = "";

    private String mTrap = null;

    public LocalTimer(ResourceLinkManager technology) {
        super(technology);
        registerTrait(mBaseTrait);
        registerTrait(mTimerTrait);
        registerTrait(mEnabledDisabledTrait);
    }

    @Override
    protected ScheduledExecutorService getExecutor() {
        return mExecutor;
    }

    private void updateRpnContextVariables() {
        mSharedRPNContext.setVariable("c", getCount());
        mSharedRPNContext.updateRtcVariables(Calendar.getInstance());
    }

    private void stopTimer() {
        if (mTimer != null && !mTimer.isDone()) {
            mTimer.cancel(false);
            if (DEBUG) LOGGER.info("Timer stopped");
        }
        mTimer = null;
    }

    private void resetTimer() {
        if (mEnabled) {
            stopTimer();

            long now = System.nanoTime();
            Object nextObj = mScheduleProgram.apply(null);
            if (nextObj instanceof Number) {
                double seconds = ((Number) nextObj).doubleValue();
                if (seconds > 0) {
                    long nanoseconds = TimeUnit.MILLISECONDS.toNanos((long) (seconds * 1000));
                    mFireTime = now + nanoseconds;

                    mTimer = getExecutor().schedule(
                            this::handleTimerFired, nanoseconds, TimeUnit.NANOSECONDS);
                    mTimerTrait.didChangeNext((float) seconds);
                    if (DEBUG) LOGGER.info("Timer started, will fire in " + seconds + "s (" + nanoseconds + "ns)");
                } else {
                    if (DEBUG) LOGGER.info("Schedule program refused to emit time to next event (value too small)");
                }
            } else {
                if (DEBUG) LOGGER.info("Schedule program refused to emit time to next event (Not a number)");
            }
        }
    }

    private boolean doesPredicatePass() {
        Object predObj = mPredicateProgram.apply(true);

        try {
            if (predObj != null && TypeConverter.BOOLEAN.coerceNonNull(predObj)) {
                if (DEBUG) LOGGER.info("Predicate passed");
                return true;
            }
        } catch (InvalidValueException e) {
            e.printStackTrace();
        }

        if (DEBUG) LOGGER.info("Predicate failed");
        return false;
    }

    private boolean isRunning() {
        Future<?> timer = mTimer;
        return timer != null && !timer.isDone();
    }

    @Override
    protected void invoke() {
        if (mTrap != null) {
            mTrap = null;
            mBaseTrait.didChangeTrap(null);
        }
        super.invoke();
        mSharedRPNContext.setVariable("c", getCount());
    }

    @Override
    protected void onInvokeError(int actionIndex, String errorToken) {
        String trap = actionIndex + ":" + errorToken;
        mTrap = trap;
        mBaseTrait.didChangeTrap(trap);
    }

    private void handleTimerFired() {
        if (DEBUG) LOGGER.info("handleTimerFired");

        updateRpnContextVariables();

        if (!doesPredicatePass()) {
            resetTimer();
            return;
        }

        invoke();

        if (mAutoReset) {
            if (DEBUG) LOGGER.info("Auto restart");
            resetTimer();
        }

        if (!isRunning()) {
            if (DEBUG) LOGGER.info("Timer done for now");
            stopTimer();
            mTimerTrait.didChangeRunning(false);
            mTimerTrait.didChangeNext(null);
            changedPersistentState();
        }

        if (!isRunning() && mAutoDelete) {
            if (DEBUG) LOGGER.info("Auto delete");
            delete();
        }
    }

    private BaseTrait.AbstractLocalTrait mBaseTrait = new BaseTrait.AbstractLocalTrait() {
        @Override
        public String onGetModel() {
            return "Timer";
        }

        @Override
        public String onGetNameDefault() {
            return "Timer";
        }

        @Override
        public String onGetManufacturer() {
            return "Splot for Java";
        }

        @Override
        public Boolean onGetPermanent()  {
            return getPermanent();
        }

        @Override
        public @Nullable String onGetTrap() {
            return mTrap;
        }
    };

    protected boolean getPermanent() {
        return true;
    }

    private EnabledDisabledTrait.AbstractLocalTrait mEnabledDisabledTrait = new EnabledDisabledTrait.AbstractLocalTrait() {
        @Override
        public Boolean onGetValue() {
            return mEnabled;
        }

        @Override
        public void onSetValue(@Nullable Boolean value) {

            if (value == null || value == mEnabled) {
                return;
            }

            if (value) {
                mEnabled = true;

                if (mAutoReset) {
                    updateRpnContextVariables();
                    resetCount();
                    mSharedRPNContext.setVariable("c", getCount());
                    resetTimer();
                }

                if (isRunning()) {
                    if (DEBUG) LOGGER.info("Enabled and running");
                    mTimerTrait.didChangeRunning(isRunning());
                } else {
                    if (DEBUG) LOGGER.info("Enabled and idle");
                }

            } else {
                boolean wasRunning = isRunning();

                mEnabled = false;

                stopTimer();

                if (wasRunning) {
                    mTimerTrait.didChangeRunning(false);
                }

                if (DEBUG) LOGGER.info("Disabled");
            }

            didChangeValue(value);
            changedPersistentState();
        }
    };

    private AutomationTimerTrait.AbstractLocalTrait mTimerTrait = new AutomationTimerTrait.AbstractLocalTrait() {
        @Override
        public Boolean onGetRunning() {
            return isRunning();
        }

        @Override
        public @Nullable Float onInvokeReset(Map<String, Object> ignored) {
            if (mEnabled) {
                if (mAutoReset) {
                    resetCount();
                }
                mSharedRPNContext.setVariable("c", getCount());
                resetTimer();
                mTimerTrait.didChangeRunning(false);
            }

            return null;
        }

        @Override
        public void onSetRunning(@Nullable Boolean value) throws BadStateForPropertyValueException {
            if (value == null || value.equals(isRunning())) {
                return;
            }

            if (value) {
                updateRpnContextVariables();
                resetTimer();
                if (isRunning()) {
                    didChangeRunning(true);
                } else {
                    LOGGER.info("Failed to explicitly start");
                    throw new BadStateForPropertyValueException("Unable to start");
                }
            } else {
                stopTimer();
                didChangeRunning(false);
                didChangeNext(null);
            }
        }

        @Override
        public @Nullable String onGetScheduleProgram() {
            return mScheduleProgramRecipe;
        }

        @Override
        public void onSetScheduleProgram(@Nullable String value) throws InvalidPropertyValueException {
            if (value == null) {
                value = "";
            }

            if (value.equals(mScheduleProgramRecipe)) {
                return;
            }

            try {
                mScheduleProgram = mScheduleRPNContext.compile(value);
                mScheduleProgramRecipe = value;
            } catch(RPNException e) {
                throw new InvalidPropertyValueException(e);
            }

            if (isRunning()) {
                updateRpnContextVariables();
                resetTimer();
            }

            didChangeScheduleProgram(value);
        }

        @Override
        public @Nullable String onGetPredicateProgram() {
            return mPredicateProgramRecipe;
        }

        @Override
        public void onSetPredicateProgram(@Nullable String value) throws InvalidPropertyValueException {
            if (value == null) {
                value = "";
            }

            if (value.equals(mPredicateProgramRecipe)) {
                return;
            }

            try {
                mPredicateProgram = mPredicateRPNContext.compile(value);
                mPredicateProgramRecipe = value;
            } catch(RPNException e) {
                throw new InvalidPropertyValueException(e);
            }

            didChangePredicateProgram(value);
        }

        @Override
        public Boolean onGetAutoReset() {
            return mAutoReset;
        }

        @Override
        public Boolean onGetAutoDelete() {
            return mAutoDelete;
        }

        @Override
        public void onSetAutoDelete(@Nullable Boolean value) {
            if (value == null || value.equals(mAutoDelete)) {
                return;
            }
            mAutoDelete = value;
            didChangeAutoDelete(value);
        }

        @Override
        public void onSetAutoReset(@Nullable Boolean value)  {
            if (value == null || value.equals(mAutoReset)) {
                return;
            }
            mAutoReset = value;
            didChangeAutoReset(value);
        }

        @Override
        @Nullable
        public Float onGetNext() {
            if (!isRunning() || mFireTime == 0) {
                return null;
            }

            long next = mFireTime - System.nanoTime();

            if (next < 0) {
                return null;
            }

            return TimeUnit.NANOSECONDS.toMillis(next) / 1000.0f;
        }
    };

    @Override
    public void initWithPersistentState(@Nullable Map<String, Object> persistentState) {
        Boolean enabled = null;
        Boolean running = null;

        if (persistentState != null) {
            enabled = EnabledDisabledTrait.STAT_VALUE.coerceFromMapNoThrow(persistentState);
            running = AutomationTimerTrait.STAT_RUNNING.coerceFromMapNoThrow(persistentState);

            if (enabled != null && running != null && !Objects.equals(enabled,running)) {
                AutomationTimerTrait.STAT_RUNNING.removeFromMap(persistentState);
            }
            EnabledDisabledTrait.STAT_VALUE.removeFromMap(persistentState);
        }

        super.initWithPersistentState(persistentState);

        if (enabled != null) {
            mEnabled = enabled;
            if (running != null && running) {
                updateRpnContextVariables();
                resetTimer();
            }
        }
    }
}
