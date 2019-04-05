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

import java.net.URI;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.logging.Logger;

import static com.google.iot.m2m.trait.AutomationRuleTrait.MATCH_ALL;
import static com.google.iot.m2m.trait.AutomationRuleTrait.MATCH_ANY;

public class LocalRule extends LocalActions {
    private static final boolean DEBUG = false;
    private static final Logger LOGGER = Logger.getLogger(LocalRule.class.getCanonicalName());

    private RPNContext mSharedRPNContext = new RPNContext();

    // True if this rule is enabled.
    private boolean mEnabled = false;

    private boolean mMatchAll = true;

    @SuppressWarnings("unchecked")
    private static final Map<String, Object>[] EMPTY_MAP_ARRAY = new Map[]{};

    @SuppressWarnings("unchecked")
    private Map<String,Object>[] mConditionsInfo = new Map[0];

    class Condition implements ResourceLink.Listener<Object> {
        final ResourceLink<Object> mResourceLink;
        Function<Object, Object> mExpression = (x) -> true;

        volatile Object mLastValue = null;
        volatile Object mCurrValue = null;

        Condition(ResourceLink<Object> resourceLink) {
            mResourceLink = resourceLink;
        }

        void beginMonitoring() {
            mResourceLink.registerListener(getExecutor(), this);
        }

        void endMonitoring() {
            mResourceLink.unregisterListener(this);
        }

        boolean evaluate() {
            boolean ret;
            if (DEBUG) LOGGER.info("evaluate: currValue: " + mCurrValue);
            if (DEBUG) LOGGER.info("evaluate: lastValue: " + mLastValue);
            try {
                Object value = mCurrValue;
                mSharedRPNContext.setVariable("v", value);
                mSharedRPNContext.setVariable("v_l", mLastValue);
                mLastValue = mCurrValue;
                ret = TypeConverter.BOOLEAN.coerceNonNull(mExpression.apply(value));
            } catch (InvalidValueException ignore) {
                ret = false;
            }
            if (DEBUG) LOGGER.info("Evaluating "
                    + mResourceLink.getUri() + " -> " + ret);

            return ret;
        }

        @Override
        public void onResourceLinkChanged(ResourceLink rl, @Nullable Object value) {
            synchronized (LocalRule.this) {
                mLastValue = mCurrValue;
                mCurrValue = value;
            }
            scheduleEvaluateConditions();
        }
    }

    private List<Condition> mConditions = new ArrayList<>();

    private ScheduledExecutorService mExecutor = Utils.getDefaultExecutor();

    private AtomicBoolean mEvaluationPending = new AtomicBoolean(false);
    private Future<?> mScheduledEvaluation = null;

    @Override
    protected ScheduledExecutorService getExecutor() {
        return mExecutor;
    }

    public LocalRule(ResourceLinkManager technology) {
        super(technology);
        registerTrait(mBaseTrait);
        registerTrait(mRuleTrait);
        registerTrait(mEnabledDisabledTrait);
        updateRpnContextVariables();
    }

    private void updateRpnContextVariables() {
        mSharedRPNContext.setVariable("c", getCount());
        mSharedRPNContext.updateRtcVariables(Calendar.getInstance());
    }

    @Override
    protected void invoke() {
        super.invoke();
        mSharedRPNContext.setVariable("c", getCount());
    }

    private synchronized void beginMonitoringConditions() {
        if (DEBUG) LOGGER.info("beginMonitoringConditions");

        for (Condition condition : mConditions) {
            condition.beginMonitoring();
        }

        mEvaluationPending.set(false);
    }

    private synchronized void endMonitoringConditions() {
        if (DEBUG) LOGGER.info("endMonitoringConditions");

        if (mScheduledEvaluation != null) {
            mScheduledEvaluation.cancel(true);
            mScheduledEvaluation = null;
        }

        for (Condition condition : mConditions) {
            condition.endMonitoring();
        }

        mEvaluationPending.set(false);
    }

    private void scheduleEvaluateConditions() {
        if (DEBUG) LOGGER.info("scheduleEvaluateConditions");
        if (mEvaluationPending.compareAndSet(false, true)) {
            synchronized (this) {
                mScheduledEvaluation = getExecutor().submit(this::evaluateConditions);
            }
        }
    }

    private synchronized void evaluateConditions() {
        mScheduledEvaluation = null;
        if (DEBUG) LOGGER.info("evaluateConditions: Evaluating...");
        try {
            if (mConditions.isEmpty()) {
                // Don't let empty conditions trigger stuff.
                return;
            }

            updateRpnContextVariables();

            for (Condition condition : mConditions) {
                if (!condition.evaluate()) {
                    // This condition isn't satisfied.

                    if (mMatchAll) {
                        // We are triggering only if everything matches.
                        if (DEBUG) LOGGER.info("evaluateConditions: FAIL");
                        return;
                    }
                } else {
                    // This condition IS satisfied.

                    if (!mMatchAll) {
                        // We are triggering on any match.
                        if (DEBUG) LOGGER.info("evaluateConditions: PASS");
                        invoke();
                        return;
                    }
                }
            }

            if (mMatchAll) {
                if (DEBUG) LOGGER.info("evaluateConditions: PASS");
                invoke();
            } else {
                if (DEBUG) LOGGER.info("evaluateConditions: FAIL");
            }
        } finally {
            mEvaluationPending.set(false);
        }
    }

    private BaseTrait.AbstractLocalTrait mBaseTrait = new BaseTrait.AbstractLocalTrait() {
        @Override
        public String onGetModel() {
            return "Rule";
        }

        @Override
        public String onGetNameDefault() {
            return "Rule";
        }

        @Override
        public String onGetManufacturer() {
            return "Splot for Java";
        }

        @Override
        public Boolean onGetPermanent()  {
            return getPermanent();
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

                beginMonitoringConditions();

            } else {
                mEnabled = false;

                endMonitoringConditions();
            }

            didChangeValue(value);
            changedPersistentState();
        }
    };

    private AutomationRuleTrait.AbstractLocalTrait mRuleTrait = new AutomationRuleTrait.AbstractLocalTrait() {
        @Nullable
        @Override
        public Map<String, Object>[] onGetConditions() {
            return mConditionsInfo;
        }

        @Override
        public void onSetConditions(@Nullable Map<String, Object>[] value) throws InvalidPropertyValueException {
            if (Arrays.equals(mConditionsInfo, value)) {
                return;
            }

            List<Condition> conditions = new ArrayList<>();

            if (value == null) {
                // Clear the list.
                value = EMPTY_MAP_ARRAY;

            } else {
                try {
                    for (Map<String, Object> conditionInfo : value) {
                        if (conditionInfo == null) {
                            throw new InvalidPropertyValueException("Condition can't be null");
                        }

                        // Do we skip this action?
                        if (AutomationRuleTrait.PARAM_COND_SKIP.isInMap(conditionInfo)) {
                            Boolean skip = AutomationRuleTrait.PARAM_COND_SKIP.coerceFromMap(conditionInfo);
                            if (skip != null && skip) {
                                continue;
                            }
                        }

                        URI path = AutomationRuleTrait.PARAM_COND_PATH.coerceFromMap(conditionInfo);

                        if (path == null) {
                            throw new InvalidPropertyValueException("Condition path can't be null");
                        }

                        String expression = AutomationRuleTrait.PARAM_COND_EXPR.coerceFromMap(conditionInfo);

                        if (expression == null) {
                            expression = "TRUE";
                        }

                        ResourceLink<Object> resourceLink = getTechnology().getResourceLinkForNativeUri(path);
                        Condition condition = new Condition(resourceLink);

                        condition.mExpression = mSharedRPNContext.compile(expression);

                        conditions.add(condition);
                    }
                } catch (InvalidValueException | UnknownResourceException | RPNException e) {
                    throw new InvalidPropertyValueException(e);
                }
            }

            synchronized (LocalRule.this) {
                if (mEnabled) {
                    endMonitoringConditions();
                }

                mConditionsInfo = value;
                mConditions = conditions;

                if (mEnabled) {
                    beginMonitoringConditions();
                }
            }

            didChangeConditions(mConditionsInfo);
        }

        @Override
        public String onGetMatch() {
            return mMatchAll ? MATCH_ALL : MATCH_ANY;
        }

        @Override
        public void onSetMatch(@Nullable String value) throws InvalidPropertyValueException {
            if (value == null) {
                value = MATCH_ALL;
            }

            boolean matchAll;

            if (MATCH_ALL.equals(value)) {
                matchAll = true;

            } else if (MATCH_ANY.equals(value)) {
                matchAll = false;
            } else {
                throw new InvalidPropertyValueException("Invalid value \"" + value + "\"");
            }

            if (matchAll != mMatchAll) {
                mMatchAll = matchAll;
                if (mEnabled) {
                    scheduleEvaluateConditions();
                }
                didChangeMatch(onGetMatch());
            }
        }
    };

    @Override
    public void initWithPersistentState(@Nullable Map<String, Object> persistentState) {
        Boolean enabled = null;

        if (persistentState != null) {
            enabled = EnabledDisabledTrait.STAT_VALUE.coerceFromMapNoThrow(persistentState);

            EnabledDisabledTrait.STAT_VALUE.removeFromMap(persistentState);
        }

        super.initWithPersistentState(persistentState);

        if (enabled != null) {
            mEnabled = enabled;
            getExecutor().execute(this::beginMonitoringConditions);
        }
    }
}
