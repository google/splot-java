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
package com.google.iot.m2m.local;

import com.google.iot.m2m.base.*;
import com.google.iot.m2m.trait.LevelTrait;
import com.google.iot.m2m.trait.OnOffTrait;
import com.google.iot.m2m.trait.SceneTrait;
import com.google.iot.m2m.trait.TransitionTrait;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Abstract class for more easily implementing local {@link FunctionalEndpoint}s that support smooth
 * transitions between property values, as well as scenes.
 *
 * <p>By subclassing this class instead of {@link LocalSceneFunctionalEndpoint}, transition support
 * will be largely implemented for you automatically. Like {@link LocalFunctionalEndpoint},
 * Subclasses of this class should <em>NOT</em> be used to implement non-local FunctionalEndpoints:
 * in those cases a class directly implementing the methods of {@link FunctionalEndpoint} should be
 * used.
 *
 * <p>If transitions don't make sense for your functional endpoint (for example, if it is an input
 * rather than an output), then you should subclass {@link LocalFunctionalEndpoint} (or {@link
 * LocalFunctionalEndpoint} if you still need scenes) instead.
 *
 * @see LocalTransitioningFunctionalEndpoint
 * @see LocalFunctionalEndpoint
 */
public abstract class LocalTransitioningFunctionalEndpoint extends LocalSceneFunctionalEndpoint {
    private static final boolean DEBUG = false;
    private static final Logger LOGGER =
            Logger.getLogger(LocalTransitioningFunctionalEndpoint.class.getCanonicalName());

    /**
     * This is an internal marker property used to communicate to other parts of the class that any
     * transition properties present came from defaults and weren't explicitly specified by the
     * entity that requested the change.
     */
    private static final PropertyKey<Boolean> TRANS_IS_DEFAULT =
            new PropertyKey<>("TRANS_IS_DEFAULT", java.lang.Boolean.class);

    /**
     * The values of the transitioning properties when the transition is at 0%. This property, along
     * with {@link #mTransitionEnd}, is used to determine the incremental property states between 0%
     * and 100%.
     */
    private final Map<String, Object> mTransitionBegin = new HashMap<>();

    /**
     * The values of the transitioning properties when the transition is at 100%. This property,
     * along with {@link #mTransitionBegin}, is used to determine the incremental property states
     * between 0% and 100%. Also, the keys in this map are used as the authoritative list of
     * properties that are being transitioned.
     */
    private final Map<String, Object> mTransitionEnd = new HashMap<>();

    /**
     * The final values of the transitioning properties when the transition has completed. This is
     * different from {@link #mTransitionEnd}, which is used for the calculation of intermediate
     * property values: these properties are only applied once the transition is fully complete.
     */
    private final Map<String, Object> mTransitionFinal = new HashMap<>();

    /**
     * The timestamp marking the start of the transition, in nanoseconds.
     *
     * @see #nanoTime()
     */
    private long mTimestampBegin = 0;

    /**
     * The timestamp marking the end of the transition, in nanoseconds.
     *
     * @see #nanoTime()
     */
    private long mTimestampEnd = 0;

    /** Future for canceling the timer used to update the properties during the transition. */
    private ScheduledFuture<?> mTimer = null;

    /** Indicates if a transition is currently in progress or not. */
    private volatile boolean mTransitionInProgress = false;

    /** We explicitly provide the transition trait. It must not be provided by the subclass. */
    private final TransitionTrait.AbstractLocalTrait mTransitionTrait =
            new TransitionTrait.AbstractLocalTrait() {
                @Override
                public Float onGetDuration() {
                    return getRemainingDuration();
                }

                @Override
                public void onSetDuration(@Nullable Float value) {
                    // Ultimately this method is not used. There are a few edge cases
                    // where it might end up getting called anyway, so we have this
                    // method exist with no body.
                }
            };

    @Override
    protected ScheduledExecutorService getExecutor() {
        return Utils.getDefaultExecutor();
    }

    /**
     * Method that simply returns {@link System#nanoTime()}. Intended to be overridden for
     * unit tests.
     */
    protected long nanoTime() {
        return System.nanoTime();
    }

    protected LocalTransitioningFunctionalEndpoint() {
        registerTrait(mTransitionTrait);
    }

    private Boolean isTransitionInProgress() {
        return mTransitionInProgress;
    }

    @Override
    public synchronized Map<String, Object> copyPersistentState() {
        Map<String, Object> ret = super.copyPersistentState();

        if (mTransitionInProgress) {
            ret.putAll(mTransitionFinal);

            TransitionTrait.STAT_DURATION.removeFromMap(ret);
            TransitionTrait.STAT_SPEED.removeFromMap(ret);
            SceneTrait.STAT_SCENE_ID.removeFromMap(ret);
        }

        return ret;
    }

    /** Stops the current transition. */
    public final synchronized void stopTransition() {
        pauseTransition();
        if (mTimer != null) {
            mTimer.cancel(false);
            mTimer = null;
        }
        mTransitionBegin.clear();
        mTransitionEnd.clear();
        mTransitionFinal.clear();
        mTimestampBegin = 0;
        mTimestampEnd = 0;
    }

    /**
     * Pauses the transition in such a way that it may be later resumed by calling {@link
     * #resumeTransition()}. Note that the end timestamp is not updated.
     */
    public final synchronized void pauseTransition() {
        if (mTimer != null) {
            mTimer.cancel(false);
            mTimer = null;
        }
        mTransitionInProgress = false;
    }

    /** Resumes a transition that was previously paused by {@link #pauseTransition()}. */
    public final synchronized void resumeTransition() {
        if (mTimestampEnd != 0 && (mTimer == null || mTimer.isDone())) {
            long period = (mTimestampEnd - mTimestampBegin) / 1000;

            // The minimum duration between transition updates is 50ms.
            period = Math.max(period,
                            TimeUnit.NANOSECONDS.convert(50, TimeUnit.MILLISECONDS));

            // The maximum duration between transition updates is 1s.
            period = Math.min(period,
                            TimeUnit.NANOSECONDS.convert(1, TimeUnit.SECONDS));

            mTimer =
                    getExecutor()
                            .scheduleAtFixedRate(
                                    this::updateCurrentTransitionValues,
                                    period,
                                    period,
                                    TimeUnit.NANOSECONDS);
            mTransitionTrait.didChangeDuration(getRemainingDuration());
            mTransitionInProgress = true;

            // Call this here so that we block execution until we
            // at least get the first set of values dialed in.
            updateCurrentTransitionValues();
        }
        if (DEBUG) {
            LOGGER.info("Transition Begin: " + mTransitionBegin);
            LOGGER.info("Transition End: " + mTransitionEnd);
            LOGGER.info("Transition Remaining: " + getRemainingDuration());
        }
    }

    private synchronized void updateTransition(
            float duration, Map<String, Object> finalState, boolean isDefault)
            throws PropertyException, TechnologyException {
        final Map<String, Object> currentState = copyCachedState();

        if (isDefault && isTransitionInProgress()) {
            pauseTransition();

            mTransitionBegin.putAll(currentState);
            mTransitionBegin.putAll(getCurrentTransitionValues(getPercentDone()));

        } else {
            // This clears mTransitionBegin and mTransitionFinal.
            stopTransition();

            mTransitionBegin.putAll(currentState);

            for (String key : finalState.keySet()) {
                Object val = currentState.get(key);
                if (val != null) {
                    mTransitionFinal.put(key, val);
                }
            }
        }

        mTimestampBegin = nanoTime();
        mTimestampEnd =
                mTimestampBegin
                        + (long) (duration * TimeUnit.NANOSECONDS.convert(1, TimeUnit.SECONDS));

        mTransitionEnd.putAll(finalState);
        mTransitionEnd.remove(TransitionTrait.STAT_DURATION.getName());
        mTransitionEnd.remove(TransitionTrait.STAT_SPEED.getName());
        mTransitionEnd.remove(SceneTrait.STAT_SCENE_ID.getName());

        mTransitionFinal.putAll(mTransitionEnd);

        if (DEBUG) {
            LOGGER.info("updateTransition: finalState = " + finalState);
            LOGGER.info("updateTransition: mTransitionBegin = " + mTransitionBegin);
            LOGGER.info("updateTransition: mTransitionEnd = " + mTransitionEnd);
            LOGGER.info("updateTransition: mTransitionFinal = " + mTransitionFinal);
        }

        // Special case for when we have both the OnOff and Level traits.
        if (OnOffTrait.STAT_VALUE.isInMap(mTransitionBegin)
                && LevelTrait.STAT_VALUE.isInMap(mTransitionBegin)) {
            Boolean begin = OnOffTrait.STAT_VALUE.coerceFromMapNoThrow(mTransitionBegin);
            Boolean end = OnOffTrait.STAT_VALUE.coerceFromMapNoThrow(mTransitionFinal);
            Float level = LevelTrait.STAT_VALUE.coerceFromMapNoThrow(mTransitionFinal);

            if (end == null) {
                end = OnOffTrait.STAT_VALUE.coerceFromMapNoThrow(currentState);
            }

            if (level == null) {
                level = LevelTrait.STAT_VALUE.coerceFromMapNoThrow(currentState);
            }

            if (DEBUG) {
                LOGGER.info("updateTransition: onof/levl begin = " + begin);
                LOGGER.info("updateTransition: onof/levl end = " + end);
                LOGGER.info("updateTransition: onof/levl level = " + level);
            }

            if (Boolean.valueOf(false).equals(begin) && begin.equals(end)) {
                /* We don't perform transitions while we are off, we just do
                 * instantaneous changes.
                 */
                applyPropertiesImmediately(finalState);
                stopTransition();
                mTransitionTrait.didChangeDuration(0.0f);
                return;

            } else if ((level != null) && (begin != null) && (end != null) && !begin.equals(end)) {
                OnOffTrait.STAT_VALUE.putInMap(mTransitionBegin, true);
                OnOffTrait.STAT_VALUE.putInMap(mTransitionEnd, true);
                if (begin && !end) {
                    // Transition from on to off
                    LevelTrait.STAT_VALUE.putInMap(mTransitionEnd, 0.0f);
                    LevelTrait.STAT_VALUE.putInMap(mTransitionFinal, level);

                } else {
                    // Transition from off to on
                    LevelTrait.STAT_VALUE.putInMap(mTransitionBegin, 0.0f);
                    LevelTrait.STAT_VALUE.putInMap(mTransitionEnd, level);
                    LevelTrait.STAT_VALUE.putInMap(mTransitionFinal, level);

                    // Make sure we don't flicker
                    LocalTrait trait = getTraitForPropertyKey(LevelTrait.STAT_VALUE);
                    trait.setValueForPropertyKey(LevelTrait.STAT_VALUE, 0.0f);
                }
            }
        }

        resumeTransition();
        changedPersistentState();
    }

    /**
     * Returns the remaining amount of time (in seconds) that remain in the current transition.
     * Returns zero if there is no transition currently in progress.
     */
    public final float getRemainingDuration() {
        final float remaining =
                (float)
                        ((mTimestampEnd - nanoTime())
                                / (double) TimeUnit.NANOSECONDS.convert(1, TimeUnit.SECONDS));
        if (remaining < 0) {
            return 0;
        }
        return remaining;
    }

    /**
     * Returns the percent complete (0.0f - 1.0f) for the current transition. Returns 1.0f if there
     * is no transition currently in progress.
     */
    public final synchronized float getPercentDone() {
        final long now = nanoTime();
        float percent = (float) (now - mTimestampBegin) / (float) (mTimestampEnd - mTimestampBegin);
        if (now > mTimestampEnd) {
            percent = 1.0f;
        }
        return percent;
    }

    @Override
    protected synchronized <T> T getPropertyTargetValue(PropertyKey<T> key)
            throws PropertyException, TechnologyException {
        T ret = key.coerceFromMapNoThrow(mTransitionFinal);
        if (ret == null) {
            ret = super.getPropertyTargetValue(key);
        }
        return ret;
    }

    /**
     * Subclasses should override this class to more selectively indicate which state properties
     * should be transitioned.
     *
     * @param key the key for the property in question
     * @return <code>true</code> if the property should be transitioned, <code>false</code>
     *     otherwise.
     */
    protected synchronized boolean shouldTransitionProperty(PropertyKey<?> key) {
        // Strictly, only state properties can be transitioned.
        if (!key.isSectionState()) {
            return false;
        }

        LocalTrait trait = getTraitForPropertyKey(key);

        if (trait == null || !trait.onCanTransitionProperty(key)) {
            return false;
        }

        if (key.equals(TransitionTrait.STAT_DURATION)) {
            return false;

        } else if (key.equals(TransitionTrait.STAT_SPEED)) {
            return false;

        } else if (key.equals(SceneTrait.STAT_SCENE_ID)) {
            return false;

        } else if (key.equals(SceneTrait.STAT_GROUP_ID)) {
            return false;

        } else if (Number.class.isAssignableFrom(key.getType())) {
            return true;

        } else if (Boolean.class.isAssignableFrom(key.getType())) {
            return true;

        } else if (float[].class.isAssignableFrom(key.getType())) {
            return true;

        } else if (double[].class.isAssignableFrom(key.getType())) {
            return true;
        }

        return false;
    }

    /**
     * Subclasses can override this method to change how individual properties are transitioned.
     * This can be used to change the transition curve for specific properties.
     *
     * @param key The name of this property as a string.
     * @param percent the percentage indicating how much the resulting value should be influenced by
     *     <code>begin</code> vs <code>end</code>.
     * @param begin the return value when <code>percent</code> is equal to 0.0
     * @param end the return value when <code>percent</code> is equal to 1.0
     * @return the intermediate value between <code>begin</code> and <code>end</code>.
     */
    protected synchronized Object calculateIntermediateValue(
            String key, float percent, Object begin, Object end) {
        if (end instanceof Float) {
            final float endValue = (Float) end;
            final float beginValue = (Float) begin;
            return beginValue + (endValue - beginValue) * percent;

        } else if (end instanceof Double) {
            final Double endValue = (Double) end;
            final Double beginValue = (Double) begin;
            return beginValue + (endValue - beginValue) * percent;

        } else if (end instanceof Integer) {
            final Integer endValue = (Integer) end;
            final Integer beginValue = (Integer) begin;
            return beginValue + Math.round((endValue - beginValue) * percent);

        } else if (end instanceof Long) {
            final Long endValue = (Long) end;
            final Long beginValue = (Long) begin;
            return beginValue + Math.round((endValue - beginValue) * percent);

        } else if (end instanceof Short) {
            final Short endValue = (Short) end;
            final Short beginValue = (Short) begin;
            return beginValue + Math.round((endValue - beginValue) * percent);

        } else if (end instanceof Boolean) {
            final Boolean endValue = (Boolean) end;
            final Boolean beginValue = (Boolean) begin;
            if (!endValue.equals(beginValue)) {
                return (endValue && !beginValue) || percent < 1.0f;
            }

        } else if (end instanceof float[]) {
            final float[] endValue = (float[]) end;
            final float[] beginValue = (float[]) begin;
            final int n = Math.min(endValue.length, beginValue.length);
            final float[] ret = new float[n];
            for (int i = 0; i < n; i++) {
                ret[i] = beginValue[i] + (endValue[i] - beginValue[i]) * percent;
            }
            return ret;

        } else if (end instanceof double[]) {
            final double[] endValue = (double[]) end;
            final double[] beginValue = (double[]) begin;
            final int n = Math.min(endValue.length, beginValue.length);
            final double[] ret = new double[n];
            for (int i = 0; i < n; i++) {
                ret[i] = beginValue[i] + (endValue[i] - beginValue[i]) * percent;
            }
            return ret;
        }
        return end;
    }

    private synchronized void updateCurrentTransitionValues() {
        final long now = nanoTime();
        float percent = (float) (now - mTimestampBegin) / (float) (mTimestampEnd - mTimestampBegin);

        if (now > mTimestampEnd) {
            percent = 1.0f;
            mTimer.cancel(false);
        }

        try {
            if (now <= mTimestampEnd) {
                applyPropertiesImmediately(getCurrentTransitionValues(percent));
            } else {
                applyPropertiesImmediately(mTransitionFinal);
                stopTransition();
                mTransitionTrait.didChangeDuration(0.0f);
            }
        } catch (PropertyException | TechnologyException x) {
            LOGGER.warning(x.toString());
        }
    }

    private Map<String, Object> getCurrentTransitionValues(float percent) {
        if (percent >= 1.0f) {
            return mTransitionFinal;
        }

        final Map<String, Object> transMap = new HashMap<>();

        for (Map.Entry<String, Object> entry : mTransitionEnd.entrySet()) {
            transMap.put(
                    entry.getKey(),
                    calculateIntermediateValue(
                            entry.getKey(),
                            percent,
                            mTransitionBegin.get(entry.getKey()),
                            entry.getValue()));
        }
        return transMap;
    }

    /**
     * Override this to change the default transition duration.
     *
     * @return The default transition duration (0.4s if unchanged)
     */
    protected float getDefaultTransitionDuration() {
        return 0.4f; // 400 milliseconds
    }

    /** TODO: Consider making this protected. */
    @Override
    Map<String, Object> expandProperties(Map<String, Object> properties)
            throws PropertyException, TechnologyException {
        Map<String, Object> ret = super.expandProperties(properties);

        if (!TransitionTrait.STAT_DURATION.isInMap(properties)) {
            Float duration = TransitionTrait.STAT_DURATION.coerceFromMapNoThrow(ret);

            if (duration == null) {
                final float defaultDuration = getDefaultTransitionDuration();
                TransitionTrait.STAT_DURATION.putInMap(ret, defaultDuration);
                duration = defaultDuration;
            }

            if (duration > 0.0f) {
                TRANS_IS_DEFAULT.putInMap(ret, true);
            }
        }

        return ret;
    }

    /** TODO: Consider making this protected. */
    @Override
    void applyPropertiesHook(Map<String, Object> properties)
            throws PropertyException, TechnologyException {
        final float duration;
        final boolean isDefault = properties.containsKey(TRANS_IS_DEFAULT.getName());

        if (TransitionTrait.STAT_DURATION.isInMap(properties)) {
            try {
                Float durationObj = TransitionTrait.STAT_DURATION.coerceFromMap(properties);
                if (durationObj != null) {
                    duration = durationObj;
                } else {
                    duration = 0.0f;
                }
            } catch (InvalidValueException e) {
                throw new InvalidPropertyValueException(e);
            }

        } else {
            duration = 0.0f;
        }

        if (duration > 0.0f) {
            Map<String, Object> otherProps = new HashMap<>();
            Map<String, Object> transState = new HashMap<>();
            for (Map.Entry<String, Object> entry : properties.entrySet()) {
                PropertyKey<?> key = new PropertyKey<>(entry.getKey(), entry.getValue().getClass());
                if (key.isSectionState() && shouldTransitionProperty(key)) {
                    transState.put(entry.getKey(), entry.getValue());
                } else {
                    otherProps.put(entry.getKey(), entry.getValue());
                }
            }
            updateTransition(duration, transState, isDefault);
            properties = otherProps;

        } else {
            stopTransition();
            mTransitionTrait.didChangeDuration(0.0f);
        }

        TRANS_IS_DEFAULT.removeFromMap(properties);
        TransitionTrait.STAT_DURATION.removeFromMap(properties);

        // This will apply all of the properties immediately if
        // there was no duration or a zero duration, otherwise
        // it will apply all of the properties that aren't state
        // properties.
        super.applyPropertiesHook(properties);
    }
}
