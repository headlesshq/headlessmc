package io.github.headlesshq.headlessmc.platform.client;

import io.github.headlesshq.headlessmc.files.McFiles;
import io.github.headlesshq.headlessmc.files.McFilesService;
import io.github.headlesshq.headlessmc.platform.FakePlatform;
import io.github.headlesshq.headlessmc.platform.FakePlatformService;
import io.github.headlesshq.headlessmc.platform.FakeVanillaPlatform;
import io.github.headlesshq.headlessmc.platform.FakeVanillaVersionService;
import io.github.headlesshq.headlessmc.platform.FakeVersionService;
import io.github.headlesshq.headlessmc.platform.VanillaInstaller;
import io.github.headlesshq.headlessmc.platform.VanillaVersion;
import io.github.headlesshq.headlessmc.platform.VersionID;
import io.github.headlesshq.headlessmc.platform.server.ServerInstallation;
import io.github.headlesshq.headlessmc.util.typemap.TypedMap;
import io.github.headlesshq.headlessmc.util.typemap.TypedMapImpl;
import io.github.headlesshq.headlessmc.version.FakeVersion;
import io.github.headlesshq.headlessmc.version.Version;
import io.github.headlesshq.headlessmc.version.VersionParser;
import io.github.headlesshq.headlessmc.version.arg.VersionArg;
import io.github.headlesshq.headlessmc.version.service.VersionJsonService;
import io.github.headlesshq.headlessmc.version.service.VersionJsonServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * An installer installs into the {@code .minecraft} directory it is given, and only looks at that directory.
 * There can be multiple directories, and a directory that is not one of the known ones can be installed into too.
 */
class ClientInstallerDirectoryTest {
    private static final String FABRIC = "fabric-loader-0.16.9-1.21.1";

    /** Parses the {@code <id>[:<inheritsFrom>]} format written by {@link #write}. */
    private static final VersionParser PARSER = new VersionParser() {
        @Override
        public Version parse(Reader reader) {
            try (BufferedReader buffered = new BufferedReader(reader)) {
                String[] parts = String.join("", buffered.readAllLines()).trim().split(":");
                FakeVersion version = new FakeVersion(parts[0]);
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

    @TempDir
    Path root;

    private final List<McFiles> vanillaInstalls = new ArrayList<>();
    private final List<McFiles> clientInstalls = new ArrayList<>();

    private McFiles primary;
    private McFiles secondary;
    private FakePlatformService platformService;
    private VersionJsonService versions;
    private VersionMatcherServiceImpl matcherService;
    private AbstractClientInstaller installer;

    private final VanillaInstaller vanillaInstaller = new VanillaInstaller() {
        @Override
        public Version getVersion(VanillaVersion version) {
            return new FakeVersion(version.getName());
        }

        @Override
        public Version installClient(VersionID id, McFiles mcFiles, TypedMap args) {
            vanillaInstalls.add(mcFiles);
            return PARSER.parse(write(mcFiles, id.getVersion().getName(), null));
        }

        @Override
        public ServerInstallation installServer(VersionID id, Path dir, TypedMap args) {
            throw new UnsupportedOperationException();
        }
    };

    @BeforeEach
    void setup() {
        primary = McFiles.of(root.resolve("mc"));
        secondary = McFiles.of(root.resolve("ssd"));

        FakeVanillaVersionService vanillaVersions = new FakeVanillaVersionService("1.21.1", "1.20.4");
        FakeVersionService fabricVersions = new FakeVersionService("fabric").withBuilds("1.21.1", "0.16.9");
        FakePlatform fabric = new FakePlatform("fabric", fabricVersions);
        fabric.withClientSupport(new ClientSupport(
            new DefaultVersionMatcher(vanillaVersions, fabricVersions),
            (id, mcFiles, args) -> new FakeVersion(id.getVersion().getName())
        ));

        platformService = new FakePlatformService(new FakeVanillaPlatform("1.21.1", "1.20.4"), fabric);
        versions = new VersionJsonServiceImpl(PARSER, McFilesService.of(primary, secondary));
        matcherService = new VersionMatcherServiceImpl(platformService);
        installer = new AbstractClientInstaller() {
            @Override
            protected void installClient(VersionID id, McFiles mcFiles, Version vanilla, TypedMap args) {
                clientInstalls.add(mcFiles);
                write(mcFiles, FABRIC, "1.21.1");
            }

            @Override
            protected InstallerServices getServices() {
                return new InstallerServices() {
                    @Override
                    public VersionMatcherService getVersionMatcherService() {
                        return matcherService;
                    }

                    @Override
                    public VersionJsonService getVersionService() {
                        return versions;
                    }

                    @Override
                    public VanillaInstaller getVanillaInstaller() {
                        return vanillaInstaller;
                    }
                };
            }

            @Override
            protected String getPlatformName() {
                return "fabric";
            }
        };
    }

    private static Path write(McFiles mcFiles, String id, String inheritsFrom) {
        try {
            Path dir = Files.createDirectories(mcFiles.getVersionsDir().resolve(id));
            return Files.writeString(dir.resolve(id + ".json"), inheritsFrom == null ? id : id + ":" + inheritsFrom);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private VersionID fabric() {
        return VersionID.resolve(platformService, VersionArg.parse("fabric", "1.21.1"));
    }

    private static Optional<Path> json(McFiles mcFiles) {
        return Optional.of(mcFiles.getVersionsDir().resolve(FABRIC).resolve(FABRIC + ".json").toAbsolutePath().normalize());
    }

    @Test
    void installsIntoThePrimaryDirectory() {
        Version version = installer.installClient(fabric(), primary, new TypedMapImpl());

        assertEquals(json(primary), version.asFile());
        assertEquals(List.of(primary), clientInstalls);
        // the vanilla version it needs goes into the same directory
        assertEquals(List.of(primary), vanillaInstalls);
        assertTrue(Files.exists(primary.getVersionsDir().resolve("1.21.1").resolve("1.21.1.json")));
    }

    @Test
    void installsIntoAnAdditionalDirectory() {
        Version version = installer.installClient(fabric(), secondary, new TypedMapImpl());

        assertEquals(json(secondary), version.asFile());
        assertEquals(List.of(secondary), vanillaInstalls);
        assertFalse(Files.exists(primary.getMcDir()));
    }

    @Test
    void installsIntoADirectoryThatIsNotKnown() {
        // e.g. "install fabric 1.21.1 --dir /custom": the result is found in the directory that has been installed
        // into, and not looked for in the primary directory
        McFiles custom = McFiles.of(root.resolve("custom"));

        Version version = installer.installClient(fabric(), custom, new TypedMapImpl());

        assertEquals(json(custom), version.asFile());
        assertEquals(List.of(custom), clientInstalls);
        assertEquals(List.of(custom), vanillaInstalls);
        assertFalse(Files.exists(primary.getMcDir()));
    }

    @Test
    void aVersionInstalledInAnotherDirectoryDoesNotCount() {
        write(primary, "1.21.1", null);
        write(primary, FABRIC, "1.21.1");

        Version version = installer.installClient(fabric(), secondary, new TypedMapImpl());

        // installed again, into the directory that has been asked for
        assertEquals(List.of(secondary), clientInstalls);
        assertEquals(json(secondary), version.asFile());
        assertTrue(Files.exists(json(primary).orElseThrow()));
    }

    @Test
    void aVersionInstalledInTheSameDirectoryIsReused() {
        write(secondary, "1.21.1", null);
        write(secondary, FABRIC, "1.21.1");

        Version version = installer.installClient(fabric(), secondary, new TypedMapImpl());

        assertEquals(List.of(), clientInstalls);
        assertEquals(List.of(), vanillaInstalls);
        assertEquals(json(secondary), version.asFile());
    }

    @Test
    void aForcedInstallReinstallsInTheGivenDirectory() {
        write(primary, "1.21.1", null);
        write(primary, FABRIC, "1.21.1");
        write(secondary, "1.21.1", null);
        write(secondary, FABRIC, "1.21.1");
        TypedMap args = new TypedMapImpl();
        args.put(ClientInstaller.FORCE_INSTALL, true);

        Version version = installer.installClient(fabric(), secondary, args);

        assertEquals(List.of(secondary), clientInstalls);
        assertEquals(json(secondary), version.asFile());
    }

    @Test
    void theVanillaInstallerOfTheFixturesRecordsTheDirectory() {
        FakeVanillaPlatform vanilla = new FakeVanillaPlatform("1.21.1");
        VersionID id = VersionID.resolve(new FakePlatformService(vanilla), VersionArg.parse("1.21.1"));

        vanilla.getInstaller().installClient(id, secondary, new TypedMapImpl());

        assertEquals(List.of(secondary), vanilla.installedClientDirs());
        assertEquals(List.of(id), vanilla.installedClients());
    }

}
