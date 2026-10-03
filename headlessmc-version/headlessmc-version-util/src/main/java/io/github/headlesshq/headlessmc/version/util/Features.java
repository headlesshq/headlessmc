package io.github.headlesshq.headlessmc.version.util;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Data
@Getter(AccessLevel.NONE)
public class Features {
    private final Map<Feature, Boolean> features = new HashMap<>();

    public boolean get(Feature feature) {
        return features.getOrDefault(feature, false);
    }

    public void add(Feature feature, boolean value) {
        features.put(feature, value);
    }

    public void remove(Feature feature) {
        features.remove(feature);
    }

    public static Features fromTemplates(TemplateStrings templates) {
        Features result = new Features();
        for (TemplateString templateString : templates.getTemplates()) {
            Feature feature = templateString.feature();
            if (feature != null) {
                result.add(feature, true);
            }
        }

        return result;
    }

}
