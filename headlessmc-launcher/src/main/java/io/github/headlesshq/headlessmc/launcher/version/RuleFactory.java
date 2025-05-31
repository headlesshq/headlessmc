package io.github.headlesshq.headlessmc.launcher.version;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.headlesshq.headlessmc.launcher.util.JsonUtil;
import io.github.headlesshq.headlessmc.os.OS;
import lombok.val;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

class RuleFactory {
    public Rule parse(JsonElement jsonElement) {
        if (jsonElement == null || !jsonElement.isJsonArray()) {
            return Rule.ALLOW;
        }

        val jsonArray = jsonElement.getAsJsonArray();
        val rules = new ArrayList<Rule>(jsonArray.size() + 1);
        rules.add(Rule.DISALLOW); // libraries seem to be disallowed by default
        for (val ruleElement : jsonArray) {
            if (!ruleElement.isJsonObject()) {
                continue;
            }

            val ruleJo = ruleElement.getAsJsonObject();
            rules.add(parse(ruleJo));
        }

        return ofRules(rules);
    }

    private Rule parse(JsonObject ruleJo) {
        val a = ruleJo.get("action");
        if (a == null) {
            return Rule.UNDECIDED;
        }

        val action = Rule.Action.valueOf(a.getAsString().toUpperCase(Locale.ENGLISH));
        val os = ruleJo.get("os");
        val feat = ruleJo.get("features");
        val rules = new ArrayList<Rule>(os != null && feat != null ? 3 : 2);
        if (os != null) {
            rules.add(parseOs(os.getAsJsonObject(), action));
        }

        if (feat != null) {
            val m = JsonUtil.toBoolMap(feat.getAsJsonObject());
            rules.add(ofFeature(m, action));
        }

        if (rules.isEmpty()) {
            rules.add((operatingSystem, feature) -> action);
        }

        return ofRules(rules);
    }

    private Rule parseOs(JsonObject os, Rule.Action action) {
        val osType = os.getAsJsonObject().get("name");
        val type = osType == null ? null : osType.getAsString();

        Pattern version = null;
        val versionObject = os.getAsJsonObject().get("version");
        if (versionObject != null) {
            version = Pattern.compile(versionObject.getAsString());
        }

        // val arch = os.getAsJsonObject().get("arch"); TODO: this?
        return ofOs(type, version, action);
    }

    private Rule ofOs(String type, Pattern version, Rule.Action action) {
        val osType = type == null ? null : OS.Type.valueOf(type.toUpperCase(Locale.ENGLISH));
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
            for (val rule : rules) {
                val action = rule.apply(os, features);
                result = action == Rule.Action.UNDECIDED ? result : action;
            }

            return result;
        };
    }

}
