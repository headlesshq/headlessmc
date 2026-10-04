package io.github.headlesshq.headlessmc.commands.profile;

import io.github.headlesshq.headlessmc.commands.server.VmArgsUtil;
import io.github.headlesshq.headlessmc.console.ArgSplitter;
import io.github.headlesshq.headlessmc.launcher.profile.LaunchOptions;
import io.github.headlesshq.headlessmc.launcher.profile.Profile;
import io.github.headlesshq.headlessmc.platform.PlatformService;
import io.github.headlesshq.headlessmc.platform.VersionID;
import io.github.headlesshq.headlessmc.version.arg.Side;
import io.github.headlesshq.headlessmc.version.arg.VersionArg;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;

@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class EditFieldHelper {
    private final PlatformService platformService;
    private final ArgSplitter splitter;

    SequencedMap<String, EditCommand.EditField<?>> fields(@Nullable Profile profile) {
        SequencedMap<String, EditCommand.EditField<?>> result = new LinkedHashMap<>();

        result.put("name", new EditCommand.EditField<>(Profile::name, Function.identity(), Function.identity(), Profile::withName));
        result.put(
            "version",
            new EditCommand.EditField<>(
                Profile::version,
                VersionArg::toString,
                string -> {
                    VersionArg arg = VersionArg.parse(splitter.split(string));
                    VersionID.resolve(platformService, arg); // check if it can be resolved
                    return arg;
                },
                (p, v) -> p.withVersion(v).withCurrentVersion(v)
            )
        );
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

        if (profile == null || Side.CLIENT.equals(profile.version().side().orElse(Side.CLIENT))) {
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

    record Args() {

    }

}
