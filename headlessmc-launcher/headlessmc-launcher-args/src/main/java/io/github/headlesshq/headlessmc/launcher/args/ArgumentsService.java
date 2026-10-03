package io.github.headlesshq.headlessmc.launcher.args;

import io.github.headlesshq.headlessmc.java.args.SystemPropertyUtil;
import io.github.headlesshq.headlessmc.launcher.profile.Profile;
import io.github.headlesshq.headlessmc.os.CPU;
import io.github.headlesshq.headlessmc.os.OS;
import io.github.headlesshq.headlessmc.version.Version;
import io.github.headlesshq.headlessmc.version.util.Features;
import io.github.headlesshq.headlessmc.version.util.Rules;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.VisibleForTesting;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class ArgumentsService {
    private final CPU cpu;
    private final OS os;

    public Arguments process(
        Profile profile,
        Version version,
        Features features,
        @Nullable String loggingArg
    ) {
        Arguments result = process(version, features, !profile.hasDefaultClientJvmArgs());

        if (loggingArg != null) {
            result.addVmArg(loggingArg);
        }

        result.systemProperties().putAll(profile.systemProperties());
        // afaik java only counts the last added argument, e.g. if -Xmx2G and -Xmx4G are both added, -Xmx4G is used
        profile.vmArgs().forEach(result::addVmArg);
        if (profile.gameArgs() != null) {
            // TODO: we need to remove argument pairs --arg value, if profile.gameArgs also defines --arg value2...
            result.gameArgs().addAll(profile.gameArgs());
        }

        return result;
    }

    @VisibleForTesting
    Arguments process(Version version, Features features, boolean processDefaults) {
        Map<String, List<Version.Argument>> args = getArgs(version);
        for (String key : args.keySet()) {
            if (!Version.KNOWN_ARGUMENTS.contains(key)) {
                log.error("Failed to handle unknown argument key: {} with arguments {}", key, args.get(key));
            }
        }

        Arguments result = Arguments.mutable();
        process(args.get(Version.JVM_ARGUMENTS), result, features, true);
        if (processDefaults) {
            process(args.get(Version.DEFAULT_JVM_ARGUMENTS), result, features, true);
        }

        process(args.get(Version.GAME_ARGUMENTS), result, features, false);
        return result;
    }

    private void process(@Nullable List<Version.Argument> args, Arguments result, Features features, boolean jvm) {
        if (args == null) {
            return;
        }

        for (Version.Argument argument : args) {
            Rules rules = Rules.of(argument.getRules());
            if (rules.disallow(cpu, os, features)) {
                continue;
            }

            for (String arg : argument.value()) {
                if (!jvm && SystemPropertyUtil.isSystemProperty(arg)) {
                    log.warn("SystemProperty encountered in game-args: {}", arg);
                }

                if (jvm) {
                    result.addVmArg(arg);
                } else {
                    result.gameArgs().add(arg);
                }
            }
        }
    }

    private Map<String, List<Version.Argument>> getArgs(Version version) {
        String mcArgs = version.getMinecraftArguments();
        Map<String, List<Version.Argument>> args = version.getArguments();
        if (mcArgs == null && args == null) {
            throw new ArgumentException("Failed to find either mcArgs nor arguments entry in " + version.getId());
        } else if (mcArgs != null && args != null) {
            throw new ArgumentException("Found both mcArgs and arguments entry in " + version.getId());
        } else if (args != null) {
            return args;
        } else {
            String[] split = mcArgs.split(" ");
            Map<String, List<Version.Argument>> result = new HashMap<>();
            result.put(Version.GAME_ARGUMENTS, Arrays.stream(split)
                .map(McArgument::new)
                .map(Version.Argument.class::cast)
                .toList());

            return result;
        }
    }

}
