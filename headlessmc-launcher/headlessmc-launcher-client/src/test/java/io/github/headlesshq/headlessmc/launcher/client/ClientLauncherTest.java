package io.github.headlesshq.headlessmc.launcher.client;

import io.github.headlesshq.headlessmc.auth.Account;
import io.github.headlesshq.headlessmc.auth.LastUsedAccountService;
import io.github.headlesshq.headlessmc.files.McFiles;
import io.github.headlesshq.headlessmc.files.TestFiles;
import io.github.headlesshq.headlessmc.java.launcher.AbstractJavaProcessBuilder;
import io.github.headlesshq.headlessmc.java.launcher.JavaLauncherService;
import io.github.headlesshq.headlessmc.java.launcher.JavaProcess;
import io.github.headlesshq.headlessmc.java.launcher.JavaProcessBuilder;
import io.github.headlesshq.headlessmc.java.launcher.JavaProcessImpl;
import io.github.headlesshq.headlessmc.launcher.LauncherConfig;
import io.github.headlesshq.headlessmc.launcher.args.ArgumentTemplateService;
import io.github.headlesshq.headlessmc.launcher.args.ArgumentsService;
import io.github.headlesshq.headlessmc.launcher.assets.AssetDownloader;
import io.github.headlesshq.headlessmc.launcher.assets.AssetsLocation;
import io.github.headlesshq.headlessmc.launcher.libraries.ClasspathService;
import io.github.headlesshq.headlessmc.launcher.logging.LoggingDownloader;
import io.github.headlesshq.headlessmc.launcher.process.LaunchException;
import io.github.headlesshq.headlessmc.launcher.process.ProcessLauncher;
import io.github.headlesshq.headlessmc.launcher.profile.LaunchOptions;
import io.github.headlesshq.headlessmc.launcher.profile.Profile;
import io.github.headlesshq.headlessmc.os.CPU;
import io.github.headlesshq.headlessmc.os.OS;
import io.github.headlesshq.headlessmc.patcher.PatchContext;
import io.github.headlesshq.headlessmc.patcher.PatchException;
import io.github.headlesshq.headlessmc.patcher.PatchResult;
import io.github.headlesshq.headlessmc.patcher.PatchService;
import io.github.headlesshq.headlessmc.patcher.Patcher;
import io.github.headlesshq.headlessmc.platform.FakePlatform;
import io.github.headlesshq.headlessmc.platform.FakePlatformService;
import io.github.headlesshq.headlessmc.platform.FakeVanillaPlatform;
import io.github.headlesshq.headlessmc.platform.VersionID;
import io.github.headlesshq.headlessmc.platform.client.ClientSupport;
import io.github.headlesshq.headlessmc.platform.client.FakeVersionMatcherService;
import io.github.headlesshq.headlessmc.platform.client.VersionMatcher;
import io.github.headlesshq.headlessmc.util.maven.MavenRepository;
import io.github.headlesshq.headlessmc.version.FakeVersion;
import io.github.headlesshq.headlessmc.version.Version;
import io.github.headlesshq.headlessmc.version.arg.VersionArg;
import io.github.headlesshq.headlessmc.version.service.FakeVersionJsonService;
import io.github.headlesshq.headlessmc.version.util.Feature;
import io.github.headlesshq.headlessmc.version.util.Features;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SequencedMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;

class ClientLauncherTest {
    private static final String MAIN_CLASS = "net.minecraft.client.main.Main";
    private static final Account ACCOUNT = new Account("msa", "Steve", "uuid-steve", "token-123", Account.TYPE_MSA, "xuid");

    @TempDir
    Path root;

    /** Exposes the built command instead of starting a process. */
    private static final class RecordingBuilder extends AbstractJavaProcessBuilder {
        @Override
        public JavaProcess start() {
            return new JavaProcessImpl(
                getId(), Map.of(), List.of(), List.of(), List.of(),
                Optional.ofNullable(getJava()), Optional.empty(), Optional.empty(),
                Optional.ofNullable(getDirectory()), Optional.ofNullable(getJar()), Optional.empty()
            );
        }
    }

    private final JavaLauncherService launcherService = new JavaLauncherService() {
        @Override
        public JavaProcessBuilder buildProcess() {
            return new RecordingBuilder();
        }

        @Override
        public String getName() {
            return "recording";
        }
    };

    /** Records the patchers it was called with and applies the configured result transformation. */
    private static final class RecordingPatchService implements PatchService {
        private final Map<String, Patcher> patchers = new LinkedHashMap<>();
        private final List<List<Patcher>> calls = new ArrayList<>();
        private final List<Integer> javaVersions = new ArrayList<>();
        private java.util.function.UnaryOperator<PatchResult> transformation = result -> result;

        @Override
        public PatchResult patch(PatchResult patchResult, int javaVersion, List<Patcher> patchers) {
            calls.add(patchers);
            javaVersions.add(javaVersion);
            return transformation.apply(patchResult);
        }

        @Override
        public List<Patcher> getPatchers() {
            return List.copyOf(patchers.values());
        }

        @Override
        public Optional<Patcher> getPatcher(String name) {
            return Optional.ofNullable(patchers.get(name));
        }
    }

    private record NamedPatcher(String name) implements Patcher {
        @Override
        public void patch(PatchContext context) {
        }

        @Override
        public long version() {
            return 0;
        }
    }

    private record Argument(List<String> value) implements Version.Argument {
        @Override
        public @Nullable List<Version.Rule> getRules() {
            return null;
        }
    }

    private final class Config implements LauncherConfig {
        @Override
        public boolean crashReportWatcher() {
            return false;
        }

        @Override
        public boolean autoInstall() {
            return autoInstall;
        }

        @Override
        public boolean free() {
            return false;
        }
    }

    private final AssetDownloader assetDownloader = Mockito.mock(AssetDownloader.class);
    private final LoggingDownloader loggingDownloader = Mockito.mock(LoggingDownloader.class);
    private final ClasspathService classpathService = Mockito.mock(ClasspathService.class);
    private final RecordingPatchService patchService = new RecordingPatchService();
    private final FakeVersionJsonService versionJsonService = new FakeVersionJsonService();
    private final FakeVersionMatcherService versionMatcherService = new FakeVersionMatcherService();
    private boolean autoInstall = true;

    private FakeVanillaPlatform vanilla;
    private FakePlatform fabric;
    private FakePlatformService platformService;
    private McFiles mcFiles;
    private Path assetsDir;
    private List<Path> classpath;

    @BeforeEach
    void setup() {
        mcFiles = TestFiles.mcFiles(root);
        assetsDir = root.resolve("assets");
        classpath = List.of(root.resolve("libs/a.jar"), root.resolve("versions/1.21.1.jar"));

        vanilla = new FakeVanillaPlatform("1.21.1", "1.20.4", "23w14a", "23w13a", "1.12.2", "13w16a", "1.5.2");
        fabric = FakePlatform.create("fabric", new String[]{"1.21.1", "0.16.9"})
            .withRepository(MavenRepository.of("https://maven.fabricmc.net/"));
        platformService = new FakePlatformService(vanilla, fabric);

        Mockito.when(assetDownloader.download(any())).thenReturn(new AssetsLocation("17", assetsDir));
        Mockito.when(loggingDownloader.downloadLogging(any(), any())).thenReturn(Optional.empty());
        Mockito.when(classpathService.buildClasspath(any(), any(), any(), anyList())).thenAnswer(_ -> classpath);
    }

    private ClientLauncher launcher() {
        return new ClientLauncher(
            new ArgumentTemplateService(),
            Mockito.mock(LastUsedAccountService.class),
            Mockito.mock(SpecificsPropertiesService.class),
            versionMatcherService,
            versionJsonService,
            new TemplateStringService(mcFiles, CPU.X64),
            loggingDownloader,
            launcherService,
            classpathService,
            new ArgumentsService(CPU.X64, new OS("linux", OS.Type.LINUX, "1")),
            assetDownloader,
            platformService,
            patchService,
            new Config(),
            mcFiles
        );
    }

    private FakeVersion version(String id) {
        return new FakeVersion(id)
            .withType("release")
            .withMainClass(MAIN_CLASS)
            .withJavaVersion(21)
            .withAssetIndex(Mockito.mock(Version.AssetIndex.class))
            .withArguments(Map.of(
                Version.JVM_ARGUMENTS, List.of(new Argument(List.of("-Dfrom.version=yes", "-Xss1M"))),
                Version.GAME_ARGUMENTS, List.of(
                    new Argument(List.of("--username", "${auth_player_name}")),
                    new Argument(List.of("--gameDir", "${game_directory}"))
                )
            ));
    }

    /** Registers the given version as installed and matching {@code <platform>/1.21.1}. */
    private FakeVersion install(FakeVersion version, String platform) {
        return install(version, platform, "1.21.1");
    }

    /** Registers the given version as installed and matching {@code <platform>/<mcVersion>}. */
    private FakeVersion install(FakeVersion version, String platform, String mcVersion) {
        versionJsonService.add(version);
        versionMatcherService.withMatch(version.getId(), VersionID.resolve(platformService, VersionArg.parse(platform, mcVersion)));
        return version;
    }

    private Profile joining(String mcVersion, LaunchOptions.Join.Type type, String target) {
        return profile("vanilla", mcVersion).withOptions(new LaunchOptions(
            Optional.empty(), Optional.empty(), Optional.of(new LaunchOptions.Join(type, target)), false
        ));
    }

    private FakeVersion versionWithGameArgs(String id, String... gameArgs) {
        return version(id).withArguments(Map.of(Version.GAME_ARGUMENTS, List.of(new Argument(List.of(gameArgs)))));
    }

    private Profile profile(String... versionArg) {
        return new Profile("profile", VersionArg.parse(versionArg), root.resolve("game"));
    }

    private static RecordingBuilder builder(ProcessLauncher process) {
        return (RecordingBuilder) process.getJavaProcessBuilder().orElseThrow();
    }

    private static Map<String, @Nullable String> systemProperties(RecordingBuilder builder) {
        Map<String, @Nullable String> result = new LinkedHashMap<>();
        builder.getSystemProperties().forEach(e -> result.put(e.getKey(), e.getValue()));
        return result;
    }

    @Test
    void buildsTheClientProcessFromTheInstalledVersion() {
        install(version("1.21.1"), "vanilla");
        Profile profile = profile("vanilla", "1.21.1");

        ProcessLauncher process = launcher().launcher(profile, ACCOUNT);

        assertEquals("mc-client-1.21.1", process.getId());
        assertEquals(root.resolve("game"), process.getGameDir());

        RecordingBuilder builder = builder(process);
        assertEquals(MAIN_CLASS, builder.getMainClass());
        assertEquals(root.resolve("game"), builder.getDirectory());
        assertEquals(21, builder.getVersion());
        assertEquals(classpath.stream().map(p -> p.toAbsolutePath().toString()).toList(), builder.getClassPath());
        assertEquals(List.of("-Xss1M"), builder.getJvmArgs());
        assertEquals(
            List.of("--username", "Steve", "--gameDir", root.resolve("game").toAbsolutePath().toString()),
            builder.getArgs()
        );
        assertFalse(builder.isClasspathArgProvided());

        Map<String, @Nullable String> properties = systemProperties(builder);
        assertEquals("yes", properties.get("from.version"));
        assertEquals(mcFiles.getLibraryDir().toAbsolutePath().toString(), properties.get("libraryDirectory"));
    }

    @Test
    void classpathTemplateIsAvailableToArguments() {
        FakeVersion version = install(version("1.21.1"), "vanilla");
        version.withArguments(Map.of(
            Version.JVM_ARGUMENTS, List.of(new Argument(List.of("-cp", "${classpath}"))),
            Version.GAME_ARGUMENTS, List.of()
        ));

        RecordingBuilder builder = builder(launcher().launcher(profile("vanilla", "1.21.1"), ACCOUNT));

        String expected = String.join(
            java.io.File.pathSeparator,
            classpath.stream().map(p -> p.toAbsolutePath().toString()).toList()
        );
        assertEquals(List.of("-cp", expected), builder.getJvmArgs());
        assertTrue(builder.isClasspathArgProvided());
    }

    @ParameterizedTest
    @ValueSource(strings = {"-cp", "-classpath", "--class-path", "--class-path=custom.jar"})
    void classpathArgsFromTheProfileAreDetected(String arg) {
        install(version("1.21.1"), "vanilla");
        Profile profile = profile("vanilla", "1.21.1").withVmArgs(List.of(arg));

        assertTrue(builder(launcher().launcher(profile, ACCOUNT)).isClasspathArgProvided());
    }

    @Test
    void similarArgsAreNotMistakenForClasspathArgs() {
        install(version("1.21.1"), "vanilla");
        Profile profile = profile("vanilla", "1.21.1").withVmArgs(List.of("-cpx", "--class-paths"));

        assertFalse(builder(launcher().launcher(profile, ACCOUNT)).isClasspathArgProvided());
    }

    @Test
    void javaVersionOfTheProfileOverridesTheVersionJson() {
        install(version("1.21.1"), "vanilla");
        Profile profile = profile("vanilla", "1.21.1").withJavaVersion(17);

        RecordingBuilder builder = builder(launcher().launcher(profile, ACCOUNT));

        assertEquals(17, builder.getVersion());
        assertEquals(List.of(17), patchService.javaVersions);
    }

    @Test
    void javaVersionIsTakenFromTheVersionJsonByDefault() {
        install(version("1.21.1").withJavaVersion(8), "vanilla");

        launcher().launcher(profile("vanilla", "1.21.1"), ACCOUNT);

        assertEquals(List.of(8), patchService.javaVersions);
    }

    @Test
    void patchersOfTheProfileArePassedToThePatchService() {
        install(version("1.21.1"), "vanilla");
        Patcher lwjgl = new NamedPatcher("lwjgl");
        Patcher log4j = new NamedPatcher("log4j");
        patchService.patchers.put("lwjgl", lwjgl);
        patchService.patchers.put("log4j", log4j);
        Profile profile = profile("vanilla", "1.21.1").withPatchers(List.of("log4j", "lwjgl"));

        launcher().launcher(profile, ACCOUNT);

        assertEquals(List.of(List.of(log4j, lwjgl)), patchService.calls);
    }

    @Test
    void unknownPatchersFail() {
        install(version("1.21.1"), "vanilla");
        Profile profile = profile("vanilla", "1.21.1").withPatchers(List.of("missing"));

        assertThrows(PatchException.class, () -> launcher().launcher(profile, ACCOUNT));
    }

    @Test
    void patchResultsAreAppliedToTheProcess() {
        install(version("1.21.1"), "vanilla");
        Path patched = root.resolve("patched/lwjgl.jar");
        Path agent = root.resolve("agents/agent.jar");
        patchService.transformation = result -> new PatchResult(
            new LinkedHashSet<>(List.of(patched)), new LinkedHashSet<>(), Map.of()
        ).withAgent(agent)
            .withSystemProperty("patched.property", "value")
            .withSystemProperty("from.version", "patcher")
            .withSystemProperty("user.property", "patcher");

        SequencedMap<String, @Nullable String> userProperties = new LinkedHashMap<>();
        userProperties.put("user.property", "user");
        Profile profile = profile("vanilla", "1.21.1").withSystemProperties(userProperties);

        RecordingBuilder builder = builder(launcher().launcher(profile, ACCOUNT));

        assertEquals(List.of(patched.toAbsolutePath().toString()), builder.getClassPath());
        assertEquals(List.of("-Xss1M", "-javaagent:" + agent.toAbsolutePath()), builder.getJvmArgs());
        Map<String, @Nullable String> properties = systemProperties(builder);
        assertEquals("value", properties.get("patched.property"));
        // patchers do not override properties defined by the version or the user
        assertEquals("yes", properties.get("from.version"));
        assertEquals("user", properties.get("user.property"));
    }

    @Test
    void libraryDirectoryCanBeOverriddenByTheUser() {
        install(version("1.21.1"), "vanilla");
        SequencedMap<String, @Nullable String> userProperties = new LinkedHashMap<>();
        userProperties.put("libraryDirectory", "custom");
        Profile profile = profile("vanilla", "1.21.1").withSystemProperties(userProperties);

        RecordingBuilder builder = builder(launcher().launcher(profile, ACCOUNT));

        assertEquals("custom", systemProperties(builder).get("libraryDirectory"));
    }

    @Test
    void profileArgumentsAreAppended() {
        install(version("1.21.1"), "vanilla");
        Profile profile = profile("vanilla", "1.21.1")
            .withVmArgs(List.of("-Xmx4G", "-Dprofile.property=${auth_player_name}"))
            .withGameArgs(List.of("--demo"));

        RecordingBuilder builder = builder(launcher().launcher(profile, ACCOUNT));

        assertEquals(List.of("-Xss1M", "-Xmx4G"), builder.getJvmArgs());
        assertEquals("Steve", systemProperties(builder).get("profile.property"));
        assertEquals("--demo", builder.getArgs().getLast());
    }

    @Test
    void loggingArgumentIsAdded() {
        install(version("1.21.1"), "vanilla");
        Mockito.when(loggingDownloader.downloadLogging(any(), any()))
            .thenReturn(Optional.of("-Dlog4j.configurationFile=logging.xml"));

        RecordingBuilder builder = builder(launcher().launcher(profile("vanilla", "1.21.1"), ACCOUNT));

        assertEquals("logging.xml", systemProperties(builder).get("log4j.configurationFile"));
    }

    @Test
    void assetsAreDownloadedForTheAssetIndexOfTheVersion() {
        FakeVersion version = install(version("1.21.1"), "vanilla");

        launcher().launcher(profile("vanilla", "1.21.1"), ACCOUNT);

        Mockito.verify(assetDownloader).download(version.getAssetIndex());
    }

    @Test
    void classpathIsBuiltWithVanillaAndPlatformRepositories() {
        install(version("fabric-loader-1.21.1"), "fabric");

        launcher().launcher(profile("fabric", "1.21.1"), ACCOUNT);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<MavenRepository>> repositories = ArgumentCaptor.forClass(List.class);
        Mockito.verify(classpathService).buildClasspath(any(), any(), any(), repositories.capture());
        assertEquals(fabric.getRepositories(), repositories.getValue());
    }

    @Test
    void demoOptionEnablesTheDemoFeature() {
        install(version("1.21.1"), "vanilla");
        Profile profile = profile("vanilla", "1.21.1")
            .withOptions(new LaunchOptions(Optional.empty(), Optional.empty(), Optional.empty(), true));

        launcher().launcher(profile, ACCOUNT);

        ArgumentCaptor<Features> features = ArgumentCaptor.forClass(Features.class);
        Mockito.verify(classpathService).buildClasspath(any(), features.capture(), any(), anyList());
        assertTrue(features.getValue().get(Feature.DEMO));
    }

    @Test
    void demoFeatureIsDisabledByDefault() {
        install(version("1.21.1"), "vanilla");

        launcher().launcher(profile("vanilla", "1.21.1"), ACCOUNT);

        ArgumentCaptor<Features> features = ArgumentCaptor.forClass(Features.class);
        Mockito.verify(classpathService).buildClasspath(any(), features.capture(), any(), anyList());
        assertFalse(features.getValue().get(Feature.DEMO));
    }

    @Test
    void serverProfilesCanStillBeLaunched() {
        install(version("1.21.1"), "vanilla");

        ProcessLauncher process = launcher().launcher(profile("server", "vanilla", "1.21.1"), ACCOUNT);

        assertEquals(MAIN_CLASS, builder(process).getMainClass());
    }

    @Test
    void missingVersionsAreInstalledIfAutoInstallIsEnabled() {
        List<VersionID> installed = new ArrayList<>();
        fabric.withClientSupport(new ClientSupport(
            Mockito.mock(VersionMatcher.class),
            (id, mcDir, _) -> {
                assertEquals(mcFiles.getMcDir(), mcDir);
                installed.add(id);
                return version("fabric-loader-1.21.1");
            }
        ));

        ProcessLauncher process = launcher().launcher(profile("fabric", "1.21.1"), ACCOUNT);

        assertEquals(1, installed.size());
        assertEquals("fabric", installed.getFirst().getPlatform().getName());
        assertEquals(MAIN_CLASS, builder(process).getMainClass());
    }

    @Test
    void missingVersionsOfPlatformsWithoutClientSupportFail() {
        LaunchException e = assertThrows(
            LaunchException.class,
            () -> launcher().launcher(profile("fabric", "1.21.1"), ACCOUNT)
        );

        assertTrue(e.getMessage().contains("Fabric does not support clients"), e.getMessage());
    }

    @Test
    void missingVersionsFailIfAutoInstallIsDisabled() {
        autoInstall = false;

        LaunchException e = assertThrows(
            LaunchException.class,
            () -> launcher().launcher(profile("fabric", "1.21.1"), ACCOUNT)
        );

        assertTrue(e.getMessage().contains("Failed to find installed version fabric/1.21.1"), e.getMessage());
        assertTrue(e.getMessage().contains("headlessmc install fabric 1.21.1"), e.getMessage());
    }

    @Test
    void otherInstalledVersionsAreNotUsed() {
        autoInstall = false;
        install(version("1.21.1"), "vanilla");

        assertThrows(LaunchException.class, () -> launcher().launcher(profile("fabric", "1.21.1"), ACCOUNT));
    }

    @ParameterizedTest
    @ValueSource(strings = {"13w16a", "1.12.2", "23w13a"})
    void serverArgumentsAreAddedForVersionsBeforeQuickPlay(String mcVersion) {
        install(versionWithGameArgs(mcVersion, "--username", "${auth_player_name}"), "vanilla", mcVersion);

        RecordingBuilder builder = builder(launcher().launcher(
            joining(mcVersion, LaunchOptions.Join.Type.SERVER, "example.com:25566"), ACCOUNT
        ));

        assertEquals(List.of("--username", "Steve", "--server", "example.com", "--port", "25566"), builder.getArgs());
    }

    @Test
    void serverArgumentsAreAddedForLegacyMinecraftArguments() {
        install(version("1.12.2").withArguments(null).withMinecraftArguments("--username ${auth_player_name}"), "vanilla", "1.12.2");

        RecordingBuilder builder = builder(launcher().launcher(
            joining("1.12.2", LaunchOptions.Join.Type.SERVER, "example.com"), ACCOUNT
        ));

        assertEquals(List.of("--username", "Steve", "--server", "example.com", "--port", "25565"), builder.getArgs());
    }

    @Test
    void serverIsNotJoinedForVersionsBeforeServerArguments() {
        install(version("1.5.2").withArguments(null).withMinecraftArguments("${auth_player_name} ${auth_session}"), "vanilla", "1.5.2");

        RecordingBuilder builder = builder(launcher().launcher(
            joining("1.5.2", LaunchOptions.Join.Type.SERVER, "example.com"), ACCOUNT
        ));

        assertEquals(List.of("Steve", "token-123"), builder.getArgs());
    }

    @ParameterizedTest
    @ValueSource(strings = {"23w14a", "1.21.1"})
    void quickPlayVersionsJoinServersViaTheirVersionJson(String mcVersion) {
        install(versionWithGameArgs(mcVersion, "--quickPlayMultiplayer", "${quickPlayMultiplayer}"), "vanilla", mcVersion);

        RecordingBuilder builder = builder(launcher().launcher(
            joining(mcVersion, LaunchOptions.Join.Type.SERVER, "example.com:25566"), ACCOUNT
        ));

        assertEquals(List.of("--quickPlayMultiplayer", "example.com:25566"), builder.getArgs());
    }

    @Test
    void quickPlayArgumentsOfTheVersionJsonArePreferredForVersionsBeforeQuickPlay() {
        install(versionWithGameArgs("1.12.2", "--quickPlayMultiplayer", "${quickPlayMultiplayer}"), "vanilla", "1.12.2");

        RecordingBuilder builder = builder(launcher().launcher(
            joining("1.12.2", LaunchOptions.Join.Type.SERVER, "example.com"), ACCOUNT
        ));

        assertEquals(List.of("--quickPlayMultiplayer", "example.com"), builder.getArgs());
    }

    @Test
    void serverArgumentsAreNotAddedForQuickPlayVersionsWithoutQuickPlayArguments() {
        install(versionWithGameArgs("1.21.1", "--username", "${auth_player_name}"), "vanilla");

        RecordingBuilder builder = builder(launcher().launcher(
            joining("1.21.1", LaunchOptions.Join.Type.SERVER, "example.com"), ACCOUNT
        ));

        assertEquals(List.of("--username", "Steve"), builder.getArgs());
    }

    @ParameterizedTest
    @EnumSource(value = LaunchOptions.Join.Type.class, names = {"SINGLEPLAYER", "REALMS"})
    void singleplayerAndRealmsAreNotJoinedWithoutQuickPlayArguments(LaunchOptions.Join.Type type) {
        install(versionWithGameArgs("1.12.2", "--username", "${auth_player_name}"), "vanilla", "1.12.2");

        RecordingBuilder builder = builder(launcher().launcher(joining("1.12.2", type, "target"), ACCOUNT));

        assertEquals(List.of("--username", "Steve"), builder.getArgs());
    }

    @Test
    void singleplayerWorldsAreJoinedViaTheVersionJson() {
        install(versionWithGameArgs("1.21.1", "--quickPlaySingleplayer", "${quickPlaySingleplayer}"), "vanilla");

        RecordingBuilder builder = builder(launcher().launcher(
            joining("1.21.1", LaunchOptions.Join.Type.SINGLEPLAYER, "world"), ACCOUNT
        ));

        assertEquals(List.of("--quickPlaySingleplayer", "world"), builder.getArgs());
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "example.com|example.com|25565",
        "example.com:25566|example.com|25566",
        " example.com:25566 |example.com|25566",
        "127.0.0.1:1|127.0.0.1|1",
        "[::1]|::1|25565",
        "[::1]:25566|::1|25566",
        "::1|::1|25565",
        "2001:db8::1|2001:db8::1|25565"
    })
    void serverAddressesAreParsed(String address, String host, int port) {
        assertEquals(new ClientLauncher.ServerAddress(host, port), ClientLauncher.ServerAddress.parse(address));
    }

    @ParameterizedTest
    @ValueSource(strings = {"example.com:", "example.com:port", "example.com:65536", "example.com:-1", "[::1]:x"})
    void invalidServerPortsFail(String address) {
        LaunchException e = assertThrows(LaunchException.class, () -> ClientLauncher.ServerAddress.parse(address));
        assertTrue(e.getMessage().contains(address), e.getMessage());
    }

}
