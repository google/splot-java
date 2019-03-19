package com.google.iot.m2m.local;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;
import com.google.iot.m2m.base.*;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.HashMap;
import java.util.Map;

public class SectionResourceLink extends AbstractResourceLink<Map<String,Map<String,Object>>> implements StateListener, MetadataListener, ConfigListener {
    public static ResourceLink<Map<String,Map<String,Object>>> createForSection(FunctionalEndpoint fe, String section) {
        return new SectionResourceLink(fe, section);
    }

    private enum Section {
        STATE(PropertyKey.SECTION_STATE),
        CONFIG(PropertyKey.SECTION_CONFIG),
        METADATA(PropertyKey.SECTION_METADATA);

        String prefix;

        Section(String x) {
            prefix = PropertyKey.SECTION_CONFIG;
        }
    }

    private final FunctionalEndpoint mFe;
    private final String mSection;

    SectionResourceLink(FunctionalEndpoint fe, String section) {
        mFe = fe;
        mSection = section;
    }

    @Override
    public ListenableFuture<Map<String,Map<String,Object>>> fetchValue() {
        ListenableFuture<Map<String,Object>> future;

        switch (mSection) {
            case PropertyKey.SECTION_STATE:
                future = mFe.fetchState();
                break;

            case PropertyKey.SECTION_CONFIG:
                future = mFe.fetchConfig();
                break;

            case PropertyKey.SECTION_METADATA:
                future = mFe.fetchMetadata();
                break;

            default:
                throw new AssertionError("Invalid section enum");
        }

        ListenableFutureTask<Map<String,Map<String,Object>>> uncollapser =
                ListenableFutureTask.create(
                        ()->uncollapseSectionFromOneLevelMap(future.get(), mSection));

        future.addListener(uncollapser, Runnable::run);

        return uncollapser;
    }

    @Override
    public ListenableFuture<?> invoke(@Nullable Map<String, Map<String,Object>> value) {
        if (value == null) {
            return Futures.immediateFailedFuture(new InvalidPropertyValueException(
                    "Can't set section to null"
            ));
        }

        try {
            return mFe.applyProperties(collapseSectionToOneLevelMap(value, mSection));

        } catch (InvalidValueException e) {
            return Futures.immediateFailedFuture(new InvalidPropertyValueException(e));
        }
    }

    @Override
    protected void onListenerCountChanged(int listeners) {
        if (listeners == 0) {
            switch (mSection) {
                case PropertyKey.SECTION_STATE:
                    mFe.unregisterStateListener(this);
                    break;

                case PropertyKey.SECTION_CONFIG:
                    mFe.unregisterConfigListener(this);
                    break;

                case PropertyKey.SECTION_METADATA:
                    mFe.unregisterMetadataListener(this);
                    break;

                default:
                    throw new AssertionError("Invalid section enum");
            }
        } else if (listeners == 1) {
            switch (mSection) {
                case PropertyKey.SECTION_STATE:
                    mFe.registerStateListener(Runnable::run, this);
                    break;

                case PropertyKey.SECTION_CONFIG:
                    mFe.registerConfigListener(Runnable::run, this);
                    break;

                case PropertyKey.SECTION_METADATA:
                    mFe.registerMetadataListener(Runnable::run, this);
                    break;

                default:
                    throw new AssertionError("Invalid section enum");
            }
        }
    }

    public void onConfigChanged(FunctionalEndpoint fe, Map<String, Object> config) {
        try {
            didChangeValue(uncollapseSectionFromOneLevelMap(config, mSection));
        } catch (InvalidValueException e) {
            throw new AssertionError(e);
        }
    }

    public void onStateChanged(FunctionalEndpoint fe, Map<String, Object> state) {
        try {
            didChangeValue(uncollapseSectionFromOneLevelMap(state, mSection));
        } catch (InvalidValueException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public void onMetadataChanged(FunctionalEndpoint fe, Map<String, Object> metadata) {
        try {
            didChangeValue(uncollapseSectionFromOneLevelMap(metadata, mSection));
        } catch (InvalidValueException e) {
            throw new AssertionError(e);
        }
    }

    static Map<String, Object> collapseSectionToOneLevelMap(Map<String, ?> payload, String section)
            throws InvalidValueException {
        final HashMap<String, Object> converted = new HashMap<>();

        for (Map.Entry<String, ?> entry : payload.entrySet()) {
            String k = entry.getKey();
            Object v = entry.getValue();
            if (!(v instanceof Map)) {
                throw new InvalidValueException("Unexpected type for trait " + k);
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> traitMap = (Map<String, Object>) v;
            traitMap.forEach((k2, v2) -> converted.put(section + "/" + k + "/" + k2, v2));
        }

        return converted;
    }

    static Map<String, Map<String, Object>> uncollapseSectionFromOneLevelMap(
            Map<String, Object> properties, String section) throws InvalidValueException {
        Map<String, Map<String, Object>> ret = new HashMap<>();

        for (Map.Entry<String, ?> entry : properties.entrySet()) {
            String k = entry.getKey();
            Object v = entry.getValue();
            String[] components = k.split("/");

            // Make sure the key is well-formed.
            if (components.length != 3) {
                throw new InvalidValueException("Key \"" + k + "\" is not properly formatted");
            }

            // Make sure the key is in the same section.
            if (!section.equals(components[0])) {
                throw new InvalidValueException("Key \"" + k + "\" is in the wrong section");
            }

            Map<String, Object> traitMap =
                    ret.computeIfAbsent(components[1], k1 -> new HashMap<>());
            traitMap.put(components[2], v);
        }

        return ret;
    }

}
