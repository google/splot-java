package com.google.iot.m2m.local;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;
import com.google.iot.m2m.base.*;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class SectionResourceLink extends AbstractResourceLink<Map<String,Map<String,Object>>> implements SectionListener {
    public static ResourceLink<Map<String,Map<String,Object>>> createForSection(FunctionalEndpoint fe, Splot.Section section, URI uri) {
        return new SectionResourceLink(fe, section, uri);
    }

    public static ResourceLink<Map<String,Map<String,Object>>> createForSection(FunctionalEndpoint fe, Splot.Section section) {
        return new SectionResourceLink(fe, section, null);
    }

    private final FunctionalEndpoint mFe;
    private final Splot.Section mSection;
    @Nullable private final URI mUri;

    private SectionResourceLink(FunctionalEndpoint fe, Splot.Section section, @Nullable URI uri) {
        mFe = fe;
        mSection = section;
        mUri = uri;
    }

    public URI getUri() {
        if (mUri == null) {
            throw new TechnologyRuntimeException("No URI");
        }
        return mUri;
    }

    @Override
    public ListenableFuture<Map<String,Map<String,Object>>> fetchValue() {
        ListenableFuture<Map<String,Object>> future;

        future = mFe.fetchSection(mSection);

        ListenableFutureTask<Map<String,Map<String,Object>>> uncollapser =
                ListenableFutureTask.create(
                        ()->uncollapseSectionFromOneLevelMap(future.get(), mSection.name));

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
            return mFe.applyProperties(collapseSectionToOneLevelMap(value, mSection.name));

        } catch (InvalidValueException e) {
            return Futures.immediateFailedFuture(new InvalidPropertyValueException(e));
        }
    }

    @Override
    protected void onListenerCountChanged(int listeners) {
        if (listeners == 0) {
            mFe.unregisterSectionListener(this);
        } else if (listeners == 1) {
            mFe.registerSectionListener(Runnable::run, mSection, this);
        }
    }

    @Override
    public void onSectionChanged(FunctionalEndpoint fe, Map<String, Object> state) {
        try {
            didChangeValue(uncollapseSectionFromOneLevelMap(state, mSection.name));
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
