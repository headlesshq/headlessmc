package io.github.headlesshq.headlessmc.launcher.args;

import io.github.headlesshq.headlessmc.launcher.profile.Profile;
import io.github.headlesshq.headlessmc.os.CPU;
import io.github.headlesshq.headlessmc.os.OS;
import io.github.headlesshq.headlessmc.version.Version;
import io.github.headlesshq.headlessmc.version.util.Feature;
import io.github.headlesshq.headlessmc.version.arg.VersionArg;
import io.github.headlesshq.headlessmc.version.util.Features;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ArgumentsServiceTest {
    private final ArgumentsService service = new ArgumentsService(CPU.X64, new OS("linux", OS.Type.LINUX, "6.0"));

    private static Version version(
        @Nullable String mcArgs,
        @Nullable Map<String, List<Version.Argument>> arguments
    ) {
        return new FakeVersion(mcArgs, arguments);
    }

    private static Version.Argument argument(@Nullable List<Version.Rule> rules, String... values) {
        return new Version.Argument() {
            @Override
            public @Nullable List<Version.Rule> getRules() {
                return rules;
            }

            @Override
            public List<String> value() {
                return List.of(values);
            }
        };
    }

    private static Version.Rule featureRule(String feature) {
        return new Version.Rule() {
            @Override
            public String getAction() {
                return Version.Rule.ALLOW;
            }

            @Override
            public @Nullable OS getOs() {
                return null;
            }

            @Override
            public @Nullable Map<String, Boolean> getFeatures() {
                return Map.of(feature, true);
            }
        };
    }

    @Test
    public void processesModernArguments() {
        Version version = version(null, Map.of(
            Version.JVM_ARGUMENTS, List.of(argument(null, "-Xmx1G", "-Dprop=value")),
            Version.GAME_ARGUMENTS, List.of(argument(null, "--username", "${auth_player_name}"))
        ));

        Arguments arguments = service.process(version, new Features(), true);

        assertEquals(List.of("-Xmx1G"), arguments.vmArgs());
        assertEquals(Map.of("prop", "value"), arguments.systemProperties());
        assertEquals(List.of("--username", "${auth_player_name}"), arguments.gameArgs());
    }

    @Test
    public void processesLegacyMinecraftArguments() {
        Version version = version("--username ${auth_player_name} --gameDir ${game_directory}", null);

        Arguments arguments = service.process(version, new Features(), true);

        assertEquals(
            List.of("--username", "${auth_player_name}", "--gameDir", "${game_directory}"),
            arguments.gameArgs()
        );
        assertTrue(arguments.vmArgs().isEmpty());
    }

    @Test
    public void throwsWithoutAnyArguments() {
        assertThrows(ArgumentException.class, () -> service.process(version(null, null), new Features(), true));
    }

    @Test
    public void throwsWithBothArgumentStyles() {
        Version version = version("--legacy", Map.of());
        assertThrows(ArgumentException.class, () -> service.process(version, new Features(), true));
    }

    @Test
    public void skipsArgumentsDisallowedByFeatures() {
        Version version = version(null, Map.of(
            Version.GAME_ARGUMENTS, List.of(
                argument(null, "--always"),
                argument(List.of(featureRule("is_demo_user")), "--demo")
            )
        ));

        assertEquals(List.of("--always"), service.process(version, new Features(), true).gameArgs());

        Features features = new Features();
        features.add(Feature.of("is_demo_user"), true);
        assertEquals(List.of("--always", "--demo"), service.process(version, features, true).gameArgs());
    }

    @Test
    public void ignoresUnknownArgumentKeys() {
        Version version = version(null, Map.of(
            "unknown-key", List.of(argument(null, "--ignored")),
            Version.GAME_ARGUMENTS, List.of(argument(null, "--kept"))
        ));

        Arguments arguments = service.process(version, new Features(), true);

        assertEquals(List.of("--kept"), arguments.gameArgs());
        assertTrue(arguments.vmArgs().isEmpty());
    }

    @Test
    public void defaultJvmArgumentsAreOnlyProcessedWhenRequested() {
        Version version = version(null, Map.of(
            Version.JVM_ARGUMENTS, List.of(argument(null, "-Xss1M")),
            Version.DEFAULT_JVM_ARGUMENTS, List.of(argument(null, "-Xmx2G")),
            Version.GAME_ARGUMENTS, List.of(argument(null, "--demo"))
        ));

        assertEquals(List.of("-Xss1M", "-Xmx2G"), service.process(version, new Features(), true).vmArgs());
        assertEquals(List.of("-Xss1M"), service.process(version, new Features(), false).vmArgs());
    }

    @Test
    public void profileArgumentsAreAppendedAfterVersionArguments() {
        Version version = version(null, Map.of(
            Version.JVM_ARGUMENTS, List.of(argument(null, "-Xss1M", "-Dversion=a", "-Doverridden=version")),
            Version.DEFAULT_JVM_ARGUMENTS, List.of(argument(null, "-Xmx2G")),
            Version.GAME_ARGUMENTS, List.of(argument(null, "--username", "${auth_player_name}"))
        ));

        LinkedHashMap<String, @Nullable String> systemProperties = new LinkedHashMap<>();
        systemProperties.put("overridden", "profile");
        systemProperties.put("profile", "b");
        Profile profile = new Profile("test", VersionArg.parse("vanilla", "1.21.1"), Path.of("test"))
            .withSystemProperties(systemProperties)
            .withVmArgs(List.of("-Xmx4G"))
            .withGameArgs(List.of("--demo"));

        Arguments arguments = service.process(profile, version, new Features(), "-Dlog4j.configurationFile=log.xml");

        assertEquals(List.of("-Xss1M", "-Xmx2G", "-Xmx4G"), arguments.vmArgs());
        assertEquals(Map.of(
            "version", "a",
            "overridden", "profile",
            "profile", "b",
            "log4j.configurationFile", "log.xml"
        ), arguments.systemProperties());
        assertEquals(List.of("--username", "${auth_player_name}", "--demo"), arguments.gameArgs());
    }

    @Test
    public void profileWithDefaultClientJvmArgsSkipsVersionDefaults() {
        Version version = version(null, Map.of(
            Version.DEFAULT_JVM_ARGUMENTS, List.of(argument(null, "-Xmx2G")),
            Version.GAME_ARGUMENTS, List.of(argument(null, "--demo"))
        ));

        Profile profile = new Profile("test", VersionArg.parse("vanilla", "1.21.1"), Path.of("test"))
            .withHasDefaultClientJvmArgs(true)
            .withGameArgs(null);

        Arguments arguments = service.process(profile, version, new Features(), null);

        assertTrue(arguments.vmArgs().isEmpty());
        assertTrue(arguments.systemProperties().isEmpty());
        assertEquals(List.of("--demo"), arguments.gameArgs());
    }

    private record FakeVersion(
        @Nullable String minecraftArguments,
        @Nullable Map<String, List<Argument>> argumentsMap
    ) implements Version {
        @Override
        public String getId() {
            return "test-version";
        }

        @Override
        public List<Library> getLibraries() {
            return List.of();
        }

        @Override
        public @Nullable String getInheritsFrom() {
            return null;
        }

        @Override
        public @Nullable String getType() {
            return null;
        }

        @Override
        public @Nullable String getMainClass() {
            return null;
        }

        @Override
        public @Nullable String getMinecraftArguments() {
            return minecraftArguments;
        }

        @Override
        public @Nullable Map<String, List<Argument>> getArguments() {
            return argumentsMap;
        }

        @Override
        public @Nullable AssetIndex getAssetIndex() {
            return null;
        }

        @Override
        public @Nullable Map<String, LoggingConfiguration> getLogging() {
            return null;
        }

        @Override
        public @Nullable Map<String, Download> getDownloads() {
            return null;
        }

        @Override
        public @Nullable JavaVersion getJavaVersion() {
            return null;
        }
    }

}
