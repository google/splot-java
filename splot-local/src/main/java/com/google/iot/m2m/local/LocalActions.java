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
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.iot.m2m.base.*;
import com.google.iot.m2m.trait.ActionsTrait;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.net.URI;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;

public abstract class LocalActions extends LocalFunctionalEndpoint {
    private static final boolean DEBUG = false;
    private static final Logger LOGGER = Logger.getLogger(LocalActions.class.getCanonicalName());

    private long mLastFiredTime = 0;

    @SuppressWarnings("unchecked")
    private static final Map<String, Object>[] EMPTY_MAP_ARRAY = new Map[]{};

    @SuppressWarnings("unchecked")
    private Map<String,Object>[] mActionsInfo = new Map[0];

    class Action {
        ResourceLink<Object> mResourceLink;
        Object mBody;

        Action(ResourceLink<Object> resourceLink, @Nullable Object body) {
            mResourceLink = resourceLink;
            mBody = body;
        }

        @CanIgnoreReturnValue
        ListenableFuture<?> invoke() {
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
            }, getExecutor());

            return future;
        }
    }

    private List<Action> mActions = new ArrayList<>();

    // Technology backing this automation
    private final ResourceLinkManager mResourceLinkManager;

    // Number of times this automation pairing has fired.
    private int mCount = 0;

    public LocalActions(ResourceLinkManager technology) {
        mResourceLinkManager = technology;
        registerTrait(mActionsTrait);
    }

    abstract protected Executor getExecutor();

    protected ResourceLinkManager getResourceLinkManager() {
        return mResourceLinkManager;
    }

    protected int getCount() {
        return mCount;
    }

    protected void resetCount() {
        mCount = 0;
        mActionsTrait.didChangeCount(0);
    }

    protected void invoke() {
        List<Action> actions = new ArrayList<>(mActions);

        for (Action action : actions) {
            action.invoke();
        }

        mCount++;
        mLastFiredTime = System.nanoTime();
        mActionsTrait.didChangeCount(mCount);
        mActionsTrait.didChangeLast(0);
    }

    private ActionsTrait.AbstractLocalTrait mActionsTrait = new ActionsTrait.AbstractLocalTrait() {
        @Override
        public Integer onGetCount() {
            return mCount;
        }

        @Nullable
        @Override
        public Map<String, Object>[] onGetActions() {
            return mActionsInfo;
        }

        @Override
        public void onSetActions(@Nullable Map<String, Object>[] value) throws InvalidPropertyValueException, TechnologyException {
            if (Arrays.equals(mActionsInfo, value)) {
                return;
            }

            List<Action> actions = new ArrayList<>();

            if (value == null) {
                // Clear the list.
                value = EMPTY_MAP_ARRAY;

            } else {
                try {
                    for (Map<String, Object> actionInfo : value) {
                        if (actionInfo == null) {
                            throw new InvalidPropertyValueException("Action can't be null");
                        }

                        // Do we skip this action?
                        if (ActionsTrait.PARAM_ACTION_SKIP.isInMap(actionInfo)) {
                            Boolean skip = ActionsTrait.PARAM_ACTION_SKIP.coerceFromMap(actionInfo);
                            if (skip != null && skip) {
                                continue;
                            }
                        }

                        String method = "POST";
                        Object body = null;
                        URI path = ActionsTrait.PARAM_ACTION_PATH.coerceFromMap(actionInfo);

                        if (path == null) {
                            throw new InvalidPropertyValueException("Action path can't be null");
                        }

                        if (ActionsTrait.PARAM_ACTION_METH.isInMap(actionInfo)) {
                            method = ActionsTrait.PARAM_ACTION_METH.getFromMap(actionInfo);
                        }

                        if (!"POST".equals(method)) {
                            throw new InvalidPropertyValueException("Method \"" + method + "\" not yet supported");
                        }

                        if (ActionsTrait.PARAM_ACTION_BODY.isInMap(actionInfo)) {
                            body = ActionsTrait.PARAM_ACTION_BODY.getFromMap(actionInfo);
                        }

                        ResourceLink<Object> resourceLink = mResourceLinkManager.getResourceLinkForUri(path);

                        if (DEBUG)
                            LOGGER.info("Will " + method + " to " + path + " with body of " + body);
                        actions.add(new Action(resourceLink, body));
                    }
                } catch (InvalidValueException|UnknownResourceException e) {
                    throw new InvalidPropertyValueException(e);
                }
            }

            mActionsInfo = value;
            mActions = actions;

            didChangeActions(mActionsInfo);
        }

        @Override
        @Nullable
        public Integer onGetLast() {
            long last = System.nanoTime() - mLastFiredTime;

            if (mLastFiredTime == 0 || last < 0) {
                return null;
            }

            return (int)TimeUnit.NANOSECONDS.toSeconds(last);
        }
    };

    @Override
    public void initWithPersistentState(@Nullable Map<String, Object> persistentState) {
        // Legacy migration.
        if (persistentState != null && persistentState.containsKey("c/timr/acti")) {
            persistentState.put(ActionsTrait.CONF_ACTIONS.getName(), persistentState.get("c/timr/acti"));
            persistentState.remove("c/timr/acti");
        }

        super.initWithPersistentState(persistentState);
    }
}
