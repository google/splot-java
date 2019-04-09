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

/**
 * Manager trait for enabling the in-band creation and management of {@link LocalRule} instances.
 * This trait is implemented by the {@link LocalAutomationManager}, but is publicly defined to
 * allow it to be integrated into custom {@link LocalFunctionalEndpoint} implementations.
 */
public class LocalRuleManagerTrait extends AutomationRuleManagerTrait.AbstractLocalTrait implements PersistentStateInterface {
    private static final boolean DEBUG = false;
    private static final Logger LOGGER = Logger.getLogger(LocalRuleManagerTrait.class.getCanonicalName());

    private final ResourceLinkManager mResourceLinkManager;

    private Map<String, LocalRule> mRuleLookup = new HashMap<>();
    private Map<FunctionalEndpoint, String> mRuleReverseLookup = new HashMap<>();
    private int mNextChildId = 1;
    private FunctionalEndpoint mParent;
    private NestedPersistentStateManager mNestedPersistentStateManager = new NestedPersistentStateManager();

    public LocalRuleManagerTrait(ResourceLinkManager resourceLinkManager, FunctionalEndpoint parent) {
        mResourceLinkManager = resourceLinkManager;
        mParent = parent;
    }

    private String getNewChildId() {
        String ret;

        do {
            ret = "" + mNextChildId++;
        } while(mRuleLookup.containsKey(ret));

        return ret;
    }

    @Override
    public Set<FunctionalEndpoint> onCopyChildrenSet() {
        return new HashSet<>(mRuleReverseLookup.keySet());
    }

    @Override
    public @Nullable String onGetIdForChild(FunctionalEndpoint child) {
        return mRuleReverseLookup.get(child);
    }

    @Override
    public @Nullable FunctionalEndpoint onGetChild(String childId) {
        return mRuleLookup.get(childId);
    }

    @CanIgnoreReturnValue
    private boolean onDeleteChild(FunctionalEndpoint child) {
        String childId = mRuleReverseLookup.get(child);

        if (childId == null || !mRuleLookup.containsKey(childId)) {
            return false;
        }

        child.setProperty(EnabledDisabledTrait.STAT_VALUE, false);

        mRuleReverseLookup.remove(child);
        mRuleLookup.remove(childId);

        mNestedPersistentStateManager.stopManaging(childId);
        mNestedPersistentStateManager.reset(childId);

        didRemoveChild(child);
        return true;
    }

    @CanIgnoreReturnValue
    private synchronized LocalRule newLocalRule(String childId) {
        if (mRuleLookup.containsKey(childId)) {
            onDeleteChild(mRuleLookup.get(childId));
        }

        LocalRule Rule = new LocalRule(mResourceLinkManager) {
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

        mRuleLookup.put(childId, Rule);
        mRuleReverseLookup.put(Rule, childId);
        mNestedPersistentStateManager.startManaging(childId, Rule);
        didAddChild(Rule);

        return Rule;
    }

    @Override
    public FunctionalEndpoint onInvokeCreate(Map<String, Object> args) throws InvalidMethodArgumentsException {
        LocalRule ret = newLocalRule(getNewChildId());
        String param = null;
        try {

            if (AutomationRuleManagerTrait.PARAM_CONDITIONS.isInMap(args)) {
                param = AutomationRuleManagerTrait.PARAM_CONDITIONS.getName();
                ret.setProperty(AutomationRuleTrait.CONF_CONDITIONS,
                        AutomationRuleManagerTrait.PARAM_CONDITIONS.coerceFromMap(args)).get();
            }

            if (AutomationRuleManagerTrait.PARAM_ACTIONS.isInMap(args)) {
                param = AutomationRuleManagerTrait.PARAM_ACTIONS.getName();
                ret.setProperty(ActionsTrait.CONF_ACTIONS,
                        AutomationRuleManagerTrait.PARAM_ACTIONS.coerceFromMap(args)).get();

            } else if (AutomationRuleManagerTrait.PARAM_ACTION_PATH.isInMap(args)) {
                Map<String, Object> action = new HashMap<>();
                param = AutomationRuleManagerTrait.PARAM_ACTION_PATH.getName();

                ActionsTrait.PARAM_ACTION_PATH.putInMap(action,
                        AutomationRuleManagerTrait.PARAM_ACTION_PATH.coerceFromMap(args));

                if (AutomationRuleManagerTrait.PARAM_ACTION_METH.isInMap(args)) {
                    param = AutomationRuleManagerTrait.PARAM_ACTION_PATH.getName();
                    ActionsTrait.PARAM_ACTION_METH.putInMap(action,
                            AutomationRuleManagerTrait.PARAM_ACTION_METH.getFromMap(args));
                }

                if (AutomationRuleManagerTrait.PARAM_ACTION_BODY.isInMap(args)) {
                    param = AutomationRuleManagerTrait.PARAM_ACTION_BODY.getName();
                    ActionsTrait.PARAM_ACTION_BODY.putInMap(action,
                            AutomationRuleManagerTrait.PARAM_ACTION_BODY.getFromMap(args));
                }

                param = AutomationRuleManagerTrait.PARAM_ACTION_PATH.getName();

                @SuppressWarnings("unchecked")
                Map<String, Object>[] actionArray = new Map[]{action};

                ret.setProperty(ActionsTrait.CONF_ACTIONS, actionArray).get();
            }

            if (AutomationRuleManagerTrait.PARAM_NAME.isInMap(args)) {
                param = AutomationRuleManagerTrait.PARAM_NAME.getName();
                ret.setProperty(BaseTrait.META_NAME,
                        AutomationRuleManagerTrait.PARAM_NAME.getFromMap(args)).get();
            }

            if (AutomationRuleManagerTrait.PARAM_MATCH.isInMap(args)) {
                param = AutomationRuleManagerTrait.PARAM_MATCH.getName();
                ret.setProperty(AutomationRuleTrait.CONF_MATCH,
                        AutomationRuleManagerTrait.PARAM_MATCH.getFromMap(args)).get();
            }

            if (AutomationRuleManagerTrait.PARAM_ENABLED.isInMap(args)) {
                param = AutomationRuleManagerTrait.PARAM_ENABLED.getName();
                ret.setProperty(EnabledDisabledTrait.STAT_VALUE,
                        AutomationRuleManagerTrait.PARAM_ENABLED.getFromMap(args)).get();
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

            newLocalRule(entry.getKey());
        }
    }

    @Override
    public void setPersistentStateListener(@Nullable PersistentStateListener listener) {
        mNestedPersistentStateManager.setPersistentStateListener(listener);
    }
}
