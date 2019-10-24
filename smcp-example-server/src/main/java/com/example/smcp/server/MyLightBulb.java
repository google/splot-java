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
package com.example.smcp.server;

import com.google.iot.m2m.local.LocalActions;
import com.google.iot.m2m.local.LocalTransitioningThing;
import com.google.iot.m2m.trait.BaseTrait;
import com.google.iot.m2m.trait.LevelTrait;
import com.google.iot.m2m.trait.LightTrait;
import com.google.iot.m2m.trait.OnOffTrait;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

import org.checkerframework.checker.nullness.qual.Nullable;

/** Example Dimmable Light Bulb Thing. */
public class MyLightBulb extends LocalTransitioningThing {
    private static final Logger LOGGER = Logger.getLogger(LocalActions.class.getCanonicalName());
    private boolean mIsOn = false;
    private float mLevel = 0.0f;

    private final BaseTrait.AbstractLocalTrait mBaseTrait =
            new BaseTrait.AbstractLocalTrait() {
                @Override
                public Map<String, String> onGetProductName() {
                    Map<String, String> nameMap = new HashMap<>();
                    nameMap.put(Locale.ENGLISH.toLanguageTag(), "Light Bulb");
                    nameMap.put(Locale.FRENCH.toLanguageTag(), "Ampoule");
                    nameMap.put(Locale.JAPANESE.toLanguageTag(), "電球");
                    return nameMap;
                }

                @Override
                public String onGetManufacturer() {
                    return "Acme, Inc.";
                }

                @Override
                public String onGetModel() {
                    return "LB01";
                }
            };

    private final OnOffTrait.AbstractLocalTrait mOnOffTrait =
            new OnOffTrait.AbstractLocalTrait() {
                @Override
                public Boolean onGetValue() {
                    return mIsOn;
                }

                @Override
                public void onSetValue(@Nullable Boolean value) {
                    if (value != null && mIsOn != value) {
                        mIsOn = value;
                        if (mIsOn) {
                            LOGGER.info("Light " + mBaseTrait.onGetUid() + " is ON");
                        } else {
                            LOGGER.info("Light " + mBaseTrait.onGetUid() + " is OFF");
                        }
                        didChangeValue(mIsOn);
                    }
                }
            };

    private final LevelTrait.AbstractLocalTrait mLevelTrait =
            new LevelTrait.AbstractLocalTrait() {
                @Override
                public Float onGetValue() {
                    return mLevel;
                }

                @Override
                public void onSetValue(@Nullable Float value) {
                    if (value != null && mLevel != value) {
                        mLevel = value;
                        if (mIsOn) {
                            LOGGER.info(String.format("Light %s level is %.1f%%",
                                    mBaseTrait.onGetUid(), mLevel*100.0));
                        } else {
                            LOGGER.info(String.format("Light %s level will be %.1f%%",
                                    mBaseTrait.onGetUid(), mLevel*100.0));
                        }
                        didChangeValue(mLevel);
                    }
                }
            };

    private final LightTrait.AbstractLocalTrait mLightTrait =
            new LightTrait.AbstractLocalTrait() {
                @Override
                public @Nullable Float onGetNativeMireds() {
                    return LightTrait.miredsFromKelvin(2700.0f);
                }

                @Override
                public @Nullable Float onGetMaxLumens() {
                    return 800.0f;
                }
            };

    public MyLightBulb() {
        registerTrait(mBaseTrait);
        registerTrait(mOnOffTrait);
        registerTrait(mLevelTrait);
        registerTrait(mLightTrait);
    }
}
