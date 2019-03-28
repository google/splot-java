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
import com.google.iot.m2m.trait.AutomationTimerTrait;
import com.google.iot.m2m.trait.BaseTrait;
import com.google.iot.m2m.trait.EnabledDisabledTrait;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoField;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.logging.Logger;

public class LocalTimer extends LocalFunctionalEndpoint {
    private static final boolean DEBUG = false;
    private static final Logger LOGGER = Logger.getLogger(LocalTimer.class.getCanonicalName());

    private RPNContext mSharedRPNContext = new RPNContext();
    private RPNContext mScheduleRPNContext = new RPNContext(mSharedRPNContext);
    private RPNContext mPredicateRPNContext = new RPNContext(mSharedRPNContext);

    private long mFireTime = 0;
    private long mLastFiredTime = 0;

    @SuppressWarnings("unchecked")
    private Map<String,Object>[] mActionsInfo = new Map[0];

    class Action {
        ResourceLink<Object> mResourceLink;
        Object mBody;

        Action(ResourceLink<Object> resourceLink, @Nullable Object body) {
            mResourceLink = resourceLink;
            mBody = body;
        }

        void invoke() {
            if (DEBUG) LOGGER.info("Invoking " + mResourceLink + " with " + mBody);
            ListenableFuture<?> future = mResourceLink.invoke(mBody);

            future.addListener(()->{
                try {
                    future.get();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException|CancellationException e) {
                    LOGGER.warning("Caught exception on action invoke: " + e);
                    e.printStackTrace();
                }
            }, mExecutor);
        }
    }

    private List<Action> mActions = new ArrayList<>();

    // Technology backing this timer
    private final Technology mTechnology;

    private ScheduledExecutorService mExecutor = Utils.getDefaultExecutor();

    private Future<?> mTimer = null;

    private boolean mAutoReset = false;

    private boolean mAutoDelete = false;

    // True if this timer is enabled.
    private boolean mEnabled = false;

    // Number of times this automation pairing has fired.
    private int mCount = 0;

    // Schedule Program
    private Function<Object, Object> mScheduleProgram = (x) -> x;
    private String mScheduleProgramRecipe = "";

    // Predicate Program
    private Function<Object, Object> mPredicateProgram = (x) -> true;
    private String mPredicateProgramRecipe = "";

    public LocalTimer(Technology technology) {
        mTechnology = technology;
        registerTrait(mBaseTrait);
        registerTrait(mTimerTrait);
        registerTrait(mEnabledDisabledTrait);
    }

    void updateRpnContextVariables() {
        final int SECONDS_PER_HOUR = 60*60;

        Calendar now = Calendar.getInstance();

        now.setFirstDayOfWeek(Calendar.MONDAY);

        mSharedRPNContext.setVariable("c", mCount);
        mSharedRPNContext.setVariable("rtc.dom", now.get(Calendar.DAY_OF_MONTH) - now.getMinimum(Calendar.DAY_OF_MONTH));
        mSharedRPNContext.setVariable("rtc.doy", now.get(Calendar.DAY_OF_YEAR)  - now.getMinimum(Calendar.DAY_OF_YEAR));
        mSharedRPNContext.setVariable("rtc.moy", now.get(Calendar.MONTH) - now.getMinimum(Calendar.MONTH));
        mSharedRPNContext.setVariable("rtc.awm", now.get(Calendar.DAY_OF_WEEK_IN_MONTH) - now.getMinimum(Calendar.DAY_OF_WEEK_IN_MONTH));
        mSharedRPNContext.setVariable("rtc.y", now.get(Calendar.YEAR));
        mSharedRPNContext.setVariable("rtc.wom", now.get(Calendar.WEEK_OF_MONTH) - now.getMinimum(Calendar.WEEK_OF_MONTH));
        mSharedRPNContext.setVariable("rtc.woy", now.get(Calendar.WEEK_OF_YEAR) - now.getMinimum(Calendar.WEEK_OF_YEAR));

        int secondOfDay = now.get(Calendar.SECOND)
                + now.get(Calendar.MINUTE)*60
                + now.get(Calendar.HOUR_OF_DAY)*3600;
        mSharedRPNContext.setVariable("rtc.tod", (double)secondOfDay/SECONDS_PER_HOUR);

        // Convert to zero-based day with monday as start of week.
        int dow = now.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY;
        if (dow < 0) {
            dow = 7 - dow;
        }
        mSharedRPNContext.setVariable("rtc.dow", dow);
    }

    void stopTimer() {
        if (mTimer != null && !mTimer.isDone()) {
            mTimer.cancel(false);
            if (DEBUG) LOGGER.info("Timer stopped");
        }
        mTimer = null;
    }

    void resetTimer() {
        if (mEnabled) {
            stopTimer();

            long now = System.nanoTime();
            Object nextObj = mScheduleProgram.apply(null);
            if (nextObj instanceof Number) {
                double seconds = ((Number) nextObj).doubleValue();
                if (seconds > 0) {
                    long nanoseconds = TimeUnit.MILLISECONDS.toNanos((long) (seconds * 1000));
                    mFireTime = now + nanoseconds;

                    mTimer = mExecutor.schedule(
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

    boolean doesPredicatePass() {
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

    boolean isRunning() {
        Future<?> timer = mTimer;
        return timer != null && !timer.isDone();
    }

    void handleTimerFired() {
        if (DEBUG) LOGGER.info("handleTimerFired");

        updateRpnContextVariables();

        if (!doesPredicatePass()) {
            resetTimer();
            return;
        }

        List<Action> actions = mActions;

        for (Action action : actions) {
            action.invoke();
        }

        mCount++;
        mLastFiredTime = System.nanoTime();
        mTimerTrait.didChangeCount(mCount);
        mTimerTrait.didChangeLast(0);
        mSharedRPNContext.setVariable("c", mCount);

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

    BaseTrait.AbstractLocalTrait mBaseTrait = new BaseTrait.AbstractLocalTrait() {
        @Override
        public String onGetModel() throws TechnologyException {
            return "Timer";
        }

        @Override
        public String onGetNameDefault() {
            return "Timer";
        }

        @Override
        public String onGetManufacturer() throws TechnologyException {
            return "Splot for Java";
        }
    };

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

                if (mAutoReset) {
                    updateRpnContextVariables();
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

    AutomationTimerTrait.AbstractLocalTrait mTimerTrait = new AutomationTimerTrait.AbstractLocalTrait() {
        @Override
        public Integer onGetCount() {
            return mCount;
        }

        @Override
        public Boolean onGetRunning() {
            return isRunning();
        }

        @Override
        public @Nullable Float onInvokeReset(Map<String, Object> ignored) throws InvalidMethodArgumentsException {
            if (mEnabled) {
                if (mAutoReset) {
                    mCount = 0;
                    didChangeCount(0);
                }
                mSharedRPNContext.setVariable("c", mCount);
                resetTimer();
                mTimerTrait.didChangeRunning(false);
            }

            return null;
        }

        @Override
        public void onSetRunning(@Nullable Boolean value) throws BadStateForPropertyValueException,PropertyReadOnlyException, InvalidPropertyValueException, TechnologyException {
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
        public @Nullable String onGetScheduleProgram() throws TechnologyException {
            return mScheduleProgramRecipe;
        }

        @Override
        public void onSetScheduleProgram(@Nullable String value) throws PropertyReadOnlyException, InvalidPropertyValueException, TechnologyException {
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
        public @Nullable String onGetPredicateProgram() throws TechnologyException {
            return mPredicateProgramRecipe;
        }

        @Override
        public void onSetPredicateProgram(@Nullable String value) throws PropertyReadOnlyException, InvalidPropertyValueException, TechnologyException {
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

        @Nullable
        @Override
        public Map<String, Object>[] onGetActions() throws TechnologyException {
            return mActionsInfo;
        }

        @Override
        public void onSetActions(@Nullable Map<String, Object>[] value) throws PropertyReadOnlyException, InvalidPropertyValueException, TechnologyException {
            if (value == null || Arrays.equals(mActionsInfo, value)) {
                return;
            }

            List<Action> actions = new ArrayList<>();
            try {
                for (Map<String, Object> actionInfo : value) {
                    if (actionInfo == null) {
                        throw new InvalidPropertyValueException("Action can't be null");
                    }

                    String method = "POST";
                    Object body = null;
                    URI path = AutomationTimerTrait.PARAM_ACTION_PATH.coerceFromMap(actionInfo);

                    if (path == null) {
                        throw new InvalidPropertyValueException("Action path can't be null");
                    }

                    if (AutomationTimerTrait.PARAM_ACTION_METH.isInMap(actionInfo)) {
                        method = AutomationTimerTrait.PARAM_ACTION_METH.getFromMap(actionInfo);
                    }

                    if (!"POST".equals(method)) {
                        throw new InvalidPropertyValueException("Method \"" + method + "\" not yet supported");
                    }

                    if (AutomationTimerTrait.PARAM_ACTION_BODY.isInMap(actionInfo)) {
                        body = AutomationTimerTrait.PARAM_ACTION_BODY.getFromMap(actionInfo);
                    }

                    ResourceLink<Object> resourceLink = mTechnology.getResourceLinkForNativeUri(path);

                    if (DEBUG) LOGGER.info("Will " + method + " to " + path + " with body of " + body);
                    actions.add(new Action(resourceLink, body));
                }
            } catch (InvalidValueException e) {
                throw new InvalidPropertyValueException(e);

            } catch (UnknownResourceException e) {
                throw new TechnologyException(e);
            }

            mActionsInfo = value;
            mActions = actions;

            didChangeActions(mActionsInfo);
        }

        @Override
        public Boolean onGetAutoReset() throws TechnologyException {
            return mAutoReset;
        }

        @Override
        public Boolean onGetAutoDelete() throws TechnologyException {
            return mAutoDelete;
        }

        @Override
        public void onSetAutoDelete(@Nullable Boolean value) throws PropertyReadOnlyException, InvalidPropertyValueException, TechnologyException {
            if (value == null || value.equals(mAutoDelete)) {
                return;
            }
            mAutoDelete = value;
            didChangeAutoDelete(value);
        }

        @Override
        public void onSetAutoReset(@Nullable Boolean value) throws PropertyReadOnlyException, InvalidPropertyValueException, TechnologyException {
            if (value == null || value.equals(mAutoReset)) {
                return;
            }
            mAutoReset = value;
            didChangeAutoReset(value);
        }

        @Override
        @Nullable
        public Float onGetNext() throws TechnologyException {
            if (!isRunning() || mFireTime == 0) {
                return null;
            }

            long next = mFireTime - System.nanoTime();

            if (next < 0) {
                return null;
            }

            return TimeUnit.NANOSECONDS.toMillis(next) / 1000.0f;
        }

        @Override
        @Nullable
        public Integer onGetLast() throws TechnologyException {
            long last = System.nanoTime() - mLastFiredTime;

            if (mLastFiredTime == 0 || last < 0) {
                return null;
            }

            return (int)TimeUnit.NANOSECONDS.toSeconds(last);
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
