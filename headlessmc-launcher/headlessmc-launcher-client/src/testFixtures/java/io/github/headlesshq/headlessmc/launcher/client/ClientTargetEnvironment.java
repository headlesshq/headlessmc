package io.github.headlesshq.headlessmc.launcher.client;

import io.github.headlesshq.headlessmc.files.DefaultFileService;
import io.github.headlesshq.headlessmc.files.DefaultFileSystemProvider;
import io.github.headlesshq.headlessmc.files.FileService;
import io.github.headlesshq.headlessmc.files.McFiles;
import io.github.headlesshq.headlessmc.files.McFilesService;
import io.github.headlesshq.headlessmc.launcher.LauncherConfig;
import io.github.headlesshq.headlessmc.launcher.profile.FakeVersionLaunchTarget;
import io.github.headlesshq.headlessmc.launcher.profile.GameDirService;
import io.github.headlesshq.headlessmc.launcher.profile.LaunchTargetService;
import io.github.headlesshq.headlessmc.launcher.profile.ProfileService;
import io.github.headlesshq.headlessmc.launcher.profile.TestLaunchTargets;
import io.github.headlesshq.headlessmc.mods.packwiz.PackwizService;
import io.github.headlesshq.headlessmc.os.CPU;
import io.github.headlesshq.headlessmc.os.OS;
import io.github.headlesshq.headlessmc.platform.FakePlatform;
import io.github.headlesshq.headlessmc.platform.FakePlatformService;
import io.github.headlesshq.headlessmc.platform.FakeVanillaPlatform;
import io.github.headlesshq.headlessmc.platform.Platform;
import io.github.headlesshq.headlessmc.platform.PlatformService;
import io.github.headlesshq.headlessmc.platform.PlatformVersion;
import io.github.headlesshq.headlessmc.platform.VanillaPlatform;
import io.github.headlesshq.headlessmc.platform.VersionID;
import io.github.headlesshq.headlessmc.platform.client.ClientInstaller;
import io.github.headlesshq.headlessmc.platform.client.ClientSupport;
import io.github.headlesshq.headlessmc.platform.client.FakeVersionMatcherService;
import io.github.headlesshq.headlessmc.platform.client.VersionMatcher;
import io.github.headlesshq.headlessmc.util.json.JsonService;
import io.github.headlesshq.headlessmc.util.typemap.TypedMap;
import io.github.headlesshq.headlessmc.version.FakeVersion;
import io.github.headlesshq.headlessmc.version.Version;
import io.github.headlesshq.headlessmc.version.VersionParser;
import io.github.headlesshq.headlessmc.version.arg.Side;
import io.github.headlesshq.headlessmc.version.arg.VersionArg;
import io.github.headlesshq.headlessmc.version.service.VersionJsonService;
import io.github.headlesshq.headlessmc.version.service.VersionJsonServiceImpl;
import org.jspecify.annotations.Nullable;
import org.mockito.Mockito;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The launch targets for clients wired up for tests, over two {@code .minecraft} directories in a test directory.
 * Versions are real files, their version.json just contains {@code <id>[:<inheritsFrom>]}.
 * The platforms {@code vanilla} and {@code fabric} can install clients, which writes such files.
 * The {@link ClientLauncher} is a mock, nothing is launched.
 */
public final class ClientTargetEnvironment {
    /** The latest build of fabric for 1.21.1. */
    public static final String LATEST_BUILD = "0.16.10";
    /** An older build of fabric for 1.21.1. */
    public static final String OLD_BUILD = "0.16.9";

    /** Parses the version.jsons of this environment, see {@link ClientTargetEnvironment}. */
    public static final VersionParser PARSER = new VersionParser() {
        @Override
        public Version parse(Reader reader) {
            try (BufferedReader buffered = new BufferedReader(reader)) {
                String[] parts = String.join("", buffered.readAllLines()).trim().split(":");
                FakeVersion version = new FakeVersion(parts[0]).withJavaVersion(21);
                return parts.length > 1 ? version.withInheritsFrom(parts[1]) : version;
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        @Override
        public Version parse(Path jsonFile) {
            return ((FakeVersion) VersionParser.super.parse(jsonFile)).withFile(jsonFile);
        }
    };

    /** A client that has been installed by one of the platforms. */
    public record Install(VersionID id, McFiles mcFiles) {
    }

    /** The platforms can be taken away, like the platform of a plugin that has been removed. */
    private final class Platforms implements PlatformService {
        private FakePlatformService delegate;

        private Platforms(FakePlatformService delegate) {
            this.delegate = delegate;
        }

        @Override
        public List<Platform> getPlatforms() {
            return delegate.getPlatforms();
        }

        @Override
        public List<Platform> getPlatformsWithoutVanilla() {
            return delegate.getPlatformsWithoutVanilla();
        }

        @Override
        public Optional<Platform> getPlatform(String name) {
            return delegate.getPlatform(name);
        }

        @Override
        public VanillaPlatform getVanillaPlatform() {
            return delegate.getVanillaPlatform();
        }
    }

    public final Path root;
    public final McFiles primary;
    public final McFiles secondary;
    public final McFilesService mcFilesService;
    public final FileService fileService = new DefaultFileService(new DefaultFileSystemProvider());
    public final FakeVersionMatcherService matcherService = new FakeVersionMatcherService();
    public final VersionJsonService versionJsonService;
    public final ClientLauncher clientLauncher = Mockito.mock(ClientLauncher.class);
    public final LauncherConfig config = Mockito.mock(LauncherConfig.class);
    public final GameDirService gameDirService = Mockito.mock(GameDirService.class);
    public final List<Install> installs = new ArrayList<>();
    public final PlatformService platformService;
    public final TestLaunchTargets targets;
    public final LaunchTargetService launchTargetService;
    public final JsonService jsonService;
    public final VersionLaunchTarget version;
    public final VersionFileLaunchTarget versionFile;
    public final PackwizLaunchTarget packwiz;

    private final FakeVanillaPlatform vanilla;
    private final Platforms platforms;

    /**
     * @param root           the test directory.
     * @param profileService where profiles are stored.
     * @param jsonService    converts the data of launch targets, {@code null} to use the one of the fixtures.
     * @param mcDirs         the {@code .minecraft} directories, the first one is the primary directory.
     *                       By default {@code <root>/mc} and {@code <root>/ssd}.
     */
    public ClientTargetEnvironment(
        Path root, ProfileService profileService, @Nullable JsonService jsonService, McFiles... mcDirs
    ) {
        this.root = root;
        this.primary = mcDirs.length > 0 ? mcDirs[0] : McFiles.of(root.resolve("mc"));
        this.secondary = mcDirs.length > 1 ? mcDirs[1] : McFiles.of(root.resolve("ssd"));
        this.mcFilesService = McFilesService.of(primary, secondary);
        this.versionJsonService = new VersionJsonServiceImpl(PARSER, mcFilesService);

        ClientInstaller clientInstaller = this::installClient;
        ClientSupport clientSupport = new ClientSupport(Mockito.mock(VersionMatcher.class), clientInstaller);
        vanilla = new FakeVanillaPlatform("1.21.1", "1.20.4").withClientSupport(clientSupport);
        FakePlatform fabric = FakePlatform.create("fabric", new String[]{"1.21.1", LATEST_BUILD, OLD_BUILD})
            .withClientSupport(clientSupport);
        // a platform that only has servers
        FakePlatform paper = FakePlatform.create("paper", new String[]{"1.21.1", "130"});
        platforms = new Platforms(new FakePlatformService(vanilla, fabric, paper));
        platformService = platforms;

        Mockito.when(config.autoInstall()).thenReturn(true);
        Mockito.when(gameDirService.getGameDir(Mockito.any(), Mockito.any())).thenAnswer(invocation ->
            invocation.<McFiles>getArgument(1).getMcDir().resolve(invocation.<VersionArg>getArgument(0).toString("-")));

        targets = new TestLaunchTargets(profileService);
        launchTargetService = targets.service();
        this.jsonService = jsonService == null ? targets.jsonService() : jsonService;
        DefaultJvmArgsService defaultJvmArgs = new DefaultJvmArgsService(
            versionJsonService, CPU.X64, new OS("linux", OS.Type.LINUX, "6.0")
        );

        version = new VersionLaunchTarget(
            matcherService, versionJsonService, platformService, mcFilesService, gameDirService,
            clientLauncher, defaultJvmArgs, config, this.jsonService
        );
        versionFile = new VersionFileLaunchTarget(
            versionJsonService, mcFilesService, gameDirService, clientLauncher, defaultJvmArgs, fileService, this.jsonService
        );
        packwiz = new PackwizLaunchTarget(
            launchTargetService, new PackwizService(platformService), fileService, this.jsonService
        );
        targets.with(
            version, versionFile, packwiz,
            new FakeVersionLaunchTarget(platformService, this.jsonService, Side.SERVER, root),
            targets.profileTarget()
        );
    }

    /**
     * An environment over {@code <root>/mc} and {@code <root>/ssd}.
     *
     * @param root           the test directory.
     * @param profileService where profiles are stored.
     */
    public ClientTargetEnvironment(Path root, ProfileService profileService) {
        this(root, profileService, null);
    }

    /** The fabric platform disappears, as if the plugin providing it had been removed. */
    public void removeFabric() {
        platforms.delegate = new FakePlatformService(vanilla);
    }

    /**
     * @param arg a version, e.g. {@code fabric 1.21.1}.
     * @return the id of the version for the platforms of this environment.
     */
    public VersionID id(String... arg) {
        return VersionID.resolve(platformService, VersionArg.parse(arg));
    }

    /**
     * @param id the id of a version.
     * @return the name the version has when it is installed by the platforms of this environment.
     */
    public static String versionName(VersionID id) {
        if (id.isVanilla()) {
            return id.getVersion().getName();
        }

        String build = id.getBuild().map(PlatformVersion::getName).orElse(LATEST_BUILD);
        return id.getPlatform().getName() + "-loader-" + build + "-" + id.getVersion().getName();
    }

    /**
     * @param mcFiles a {@code .minecraft} directory.
     * @param name    the name of a version.
     * @return the absolute, normalized path the version.json of the version has in the directory.
     */
    public Path json(McFiles mcFiles, String name) {
        return mcFiles.getVersionsDir().resolve(name).resolve(name + ".json").toAbsolutePath().normalize();
    }

    /**
     * Installs a version like the platforms do, without recording it as an installation.
     *
     * @return the version.json of the version.
     */
    public Path install(McFiles mcFiles, String... arg) {
        return write(mcFiles, id(arg));
    }

    /** Writes a version.json no platform knows, e.g. of a version installed by another launcher. */
    public Path installCustom(McFiles mcFiles, String name) {
        return writeJson(mcFiles, name, null);
    }

    private Version installClient(VersionID id, McFiles mcFiles, TypedMap args) {
        installs.add(new Install(id, mcFiles));
        return PARSER.parse(write(mcFiles, id));
    }

    private Path write(McFiles mcFiles, VersionID id) {
        String mcVersion = id.getVersion().getName();
        if (!id.isVanilla()) {
            writeJson(mcFiles, mcVersion, null);
            matcherService.withMatch(mcVersion, id.asVanillaVersion());
        }

        String name = versionName(id);
        // the id of the version that has been installed has a build, also if none has been asked for
        VersionID concrete = id.isVanilla() || id.getBuild().isPresent()
            ? id
            : id(id.getPlatform().getName(), mcVersion, LATEST_BUILD);
        matcherService.withMatch(name, VersionID.resolve(platformService, concrete.asArg().withSide(null)));
        return writeJson(mcFiles, name, id.isVanilla() ? null : mcVersion);
    }

    private Path writeJson(McFiles mcFiles, String name, @Nullable String inheritsFrom) {
        try {
            Path file = json(mcFiles, name);
            Files.createDirectories(file.getParent());
            return Files.writeString(file, inheritsFrom == null ? name : name + ":" + inheritsFrom);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
