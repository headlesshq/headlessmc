package io.github.headlesshq.headlessmc.launcher.client;

import io.github.headlesshq.headlessmc.auth.Account;
import io.github.headlesshq.headlessmc.files.McFiles;
import io.github.headlesshq.headlessmc.launcher.assets.AssetsLocation;
import io.github.headlesshq.headlessmc.launcher.profile.LaunchOptions;
import io.github.headlesshq.headlessmc.os.CPU;
import io.github.headlesshq.headlessmc.version.ProcessedVersion;
import io.github.headlesshq.headlessmc.version.Version;
import io.github.headlesshq.headlessmc.version.util.TemplateStrings;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class TemplateStringServiceTest {
    private TemplateStrings create(Path root, LaunchOptions options) {
        return new TemplateStringService(CPU.X64).create(
            McFiles.of(root.resolve("mc")),
            new AssetsLocation("assets-17", root.resolve("assets")),
            new Account("msa", "Steve", "uuid-steve", "token-123", Account.TYPE_MSA, "xuid"),
            root.resolve("game"),
            new ProcessedVersion(List.of(new FakeVersion())),
            options
        );
    }

    private static LaunchOptions noOptions() {
        return new LaunchOptions(Optional.empty(), Optional.empty(), Optional.empty(), false);
    }

    @Test
    public void createsBaseTemplates(@TempDir Path root) {
        TemplateStrings templates = create(root, noOptions());

        assertEquals("Steve", templates.process("${auth_player_name}"));
        assertEquals("token-123", templates.process("${auth_access_token}"));
        assertEquals("token-123", templates.process("${auth_session}"));
        assertEquals("uuid-steve", templates.process("${auth_uuid}"));
        assertEquals("assets-17", templates.process("${assets_index_name}"));
        assertEquals(root.resolve("assets").toAbsolutePath().toString(), templates.process("${assets_root}"));
        assertEquals(root.resolve("game").toAbsolutePath().toString(), templates.process("${game_directory}"));
        assertEquals(File.pathSeparator, templates.process("${classpath_separator}"));
        assertEquals("64", templates.process("${arch}"));
        assertEquals("test-version", templates.process("${version_name}"));
        assertEquals("release", templates.process("${version_type}"));
        assertTrue(templates.process("${library_directory}").endsWith("libraries"));
    }

    @Test
    public void addsResolutionAndQuickPlayOptions(@TempDir Path root) {
        LaunchOptions options = new LaunchOptions(
            Optional.of(new LaunchOptions.Resolution(1024, 768)),
            Optional.of("quickplay/log.json"),
            Optional.of(new LaunchOptions.Join(LaunchOptions.Join.Type.SERVER, "example.com")),
            false
        );

        TemplateStrings templates = create(root, options);

        assertEquals("1024", templates.process("${resolution_width}"));
        assertEquals("768", templates.process("${resolution_height}"));
        assertEquals("example.com", templates.process("${quickPlayMultiplayer}"));
        assertEquals("quickplay/log.json", templates.process("${quickPlayPath}"));
    }

    @Test
    public void addsSingleplayerAndRealmsJoins(@TempDir Path root) {
        TemplateStrings singleplayer = create(root, new LaunchOptions(
            Optional.empty(), Optional.empty(),
            Optional.of(new LaunchOptions.Join(LaunchOptions.Join.Type.SINGLEPLAYER, "world")), false));
        assertEquals("world", singleplayer.process("${quickPlaySingleplayer}"));

        TemplateStrings realms = create(root, new LaunchOptions(
            Optional.empty(), Optional.empty(),
            Optional.of(new LaunchOptions.Join(LaunchOptions.Join.Type.REALMS, "realm-id")), false));
        assertEquals("realm-id", realms.process("${quickPlayRealms}"));
    }

    @Test
    public void addsDashesToUndashedUuids() {
        // Yggdrasil servers return uuids without dashes
        assertEquals(
            "b882c2ee-7132-43eb-a528-415cb26e151b",
            TemplateStringService.formatUuid("b882c2ee713243eba528415cb26e151b"));
    }

    @Test
    public void keepsDashedUuids() {
        // Microsoft accounts already provide the dashed form
        String uuid = "b882c2ee-7132-43eb-a528-415cb26e151b";
        assertEquals(uuid, TemplateStringService.formatUuid(uuid));
    }

    @Test
    public void keepsValuesThatAreNoUuids() {
        // e.g. offline accounts, whose provider should not be broken by this
        assertEquals("uuid-steve", TemplateStringService.formatUuid("uuid-steve"));
        assertEquals("", TemplateStringService.formatUuid(""));
        assertNull(TemplateStringService.formatUuid(null));
    }

    private static final class FakeVersion implements Version {
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
            return "release";
        }

        @Override
        public @Nullable String getMainClass() {
            return null;
        }

        @Override
        public @Nullable String getMinecraftArguments() {
            return null;
        }

        @Override
        public @Nullable Map<String, List<Argument>> getArguments() {
            return null;
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
