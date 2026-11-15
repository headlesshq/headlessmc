package io.github.headlesshq.headlessmc.launcher.profile;

import io.github.headlesshq.headlessmc.files.AppFiles;
import io.github.headlesshq.headlessmc.files.FileService;
import io.github.headlesshq.headlessmc.util.json.JsonService;
import io.github.headlesshq.headlessmc.version.arg.Side;
import io.github.headlesshq.headlessmc.version.arg.VersionArg;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class ProfileServiceImplTest {
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
        service = new ProfileServiceImpl(jsonService, fileService, appFiles);
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
        // the data of the target is given the way it is read back from a file, as a json tree
        return new Profile(
            name,
            Optional.of(Side.CLIENT),
            new LaunchTargetHandle(VersionHandle.VERSION, tree(VersionHandle.of(version))),
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
    void saveWritesCurrentProfileVersion() {
        Profile legacy = sampleProfile("legacy", version()).withHmcVersion(0);

        Profile saved = service.save(legacy);

        assertEquals(Profile.PROFILE_VERSION, saved.hmcVersion());
        assertEquals(Optional.of(saved), service.getProfile("legacy"));
    }

    @Test
    void getProfileReadsPathsWrittenAsStrings() throws Exception {
        // profiles of format 0 were written with Jackson's default Path serializer, a file: URI string,
        // see ProfileMigrationTest. A path written like that is read in every format.
        Path gameDir = profilesDir.resolve("legacy").toAbsolutePath();
        service.save(sampleProfile("legacy", version()));
        Path file = profilesDir.resolve("legacy.json");
        String json = Files.readString(file)
            .replaceFirst(
                "\"path\"\\s*:\\s*\\{[^}]*}",
                Matcher.quoteReplacement("\"path\": \"" + gameDir.toUri() + "\"")
            );
        Files.writeString(file, json);

        Optional<Profile> loaded = service.getProfile("legacy");

        assertTrue(loaded.isPresent(), json);
        assertEquals(gameDir, loaded.get().path());
    }

    @Test
    void getProfileIgnoresCorruptFile() throws Exception {
        Files.createDirectories(profilesDir);
        Files.writeString(profilesDir.resolve("corrupt.json"), "not json");

        assertTrue(service.getProfile("corrupt").isEmpty());
        assertTrue(service.getProfiles().isEmpty());
    }

    @Test
    void theDataOfATargetIsReadBackAsItHasBeenWritten() {
        VersionHandle handle = new VersionHandle(version(), version().withBuild("0.18.1"), profilesDir.resolve("pin.json").toAbsolutePath());
        Profile profile = sampleProfile("typed", version()).withTarget(handle.asTarget());

        service.save(profile);

        Profile loaded = service.getProfile("typed").orElseThrow();
        assertEquals(VersionHandle.VERSION, loaded.target().name());
        assertEquals(handle, jsonService.convert(loaded.target().data(), VersionHandle.class));
    }

    @Test
    void theDataOfAnUnknownTargetSurvivesUnchanged() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("anything", List.of(1, "two", Map.of("three", true)));
        data.put("nothing", null);
        Profile profile = sampleProfile("plugin", version()).withTarget(new LaunchTargetHandle("from-a-plugin", data));

        service.save(profile);
        Profile loaded = service.getProfile("plugin").orElseThrow();
        service.save(loaded);

        assertEquals(profile, loaded);
        assertEquals(loaded, service.getProfile("plugin").orElseThrow());
    }

    @Test
    void profilesAreFoundIgnoringTheCaseOfTheirName() {
        service.save(sampleProfile("MixedCase", version()));

        assertEquals("MixedCase", service.getProfile("mixedcase").orElseThrow().name());
        assertTrue(Files.exists(profilesDir.resolve("mixedcase.json")));
    }

    @Test
    void removeDeletesTheFile() {
        Profile profile = service.save(sampleProfile("gone", version()));

        service.remove(profile);

        assertTrue(service.getProfile("gone").isEmpty());
        assertFalse(Files.exists(profilesDir.resolve("gone.json")));
    }

    /** Converts a handle into the json tree it is read back as. */
    private Object tree(VersionHandle handle) {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        jsonService.write(out, new LaunchTargetHandle(VersionHandle.VERSION, handle), false);
        return jsonService.parse(new java.io.ByteArrayInputStream(out.toByteArray()), LaunchTargetHandle.class).data();
    }

}
