package io.github.headlesshq.headlessmc.version.util;

import io.github.headlesshq.headlessmc.os.CPU;
import io.github.headlesshq.headlessmc.os.OS;
import io.github.headlesshq.headlessmc.version.Version;
import lombok.Data;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RulesTest {
    private static final CPU CPU_X64 = CPU.X64;
    private static final OS LINUX = new OS("Ubuntu", OS.Type.LINUX, "24");
    private static final OS WINDOWS = new OS("Windows", OS.Type.WINDOWS, "11");
    private static final OS OSX = new OS("macOS", OS.Type.MACOS, "14");

    @Test
    public void testOSRule() {
        OSImpl acceptedOS = new OSImpl("linux", null, null, null);
        Version.Rule rule = new RuleImpl(null, acceptedOS, Version.Rule.ALLOW);
        Rules rules = Rules.of(List.of(rule));

        assertTrue(rules.allow(CPU_X64, LINUX, new Features()));
        assertFalse(rules.allow(CPU_X64, WINDOWS, new Features()));
    }

    @Test
    public void nullRulesAllowEverything() {
        assertTrue(Rules.of(null).allow(CPU_X64, LINUX, new Features()));
        assertFalse(Rules.of(null).disallow(CPU_X64, LINUX, new Features()));
    }

    @Test
    public void emptyRulesDisallow() {
        assertFalse(Rules.of(List.of()).allow(CPU_X64, LINUX, new Features()));
    }

    @Test
    public void bareAllowRuleMatchesEverything() {
        Rules rules = Rules.of(List.of(new RuleImpl(null, null, Version.Rule.ALLOW)));
        assertTrue(rules.allow(CPU_X64, LINUX, new Features()));
        assertTrue(rules.allow(CPU_X64, OSX, new Features()));
    }

    @Test
    public void allowEverywhereExceptOsx() {
        // the classic lwjgl natives pattern from mojang version.jsons
        Rules rules = Rules.of(List.of(
            new RuleImpl(null, null, Version.Rule.ALLOW),
            new RuleImpl(null, new OSImpl("osx", null, null, null), Version.Rule.DISALLOW)
        ));

        assertTrue(rules.allow(CPU_X64, LINUX, new Features()));
        assertTrue(rules.allow(CPU_X64, WINDOWS, new Features()));
        assertFalse(rules.allow(CPU_X64, OSX, new Features()));
    }

    @Test
    public void featureRuleOnlyMatchesWithFeature() {
        Rules rules = Rules.of(List.of(
            new RuleImpl(Map.of("is_demo_user", true), null, Version.Rule.ALLOW)
        ));

        assertFalse(rules.allow(CPU_X64, LINUX, new Features()));

        Features features = new Features();
        features.add(Feature.of("is_demo_user"), true);
        assertTrue(rules.allow(CPU_X64, LINUX, features));
    }

    @Test
    public void combinedRuleRequiresOsAndFeatures() {
        Rules rules = Rules.of(List.of(
            new RuleImpl(Map.of("has_custom_resolution", true), new OSImpl("linux", null, null, null),
                Version.Rule.ALLOW)
        ));
        Features features = new Features();
        features.add(Feature.of("has_custom_resolution"), true);

        assertTrue(rules.allow(CPU_X64, LINUX, features));
        assertFalse(rules.allow(CPU_X64, WINDOWS, features));
        assertFalse(rules.allow(CPU_X64, LINUX, new Features()));
    }

    @Test
    public void archRuleMatchesCpu() {
        Rules x86 = Rules.of(List.of(
            new RuleImpl(null, new OSImpl(null, null, "x86", null), Version.Rule.ALLOW)
        ));
        Rules arm = Rules.of(List.of(
            new RuleImpl(null, new OSImpl(null, null, "arm", null), Version.Rule.ALLOW)
        ));

        assertTrue(x86.allow(CPU_X64, LINUX, new Features()));
        assertFalse(arm.allow(CPU_X64, LINUX, new Features()));
        assertTrue(arm.allow(CPU.AARCH64, LINUX, new Features()));
    }

    @Test
    public void versionRegexRuleMatchesOsVersion() {
        Rules rules = Rules.of(List.of(
            new RuleImpl(null, new OSImpl("windows", "^10\\..*", null, null), Version.Rule.ALLOW)
        ));

        assertTrue(rules.allow(CPU_X64, new OS("Windows", OS.Type.WINDOWS, "10.0.17134"), new Features()));
        assertFalse(rules.allow(CPU_X64, new OS("Windows", OS.Type.WINDOWS, "11.0"), new Features()));
    }

    @Test
    public void versionRangeRuleComparesVersions() {
        VersionRangeImpl range = new VersionRangeImpl("10.0", "11.0");
        Rules rules = Rules.of(List.of(
            new RuleImpl(null, new OSImpl(null, null, null, range), Version.Rule.ALLOW)
        ));

        assertTrue(rules.allow(CPU_X64, new OS("Windows", OS.Type.WINDOWS, "10.5"), new Features()));
        assertFalse(rules.allow(CPU_X64, new OS("Windows", OS.Type.WINDOWS, "9.0"), new Features()));
        assertFalse(rules.allow(CPU_X64, new OS("Windows", OS.Type.WINDOWS, "12.0"), new Features()));
    }

    @Test
    public void lastMatchingRuleWins() {
        Rules rules = Rules.of(List.of(
            new RuleImpl(null, null, Version.Rule.DISALLOW),
            new RuleImpl(null, null, Version.Rule.ALLOW)
        ));

        assertTrue(rules.allow(CPU_X64, LINUX, new Features()));
    }

    @Data
    private static final class OSImpl implements Version.Rule.OS {
        private final @Nullable String name;
        private final @Nullable String version;
        private final @Nullable String arch;
        private final @Nullable VersionRange versionRange;
    }

    @Data
    private static final class VersionRangeImpl implements Version.Rule.OS.VersionRange {
        private final @Nullable String min;
        private final @Nullable String max;
    }

    @Data
    private static final class RuleImpl implements Version.Rule {
        private final Map<String, Boolean> features;
        private final Version.Rule.@Nullable OS os;
        private final String action;
    }

}
