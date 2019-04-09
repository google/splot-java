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

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.iot.m2m.base.*;
import com.google.iot.m2m.trait.*;
import com.google.iot.m2m.util.NestedPersistentStateManager;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

public class LocalTimerManagerTrait extends AutomationTimerManagerTrait.AbstractLocalTrait implements PersistentStateInterface {
    private static final boolean DEBUG = false;
    private static final Logger LOGGER = Logger.getLogger(LocalTimerManagerTrait.class.getCanonicalName());

    private final ResourceLinkManager mResourceLinkManager;

    Map<String, LocalTimer> mTimerLookup = new HashMap<>();
    Map<FunctionalEndpoint, String> mTimerReverseLookup = new HashMap<>();
    int mNextChildId = 1;
    FunctionalEndpoint mParent;
    NestedPersistentStateManager mNestedPersistentStateManager = new NestedPersistentStateManager();

    public LocalTimerManagerTrait(ResourceLinkManager resourceLinkManager, FunctionalEndpoint parent) {
        mResourceLinkManager = resourceLinkManager;
        mParent = parent;
    }

    private String getNewChildId() {
        String ret;

        do {
            ret = "" + mNextChildId++;
        } while(mTimerLookup.containsKey(ret));

        return ret;
    }

    @Override
    public Set<FunctionalEndpoint> onCopyChildrenSet() {
        return new HashSet<>(mTimerReverseLookup.keySet());
    }

    @Override
    public @Nullable String onGetIdForChild(FunctionalEndpoint child) {
        return mTimerReverseLookup.get(child);
    }

    @Override
    public @Nullable FunctionalEndpoint onGetChild(String childId) {
        return mTimerLookup.get(childId);
    }

    @CanIgnoreReturnValue
    private boolean onDeleteChild(FunctionalEndpoint child) {
        String childId = mTimerReverseLookup.get(child);

        if (childId == null || !mTimerLookup.containsKey(childId)) {
            return false;
        }

        child.setProperty(AutomationTimerTrait.STAT_RUNNING, false);
        child.setProperty(EnabledDisabledTrait.STAT_VALUE, false);

        mTimerReverseLookup.remove(child);
        mTimerLookup.remove(childId);

        mNestedPersistentStateManager.stopManaging(childId);
        mNestedPersistentStateManager.reset(childId);

        didRemoveChild(child);
        return true;
    }

    @CanIgnoreReturnValue
    private synchronized LocalTimer newLocalTimer(String childId) {
        if (mTimerLookup.containsKey(childId)) {
            onDeleteChild(mTimerLookup.get(childId));
        }

        LocalTimer timer = new LocalTimer(mResourceLinkManager) {
            @Override
            public ListenableFuture<Boolean> delete() {
                return Futures.immediateFuture(onDeleteChild(this));
            }

            @Override
            public @Nullable FunctionalEndpoint getParentFunctionalEndpoint() {
                return mParent;
            }

            @Override
            protected boolean getPermanent() {
                return false;
            }
        };

        mTimerLookup.put(childId, timer);
        mTimerReverseLookup.put(timer, childId);
        mNestedPersistentStateManager.startManaging(childId, timer);
        didAddChild(timer);

        return timer;
    }

    @Override
    public FunctionalEndpoint onInvokeCreate(Map<String, Object> args) throws InvalidMethodArgumentsException {
        LocalTimer ret = newLocalTimer(getNewChildId());
        String param = null;
        try {
            if (AutomationTimerManagerTrait.PARAM_AUTO_DELETE.isInMap(args)) {
                param = AutomationTimerManagerTrait.PARAM_AUTO_DELETE.getName();
                ret.setProperty(AutomationTimerTrait.CONF_AUTO_DELETE,
                        AutomationTimerManagerTrait.PARAM_AUTO_DELETE.coerceFromMap(args)).get();
            }

            if (AutomationTimerManagerTrait.PARAM_AUTO_RESET.isInMap(args)) {
                param = AutomationTimerManagerTrait.PARAM_AUTO_RESET.getName();
                ret.setProperty(AutomationTimerTrait.CONF_AUTO_RESET,
                        AutomationTimerManagerTrait.PARAM_AUTO_RESET.coerceFromMap(args)).get();
            }

            if (AutomationTimerManagerTrait.PARAM_ACTIONS.isInMap(args)) {
                param = AutomationTimerManagerTrait.PARAM_ACTIONS.getName();
                ret.setProperty(ActionsTrait.CONF_ACTIONS,
                        AutomationTimerManagerTrait.PARAM_ACTIONS.coerceFromMap(args)).get();

            } else if (AutomationTimerManagerTrait.PARAM_ACTION_PATH.isInMap(args)) {
                Map<String, Object> action = new HashMap<>();
                param = AutomationTimerManagerTrait.PARAM_ACTION_PATH.getName();

                ActionsTrait.PARAM_ACTION_PATH.putInMap(action,
                        AutomationTimerManagerTrait.PARAM_ACTION_PATH.coerceFromMap(args));

                if (AutomationTimerManagerTrait.PARAM_ACTION_METH.isInMap(args)) {
                    param = AutomationTimerManagerTrait.PARAM_ACTION_PATH.getName();
                    ActionsTrait.PARAM_ACTION_METH.putInMap(action,
                            AutomationTimerManagerTrait.PARAM_ACTION_METH.getFromMap(args));
                }

                if (AutomationTimerManagerTrait.PARAM_ACTION_BODY.isInMap(args)) {
                    param = AutomationTimerManagerTrait.PARAM_ACTION_BODY.getName();
                    ActionsTrait.PARAM_ACTION_BODY.putInMap(action,
                            AutomationTimerManagerTrait.PARAM_ACTION_BODY.getFromMap(args));
                }

                param = AutomationTimerManagerTrait.PARAM_ACTION_PATH.getName();

                @SuppressWarnings("unchecked")
                Map<String, Object>[] actionArray = new Map[]{action};

                ret.setProperty(ActionsTrait.CONF_ACTIONS, actionArray).get();
            }

            if (AutomationTimerManagerTrait.PARAM_SCHEDULE_PROGRAM.isInMap(args)) {
                param = AutomationTimerManagerTrait.PARAM_SCHEDULE_PROGRAM.getName();
                ret.setProperty(AutomationTimerTrait.CONF_SCHEDULE_PROGRAM,
                        AutomationTimerManagerTrait.PARAM_SCHEDULE_PROGRAM.coerceFromMap(args)).get();
            }

            if (AutomationTimerManagerTrait.PARAM_DURATION.isInMap(args)) {
                param = AutomationTimerManagerTrait.PARAM_DURATION.getName();
                ret.setProperty(AutomationTimerTrait.CONF_SCHEDULE_PROGRAM,
                        "" + AutomationTimerManagerTrait.PARAM_DURATION.coerceFromMap(args)).get();
            }

            if (AutomationTimerManagerTrait.PARAM_NAME.isInMap(args)) {
                param = AutomationTimerManagerTrait.PARAM_NAME.getName();
                ret.setProperty(BaseTrait.META_NAME,
                        AutomationTimerManagerTrait.PARAM_NAME.getFromMap(args)).get();
            }

            if (AutomationTimerManagerTrait.PARAM_PREDICATE_PROGRAM.isInMap(args)) {
                param = AutomationTimerManagerTrait.PARAM_PREDICATE_PROGRAM.getName();
                ret.setProperty(AutomationTimerTrait.CONF_PREDICATE_PROGRAM,
                        AutomationTimerManagerTrait.PARAM_PREDICATE_PROGRAM.coerceFromMap(args)).get();
            }

            if (AutomationTimerManagerTrait.PARAM_ENABLED.isInMap(args)) {
                param = AutomationTimerManagerTrait.PARAM_ENABLED.getName();
                ret.setProperty(EnabledDisabledTrait.STAT_VALUE,
                        AutomationTimerManagerTrait.PARAM_ENABLED.getFromMap(args)).get();
            }

        } catch (InterruptedException e) {
            onDeleteChild(ret);
            Thread.currentThread().interrupt();
            throw new TechnologyRuntimeException(e);

        } catch (InvalidValueException e) {
            if (DEBUG) LOGGER.info("InvalidValueException: " + e);
            onDeleteChild(ret);
            throw new InvalidMethodArgumentsException("Bad value for \"" + param + "\" " + e);

        } catch (ExecutionException e) {
            if (DEBUG) LOGGER.info("ExecutionException: " + e);
            onDeleteChild(ret);
            throw new InvalidMethodArgumentsException("Bad value for \"" + param + "\" " + e.getCause().getMessage(), e.getCause());
        }

        return ret;
    }

    @Override
    public Map<String, Object> copyPersistentState() {
        return mNestedPersistentStateManager.copyPersistentState();
    }

    @Override
    public void initWithPersistentState(@Nullable Map<String, Object> persistentState) {
        if (persistentState == null) {
            return;
        }

        mNestedPersistentStateManager.initWithPersistentState(persistentState);

        for (Map.Entry<String, Object> entry : persistentState.entrySet()) {
            Object entryValue = entry.getValue();

            if (!(entryValue instanceof Map)) {
                continue;
            }

            newLocalTimer(entry.getKey());
        }
    }

    @Override
    public void setPersistentStateListener(@Nullable PersistentStateListener listener) {
        mNestedPersistentStateManager.setPersistentStateListener(listener);
    }
}
