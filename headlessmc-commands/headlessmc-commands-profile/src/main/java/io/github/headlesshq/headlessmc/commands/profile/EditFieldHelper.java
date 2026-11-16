package io.github.headlesshq.headlessmc.commands.profile;

import io.github.headlesshq.headlessmc.commands.server.VmArgsUtil;
import io.github.headlesshq.headlessmc.console.ArgSplitter;
import io.github.headlesshq.headlessmc.launcher.profile.LaunchOptions;
import io.github.headlesshq.headlessmc.launcher.profile.LaunchTargetService;
import io.github.headlesshq.headlessmc.launcher.profile.ParsedTarget;
import io.github.headlesshq.headlessmc.launcher.profile.Profile;
import io.github.headlesshq.headlessmc.launcher.profile.TargetArgs;
import io.github.headlesshq.headlessmc.launcher.profile.VersionHandle;
import io.github.headlesshq.headlessmc.version.arg.Side;
import io.github.headlesshq.headlessmc.version.arg.VersionArg;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.SequencedMap;
import java.util.Set;
import java.util.function.Function;

@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class EditFieldHelper {
    private final LaunchTargetService launchTargetService;
    private final ArgSplitter splitter;

    SequencedMap<String, EditCommand.EditField<?>> fields(@Nullable Profile profile) {
        return fields(profile, null);
    }

    /**
     * @param profile the profile to get the fields for, {@code null} for all fields.
     * @param target  the name of the launch target given for the {@code target} field,
     *                {@code null} to detect the launch target from the new value.
     * @return the fields that can be edited for the profile.
     */
    SequencedMap<String, EditCommand.EditField<?>> fields(@Nullable Profile profile, @Nullable String target) {
        SequencedMap<String, EditCommand.EditField<?>> result = new LinkedHashMap<>();

        // renaming is done by the EditCommand, a profile is stored under its name
        result.put("name", new EditCommand.EditField<>(Profile::name, Function.identity(), Function.identity(), Profile::withName));
        // what a profile launches: any arguments a launch target can parse
        result.put(
            "target",
            new EditCommand.EditField<>(
                launchTargetService::getLabel,
                Function.identity(),
                Function.identity(),
                (p, value) -> value.equals(launchTargetService.getLabel(p)) ? p : withTarget(p, target, List.of(splitter.split(value)))
            )
        );

        if (profile == null || isVersionTarget(profile)) {
            // a shorthand for the target of profiles that launch a version
            result.put(
                "version",
                new EditCommand.EditField<>(
                    p -> launchTargetService.describe(p).specified().map(v -> v.withSide(null).toString()).orElse(""),
                    Function.identity(),
                    Function.identity(),
                    (p, value) -> {
                        List<String> args = new ArrayList<>(List.of(splitter.split(value.replace("/", " "))));
                        if (p.isServer() && (args.isEmpty() || !"server".equalsIgnoreCase(args.getFirst()))) {
                            args.addFirst("server");
                        }

                        return withTarget(p, p.target().name(), args);
                    }
                )
            );
        }

        // this only makes the profile point to another directory, moving the files is done by the MoveCommand
        result.put("path", new EditCommand.EditField<>(Profile::path, Path::toString, Path::of, Profile::withPath));

        result.put(
            "vm-args",
            new EditCommand.EditField<>(
                p -> {
                    List<String> list = new ArrayList<>(p.vmArgs());
                    p.systemProperties().forEach((k, v) -> list.add("-D" + k + (v == null ? "" : ("=" + v))));
                    return list;
                },
                vmArgs -> String.join(" ", vmArgs),
                string -> VmArgsUtil.parseArgs(splitter, string),
                (p, args) -> {
                    List<String> vmArgs = new ArrayList<>(args);
                    SequencedMap<String, @Nullable String> properties = VmArgsUtil.removeSystemProperties(vmArgs.iterator());
                    return p.withVmArgs(vmArgs).withSystemProperties(properties);
                }
            )
        );

        result.put(
            "game-args",
            new EditCommand.EditField<>(
                p -> p.gameArgs() == null ? new ArrayList<>() : p.gameArgs(),
                gameArgs -> String.join(" ", gameArgs),
                string -> VmArgsUtil.parseArgs(splitter, string),
                Profile::withGameArgs
            )
        );

        result.put(
            "java",
            new EditCommand.EditField<>(
                p -> p.javaVersion() == null ? "" : String.valueOf(p.javaVersion()),
                Function.identity(),
                Function.identity(),
                (p, value) -> p.withJavaVersion(value.isBlank() ? null : parseJavaVersion(value))
            )
        );

        // options of clients, a profile without a side is not a client
        if (profile == null || profile.isClient()) {
            result.put(
                "resolution", new EditCommand.EditField<>(
                    p -> p.options().resolution().orElse(new LaunchOptions.Resolution(800, 600)),
                    LaunchOptions.Resolution::toString,
                    LaunchOptions.Resolution::parse,
                    (p, r) -> p.withOptions(p.options().withResolution(Optional.of(r)))
                )
            );
            /*noinspection ConstantValue
            result.put(
                "account", new EditCommand.EditField<>(
                    p -> p.account() == null ? "" : p.account(),
                    Function.identity(),
                    Function.identity(),
                    (p, a) -> a == null || a.isBlank() ? p.withAccount(null) : p.withAccount(a)
                )
            );*/

            // TODO: result.put("join", new EditField<>());
        }

        return result;
    }

    /**
     * Makes a profile launch something else. What it has been resolved to is forgotten.
     * The side of a profile never changes, so the new target has to be for the side the profile has.
     */
    private Profile withTarget(Profile profile, @Nullable String target, List<String> args) {
        Set<Side> sides = profile.side().map(Set::of).orElse(Side.BOTH);
        ParsedTarget parsed = launchTargetService.parse(target, new TargetArgs(args, sides)).parsed();
        if (!parsed.side().equals(profile.side())) {
            throw new IllegalArgumentException(
                "Profile " + profile.name() + " is " + describe(profile.side()) + ", but '" + String.join(" ", args)
                    + "' is " + describe(parsed.side()) + ". The side of a profile cannot be changed, add a new profile instead."
            );
        }

        Profile result = profile.withTarget(parsed.handle());
        // fails if the profile now inherits from itself
        launchTargetService.effective(result);
        return result;
    }

    private static boolean isVersionTarget(Profile profile) {
        String name = profile.target().name();
        return VersionHandle.VERSION.equalsIgnoreCase(name) || VersionHandle.SERVER.equalsIgnoreCase(name);
    }

    private static int parseJavaVersion(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("'" + value + "' is not a Java version, e.g. 21", e);
        }
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private static String describe(Optional<Side> side) {
        return side.map(s -> s.isServer() ? "a server" : "a client").orElse("neither a client nor a server");
    }

    record Args() {

    }

}
