package io.github.headlesshq.headlessmc.version.parser;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.headlesshq.headlessmc.os.OS;
import io.github.headlesshq.headlessmc.version.Rule;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

final class RuleParser {
    public Rule parse(@Nullable JsonElement jsonElement) {
        if (jsonElement == null || !jsonElement.isJsonArray()) {
            return Rule.ALLOW;
        }

        JsonArray jsonArray = jsonElement.getAsJsonArray();
        List<Rule> rules = new ArrayList<>(jsonArray.size() + 1);
        rules.add(Rule.DISALLOW); // libraries seem to be disallowed by default
        for (JsonElement ruleElement : jsonArray) {
            if (!ruleElement.isJsonObject()) {
                // TODO: log
                continue;
            }

            JsonObject ruleJo = ruleElement.getAsJsonObject();
            rules.add(parseRule(ruleJo));
        }

        return new ParsedRule(ofRules(rules), jsonElement.toString());
    }

    private Rule parseRule(JsonObject ruleJo) {
        JsonElement a = ruleJo.get("action");
        if (a == null) {
            return Rule.UNDECIDED;
        }

        Rule.Action action = Rule.Action.valueOf(a.getAsString().toUpperCase(Locale.ENGLISH));
        JsonElement os = ruleJo.get("os");
        JsonElement feat = ruleJo.get("features");
        List<Rule> rules = new ArrayList<Rule>(os != null && feat != null ? 3 : 2);
        if (os != null) {
            rules.add(parseOs(os.getAsJsonObject(), action));
        }

        if (feat != null) {
            Map<String, Boolean> m = JsonUtil.toBoolMap(feat.getAsJsonObject());
            rules.add(ofFeature(m, action));
        }

        if (rules.isEmpty()) {
            rules.add((operatingSystem, feature) -> action);
        }

        return ofRules(rules);
    }

    private Rule parseOs(JsonObject os, Rule.Action action) {
        JsonElement osType = os.getAsJsonObject().get("name");
        String type = osType == null ? null : osType.getAsString();

        Pattern version = null;
        JsonElement versionObject = os.getAsJsonObject().get("version");
        if (versionObject != null) {
            version = Pattern.compile(versionObject.getAsString());
        }

        // val arch = os.getAsJsonObject().get("arch"); TODO: this? !!!
        return ofOs(type, version, action);
    }

    private Rule ofOs(String type, Pattern version, Rule.Action action) {
        OS.Type osType = type == null ? null : OS.Type.valueOf(type.toUpperCase(Locale.ENGLISH));
        return (os, features) -> osType != null && osType != os.getType()
                || version != null && !version.matcher(os.getVersion()).find()
                    ? Rule.Action.UNDECIDED
                    : action;
    }

    private Rule ofFeature(Map<String, Boolean> features, Rule.Action action) {
        return (os, f) ->
                features.entrySet()
                        .stream()
                        .allMatch(e -> f.getFeature(e.getKey()) == e.getValue())
                        ? action : Rule.Action.UNDECIDED;
    }

    private Rule ofRules(List<Rule> rules) {
        return (os, features) -> {
            Rule.Action result = Rule.Action.UNDECIDED;
            for (Rule rule : rules) {
                Rule.Action action = rule.apply(os, features);
                result = action == Rule.Action.UNDECIDED ? result : action;
            }

            return result;
        };
    }

}
