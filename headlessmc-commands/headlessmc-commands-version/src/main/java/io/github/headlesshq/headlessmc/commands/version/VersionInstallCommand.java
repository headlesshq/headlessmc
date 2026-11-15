package io.github.headlesshq.headlessmc.commands.version;

import io.github.headlesshq.headlessmc.commands.util.VersionArgCommand;
import io.github.headlesshq.headlessmc.commands.util.VersionCompletions;
import io.github.headlesshq.headlessmc.console.Console;
import io.github.headlesshq.headlessmc.console.LongRunningCommand;
import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.files.AppFiles;
import io.github.headlesshq.headlessmc.files.FileService;
import io.github.headlesshq.headlessmc.files.McFiles;
import io.github.headlesshq.headlessmc.files.McFilesService;
import io.github.headlesshq.headlessmc.launcher.profile.Profile;
import io.github.headlesshq.headlessmc.launcher.profile.ProfileService;
import io.github.headlesshq.headlessmc.launcher.server.ServerService;
import io.github.headlesshq.headlessmc.platform.Platform;
import io.github.headlesshq.headlessmc.platform.PlatformService;
import io.github.headlesshq.headlessmc.platform.VersionID;
import io.github.headlesshq.headlessmc.platform.client.ClientInstaller;
import io.github.headlesshq.headlessmc.platform.client.ClientSupport;
import io.github.headlesshq.headlessmc.platform.server.ServerInstaller;
import io.github.headlesshq.headlessmc.platform.server.ServerInstallation;
import io.github.headlesshq.headlessmc.platform.server.ServerSupport;
import io.github.headlesshq.headlessmc.util.typemap.TypedMap;
import io.github.headlesshq.headlessmc.util.typemap.TypedMapImpl;
import io.github.headlesshq.headlessmc.version.Version;
import io.github.headlesshq.headlessmc.version.arg.Side;
import io.github.headlesshq.headlessmc.version.arg.VersionArg;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.jspecify.annotations.Nullable;
import picocli.CommandLine;

import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Base class for {@link InstallCommand} and {@link InstallShortcutCommand},
 * provides version installation capabilities for client/server.
 */
@Getter
@Setter
@RequiredArgsConstructor
public class VersionInstallCommand implements Runnable, VersionArgCommand, LongRunningCommand {
    private final PlatformService platformService;
    private final ProfileService profileService;
    private final ServerService serverService;
    private final FileService fileService;
    private final AppFiles appFiles;
    private final McFilesService mcFilesService;
    private final Console console;

    @CommandLine.Option(
        names = {"-f", "--force"},
        description = "Forces reinstallation even if the version is already installed.",
        defaultValue = "false"
    )
    private boolean force;

    @CommandLine.Option(
        names = {"-u", "--url"},
        description = "Uses a custom URL to download the installer."
    )
    private @Nullable String customURL;

    @CommandLine.Option(
        names = {"-d", "--dir"},
        description = "Installs the version to a custom directory, for clients a Minecraft directory, for servers the directory of the server."
    )
    private @Nullable String customDir;

    @CommandLine.Option(
        names = {"--mc"},
        description = "The Minecraft directory to install a client into, by default the primary one (hmc.files.mc)."
    )
    private @Nullable String mc;

    @CommandLine.Option(
        names = {"-n", "--name"},
        description = "This name is given to the profile/server to install."
    )
    private @Nullable String name;

    @CommandLine.Parameters(
        paramLabel = "VERSION",
        description = "The version to install, e.g. 'fabric 1.21.1'.",
        completionCandidates = VersionCompletions.class
    )
    private List<String> versionArg = new ArrayList<>();

    @Override
    public void run() {
        install();
    }

    protected VersionArg arg() {
        if (versionArg.isEmpty()) {
            throw new IllegalArgumentException("Please specify the version to download, e.g. 'vanilla 26.2'");
        }

        return VersionArg.parse(versionArg);
    }

    protected void install() {
        VersionArg arg = arg();
        VersionID id = VersionID.resolve(platformService, arg);
        Platform platform = id.getPlatform();
        try {
            if (Side.SERVER.equals(id.getSide().orElse(Side.CLIENT))) {
                ServerInstaller installer = platform.getServerSupport().map(ServerSupport::installer)
                    .orElseThrow(() -> new IllegalArgumentException("Platform " + platform.getName() + " does not support servers."));

                String name = this.name != null ? this.name : serverService.getServerName(arg);
                // TODO: silly restriction? Should we ServerService handle its profiles separately from the profile service?
                //  that way people can have client profile and server profile with same name,
                //  and target the server profile via the server commands
                // also holds for profiles that are neither client nor server, we never overwrite those with a server
                Optional<Profile> existing = profileService.getProfile(name).filter(profile -> !profile.isServer());
                if (existing.isPresent()) {
                    throw new IllegalArgumentException(existing.get().isClient()
                        ? "A client profile with name " + name + " already exists."
                        : "A profile with name " + name + " that is not a server already exists.");
                }

                if (!force && serverService.getServer(name).isPresent()) {
                    throw new IllegalArgumentException("Server " + name + " already exists, use --force to overwrite it.");
                }

                Path dir = customDir != null ? fileService.getUserPath(customDir) : appFiles.getServerDir().resolve(name);
                ServerInstallation installation = installer.installServer(
                    id, dir, new TypedMapImpl()
                );

                Profile server = serverService.add(dir, id, name, installation);
                console.write("Installed server " + server.name() + " for version " + id);
            } else {
                installClient(platform, id);
            }
        } catch (HeadlessMcException e) {
            throw new HeadlessMcException("Failed to install " + id, e);
        }
    }

    protected Version installClient(Platform platform, VersionID id) {
        TypedMap map = new TypedMapImpl();
        map.put(ClientInstaller.FORCE_INSTALL, force);
        if (customURL != null) {
            map.put(ClientInstaller.CUSTOM_INSTALLER_URL, URI.create(customURL));
        }

        McFiles mcFiles = getMcFiles();
        Version version = platform.getClientSupport().map(ClientSupport::installer).orElseThrow(() -> new IllegalArgumentException(
            "Platform " + platform.getName() + " does not support clients."
        )).installClient(id, mcFiles, map);
        console.write("Installed version " + id + " successfully");
        return version;
    }

    /**
     * @return the {@code .minecraft} directory to install a client into: {@code --mc}, else {@code --dir},
     * else the primary directory. It does not have to be one of the known directories.
     */
    protected McFiles getMcFiles() {
        if (mc != null) {
            return mcFilesService.of(fileService.getUserPath(mc));
        }

        if (customDir != null) {
            return mcFilesService.of(fileService.getUserPath(customDir));
        }

        return mcFilesService.getPrimary();
    }

}
