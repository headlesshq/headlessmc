package io.github.headlesshq.headlessmc.launcher.client;

import io.github.headlesshq.headlessmc.launcher.profile.Profile;
import io.github.headlesshq.headlessmc.os.CPU;
import io.github.headlesshq.headlessmc.os.OS;
import io.github.headlesshq.headlessmc.version.ProcessedVersion;
import io.github.headlesshq.headlessmc.version.Version;
import io.github.headlesshq.headlessmc.version.service.VersionJsonService;
import io.github.headlesshq.headlessmc.version.util.Features;
import io.github.headlesshq.headlessmc.version.util.Rules;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A version can declare default jvm arguments, e.g. the memory to launch with.
 * If a profile does not contain them, they are added on every launch.
 * When a profile is added they are written into the profile, so that the user can see and edit them.
 */
@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class DefaultJvmArgsService {
    private final VersionJsonService versionJsonService;
    private final CPU cpu;
    private final OS os;

    /**
     * @param version the version to get the default jvm arguments of.
     * @return the default jvm arguments of the version that apply to this machine.
     */
    public List<String> getDefaultJvmArgs(Version version) {
        ProcessedVersion processed = versionJsonService.process(version);
        List<Version.Argument> args = Optional.ofNullable(processed.getArguments())
            .map(arguments -> arguments.get(Version.DEFAULT_JVM_ARGUMENTS))
            .orElse(List.of());

        List<String> result = new ArrayList<>(args.size());
        for (Version.Argument arg : args) {
            if (Rules.of(arg.getRules()).allow(cpu, os, new Features())) {
                result.addAll(arg.value());
            }
        }

        return result;
    }

    /**
     * Adds the default jvm arguments of a version to the arguments of a profile.
     *
     * @param profile the profile to add the arguments to.
     * @param version the version the profile launches.
     * @return the profile with the default jvm arguments in front of its own,
     * the profile itself if it already contains the default arguments.
     */
    public Profile withDefaultJvmArgs(Profile profile, Version version) {
        if (profile.hasDefaultClientJvmArgs()) {
            return profile;
        }

        List<String> vmArgs = new ArrayList<>(getDefaultJvmArgs(version));
        vmArgs.addAll(profile.vmArgs());
        return profile.withVmArgs(vmArgs).withHasDefaultClientJvmArgs(true);
    }

}
