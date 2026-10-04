package io.github.headlesshq.headlessmc.launcher.profile;

import io.github.headlesshq.headlessmc.files.AppFiles;
import io.github.headlesshq.headlessmc.files.FileService;
import io.github.headlesshq.headlessmc.util.json.JsonService;
import io.github.headlesshq.headlessmc.version.arg.VersionArg;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class ProfileServiceImplTest {
    @Inject
    GameDirService gameDirService;
    @Inject
    JsonService jsonService;
    @Inject
    FileService fileService;
    @Inject
    AppFiles appFiles;

    ProfileServiceImpl service;
    Path profilesDir;

    @BeforeEach
    void setUp() {
        service = new ProfileServiceImpl(gameDirService, jsonService, fileService, appFiles);
        profilesDir = appFiles.getProfilesDir();
        fileService.delete(profilesDir);
    }

    @AfterEach
    void tearDown() {
        fileService.delete(profilesDir);
    }

    private VersionArg version() {
        return new VersionArg(Optional.empty(), "fabric", "1.21.1", Optional.of("0.18.1"));
    }

    private Profile sampleProfile(String name, VersionArg version) {
        SequencedMap<String, String> systemProperties = new LinkedHashMap<>();
        systemProperties.put("key", "value");
        return new Profile(
            name,
            version,
            version,
            profilesDir.resolve(name).toAbsolutePath(),
            new LaunchOptions(Optional.empty(), Optional.empty(), Optional.empty(), false),
            List.of(),
            systemProperties,
            List.of("-Xmx2G"),
            List.of("--fullscreen"),
            21,
            EulaStatus.UNKNOWN,
            false,
            Profile.PROFILE_VERSION
        );
    }

    @Test
    void getProfileByNameReturnsEmptyWhenMissing() {
        assertTrue(service.getProfile("does-not-exist").isEmpty());
    }

    @Test
    void getProfilesReturnsEmptyListWhenDirectoryMissing() {
        assertTrue(service.getProfiles().isEmpty());
    }

    @Test
    void saveThenGetProfileRoundTrips() {
        Profile profile = sampleProfile("my-profile", version());
        service.save(profile);

        Optional<Profile> loaded = service.getProfile("my-profile");
        assertTrue(loaded.isPresent());
        assertEquals(profile, loaded.get());
    }

    @Test
    void saveWritesFileNamedAfterProfile() {
        Profile profile = sampleProfile("my-profile", version());
        service.save(profile);

        assertTrue(Files.exists(profilesDir.resolve("my-profile.json")));
    }

    @Test
    void saveOverwritesExistingProfile() {
        Profile profile = sampleProfile("my-profile", version());
        service.save(profile);

        Profile updated = profile.withHasDefaultClientJvmArgs(true);
        service.save(updated);

        assertEquals(Optional.of(updated), service.getProfile("my-profile"));
    }

    @Test
    void getProfilesReturnsAllSavedProfiles() {
        service.save(sampleProfile("profile-a", version()));
        service.save(sampleProfile("profile-b", version()));

        List<Profile> profiles = service.getProfiles();
        assertEquals(2, profiles.size());
        assertTrue(profiles.stream().anyMatch(profile -> profile.name().equals("profile-a")));
        assertTrue(profiles.stream().anyMatch(profile -> profile.name().equals("profile-b")));
    }

    @Test
    void getProfilesIgnoresNonJsonFiles() throws Exception {
        service.save(sampleProfile("profile-a", version()));
        Files.writeString(profilesDir.resolve("notes.txt"), "hello");

        assertEquals(1, service.getProfiles().size());
    }

    @Test
    void getProfileIgnoresFileFromNewerHmcVersion() throws Exception {
        Files.createDirectories(profilesDir);
        Files.writeString(profilesDir.resolve("future.json"), "{\"hmcVersion\": 99}");

        assertTrue(service.getProfile("future").isEmpty());
        assertTrue(service.getProfiles().isEmpty());
    }

    @Test
    void getProfileIgnoresCorruptFile() throws Exception {
        Files.createDirectories(profilesDir);
        Files.writeString(profilesDir.resolve("corrupt.json"), "not json");

        assertTrue(service.getProfile("corrupt").isEmpty());
        assertTrue(service.getProfiles().isEmpty());
    }

    @Test
    void getProfileByVersionReturnsExistingMatch() {
        VersionArg arg = version();
        Profile profile = sampleProfile("existing", arg);
        service.save(profile);

        assertEquals(profile, service.getProfile(arg));
    }

    @Test
    void getProfileByVersionCreatesUnsavedDefaultWhenMissing() {
        VersionArg arg = version();

        Profile result = service.getProfile(arg);

        assertEquals(arg, result.version());
        assertEquals(arg, result.currentVersion());
        assertEquals(Profile.PROFILE_VERSION, result.hmcVersion());
        assertEquals(Map.of(), result.systemProperties());
        assertEquals(List.of(), result.vmArgs());
        assertEquals(List.of(), result.gameArgs());
        assertNull(result.javaVersion());
        assertFalse(result.hasDefaultClientJvmArgs());
        assertTrue(service.getProfile(result.name()).isEmpty());
    }

}
