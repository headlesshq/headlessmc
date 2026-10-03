package io.github.headlesshq.headlessmc.commands.profile;

import io.github.headlesshq.headlessmc.commands.version.VersionInstallCommand;
import io.github.headlesshq.headlessmc.console.Console;
import io.github.headlesshq.headlessmc.files.AppFiles;
import io.github.headlesshq.headlessmc.files.FileService;
import io.github.headlesshq.headlessmc.files.McFiles;
import io.github.headlesshq.headlessmc.launcher.profile.Profile;
import io.github.headlesshq.headlessmc.launcher.profile.ProfileService;
import io.github.headlesshq.headlessmc.launcher.server.ServerService;
import io.github.headlesshq.headlessmc.os.OSService;
import io.github.headlesshq.headlessmc.platform.Platform;
import io.github.headlesshq.headlessmc.platform.PlatformService;
import io.github.headlesshq.headlessmc.platform.VersionID;
import io.github.headlesshq.headlessmc.version.Version;
import io.github.headlesshq.headlessmc.version.service.VersionJsonService;
import io.github.headlesshq.headlessmc.version.util.Features;
import io.github.headlesshq.headlessmc.version.util.Rules;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Default;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Getter
@Setter
@Default
@Dependent
@Named("command:profile:add")
@CommandLine.Command(
    name = "add",
    mixinStandardHelpOptions = true,
    description = "Adds a new profile."
)
public class AddCommand extends VersionInstallCommand {
    private final VersionJsonService versionJsonService;
    private final OSService osService;

    @Inject
    public AddCommand(
        PlatformService platformService,
        ProfileService profileService,
        ServerService serverService,
        FileService fileService,
        AppFiles appFiles,
        McFiles mcFiles,
        Console console,
        VersionJsonService versionJsonService,
        OSService osService
    ) {
        super(platformService, profileService, serverService, fileService, appFiles, mcFiles, console);
        this.versionJsonService = versionJsonService;
        this.osService = osService;
    }

    @Override
    protected Version installClient(Platform platform, VersionID id) {
        String name = getName();
        if (name == null) {
            name = id.asArg().toString("-");
        }

        if (!isForce() && getProfileService().getProfile(name).isPresent()) {
            throw new IllegalArgumentException("Profile " + name + " already exists, use --force to overwrite it.");
        }

        Version version = super.installClient(platform, id);
        version = versionJsonService.process(version);

        Profile profile = getProfileService()
            .createDefault(name, id.asArg())
            .withVmArgs(getDefaultJvmArgs(version))
            .withHasDefaultClientJvmArgs(true);

        String customDir = getCustomDir();
        if (customDir != null) {
            profile = profile.withPath(getFileService().getUserPath(customDir));
        }

        getProfileService().save(profile);
        getConsole().write("Added profile " + name + " for version " + id);
        return version;
    }

    @Override
    protected Path getMcDir() {
        return getMcFiles().getMcDir();
    }

    private List<String> getDefaultJvmArgs(Version version) {
        List<Version.Argument> args = Optional.ofNullable(version.getArguments())
            .map(vArgs -> vArgs.get(Version.DEFAULT_JVM_ARGUMENTS))
            .orElse(new ArrayList<>(0));

        if (version.getArguments() == null || args.isEmpty()) {
            // return some default arguments
        }

        List<String> result = new ArrayList<>(args.size());
        for (Version.Argument arg : args) {
            Rules rules = Rules.of(arg.getRules());
            if (rules.allow(osService.getCPU(), osService.getOS(), new Features())) {
                result.addAll(arg.value());
            }
        }

        return result;
    }

}
