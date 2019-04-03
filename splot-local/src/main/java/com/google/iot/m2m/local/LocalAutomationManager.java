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
import com.google.iot.m2m.trait.AutomationPairingManagerTrait;
import com.google.iot.m2m.trait.BaseTrait;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Map;

public class LocalAutomationManager extends LocalFunctionalEndpoint {
    private final LocalPairingManagerTrait mPairingManagerTrait;
    private final LocalTimerManagerTrait mTimerManagerTrait;
    private final LocalRuleManagerTrait mRuleManagerTrait;

    private final BaseTrait.AbstractLocalTrait mBaseTrait = new BaseTrait.AbstractLocalTrait() {
        @Override
        public String onGetModel() {
            return "Automation Manager";
        }

        @Override
        public String onGetNameDefault() {
            return "Automation Manager";
        }

        @Override
        public String onGetManufacturer() {
            return "Splot for Java";
        }
    };

    public LocalAutomationManager(ResourceLinkManager technology) {
        mPairingManagerTrait = new LocalPairingManagerTrait(technology, this);
        mTimerManagerTrait = new LocalTimerManagerTrait(technology, this);
        mRuleManagerTrait = new LocalRuleManagerTrait(technology, this);

        registerTrait(mBaseTrait);
        registerTrait(mPairingManagerTrait);
        registerTrait(mTimerManagerTrait);
        registerTrait(mRuleManagerTrait);
    }

    @Override
    public Map<String, Object> copyPersistentState() {
        Map<String, Object> ret = super.copyPersistentState();
        ret.put("pairings", mPairingManagerTrait.copyPersistentState());
        ret.put("timers", mTimerManagerTrait.copyPersistentState());
        ret.put("rules", mRuleManagerTrait.copyPersistentState());
        return ret;
    }

    @Override
    public void initWithPersistentState(@Nullable Map<String, Object> persistentState) {
        if (persistentState != null) {
            Object pairingsObject = persistentState.remove("pairings");
            if (pairingsObject instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>)pairingsObject;
                mPairingManagerTrait.initWithPersistentState(map);
            } else {

                mPairingManagerTrait.initWithPersistentState(null);
            }

            Object timersObject = persistentState.remove("timers");
            if (timersObject instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>)timersObject;
                mTimerManagerTrait.initWithPersistentState(map);
            } else {

                mTimerManagerTrait.initWithPersistentState(null);
            }

            Object rulesObject = persistentState.remove("rules");
            if (rulesObject instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>)rulesObject;
                mRuleManagerTrait.initWithPersistentState(map);
            } else {

                mRuleManagerTrait.initWithPersistentState(null);
            }
        }

        super.initWithPersistentState(persistentState);
    }

    @Override
    public void setPersistentStateListener(@Nullable PersistentStateListener listener) {
        mPairingManagerTrait.setPersistentStateListener(listener);
        mTimerManagerTrait.setPersistentStateListener(listener);
        mRuleManagerTrait.setPersistentStateListener(listener);
        super.setPersistentStateListener(listener);
    }
}
