package io.github.headlesshq.headlessmc.version.parser;

import com.google.gson.JsonElement;
import io.github.headlesshq.headlessmc.os.OS;
import io.github.headlesshq.headlessmc.version.Features;
import io.github.headlesshq.headlessmc.version.Rule;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

public class RuleParserTest implements UsesResources {
    @Test
    public void testRule() {
        JsonElement element = getJsonElement("rule_os.json");
        Rule rule = new RuleParser().parse(element);
        OS os = new OS("osx", OS.Type.OSX, "1.0.0", "x86", false);
        Assertions.assertEquals(Rule.Action.DISALLOW, rule.apply(os, Features.EMPTY));
        OS windows = new OS("windows", OS.Type.WINDOWS, "1.0.0", "x86", false);
        Assertions.assertEquals(Rule.Action.ALLOW, rule.apply(windows, Features.EMPTY));
        OS linux = new OS("linux", OS.Type.LINUX, "1.0.0", "x86", false);
        Assertions.assertEquals(Rule.Action.ALLOW, rule.apply(linux, Features.EMPTY));
    }

    @Test
    public void testRuleWithVersion() {
        JsonElement element = getJsonElement("rule_os_version.json");
        Rule rule = new RuleParser().parse(element);
        OS os = new OS("osx", OS.Type.OSX, "1.0.0", "x86", false);
        Assertions.assertEquals(Rule.Action.ALLOW, rule.apply(os, Features.EMPTY));
        os = new OS("osx", OS.Type.OSX, "10.5.0", "x86", false);
        Assertions.assertEquals(Rule.Action.DISALLOW, rule.apply(os, Features.EMPTY));
        OS windows = new OS("windows", OS.Type.WINDOWS, "1.0.0", "x86", false);
        Assertions.assertEquals(Rule.Action.ALLOW, rule.apply(windows, Features.EMPTY));
        OS linux = new OS("linux", OS.Type.LINUX, "1.0.0", "x86", false);
        Assertions.assertEquals(Rule.Action.ALLOW, rule.apply(linux, Features.EMPTY));
    }

    @Test
    public void testRuleWithFeature() {
        JsonElement element = getJsonElement("rule_feature.json");
        Rule rule = new RuleParser().parse(element);
        Features features = Features.EMPTY;
        OS os = new OS("osx", OS.Type.OSX, "1.0.0", "x86", false);
        Assertions.assertEquals(Rule.Action.ALLOW, rule.apply(os, features));

        Map<String, Boolean> featureMap = new HashMap<>();
        featureMap.put("feature", true);
        features = new Features(featureMap);
        Assertions.assertEquals(Rule.Action.DISALLOW, rule.apply(os, features));

        featureMap.put("feature", false);
        Assertions.assertEquals(Rule.Action.ALLOW, rule.apply(os, features));
    }

}
