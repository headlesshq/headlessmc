package io.github.headlesshq.headlessmc.version.util;

import io.github.headlesshq.headlessmc.os.CPU;
import io.github.headlesshq.headlessmc.os.OS;
import io.github.headlesshq.headlessmc.version.Version;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * A class that helps with handling {@link Version.Rule}s.
 */
public record Rules(@Nullable List<Version.Rule> rules) {
    public boolean disallow(CPU cpu, OS os, Features features) {
        return !allow(cpu, os, features);
    }

    public boolean allow(CPU cpu, OS os, Features features) {
        if (rules == null) {
            return true;
        }

        if (rules.isEmpty()) {
            return false;
        }

        Boolean result = null;
        for (Version.Rule rule : rules) {
            if (matchesFeatures(rule, features) && matchesOS(rule, os, cpu)) {
                result = Version.Rule.ALLOW.equalsIgnoreCase(rule.getAction());
            }
        }

        return result != null && result;
    }

    private boolean matchesFeatures(Version.Rule rule, Features features) {
        Map<String, Boolean> ruleFeatures = rule.getFeatures();
        if (ruleFeatures == null) {
            return true;
        }

        for (Map.Entry<String, Boolean> entry : ruleFeatures.entrySet()) {
            if (!entry.getValue().equals(features.get(Feature.of(entry.getKey())))) {
                return false;
            }
        }

        return true;
    }

    private boolean matchesOS(Version.Rule rule, OS os, CPU cpu) {
        Version.Rule.OS ruleOS = rule.getOs();
        if (ruleOS == null) {
            return true;
        }

        String name = ruleOS.getName();
        if (name != null && !name.equalsIgnoreCase(os.type().mcType().name())) {
            return false;
        }

        String version = ruleOS.getVersion();
        if (version != null && !Pattern.compile(version).matcher(os.version()).matches()) {
            return false;
        }

        if (!isInRange(ruleOS.getVersionRange(), os)) {
            return false;
        }

        String arch = ruleOS.getArch();
        return arch == null
            || arch.equalsIgnoreCase(cpu.instructionSet()) // mojang accepts x86_64 as arch: x86 ignoring bitness
            || arch.equalsIgnoreCase(cpu.architecture())
            || cpu.synonyms().stream().anyMatch(alias -> alias.equalsIgnoreCase(arch));
    }

    private boolean isInRange(Version.Rule.OS.@Nullable VersionRange range, OS os) {
        if (range == null) {
            return true;
        }

        String version = os.version();
        String min = range.getMin();
        if (min != null && VersionComparator.INSTANCE.compare(version, min) < 0) {
            return false;
        }

        String max = range.getMax();
        return max == null || VersionComparator.INSTANCE.compare(version, max) <= 0;
    }

    public static Rules of(@Nullable List<Version.Rule> rules) {
        return new Rules(rules);
    }

}
