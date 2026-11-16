package io.github.headlesshq.headlessmc.launcher.profile;

import io.github.headlesshq.headlessmc.files.AppFiles;
import io.github.headlesshq.headlessmc.files.DefaultFileService;
import io.github.headlesshq.headlessmc.files.DefaultFileSystemProvider;
import io.github.headlesshq.headlessmc.files.FileService;
import io.github.headlesshq.headlessmc.files.McFiles;
import io.github.headlesshq.headlessmc.files.McFilesService;
import io.github.headlesshq.headlessmc.files.TestFiles;
import io.github.headlesshq.headlessmc.files.relative.AppFilesDataPathSource;
import io.github.headlesshq.headlessmc.files.relative.McFilesPathSource;
import io.github.headlesshq.headlessmc.util.json.JsonService;
import io.github.headlesshq.headlessmc.util.json.RelativePathService;
import io.github.headlesshq.headlessmc.util.json.jackson.DefaultJacksonJsonService;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * A real {@link ProfileServiceImpl} over a test directory, and the profile files in
 * {@code src/test/resources/profiles}. These files are profiles as they have been written by HeadlessMc
 * in the past, they must stay readable forever and are never regenerated.
 * Paths in them are placeholders, since they are different on every machine.
 */
final class ProfileTestFiles {
    final Path root;
    final McFiles mcFiles;
    final AppFiles appFiles;
    final Path gameDir;
    final FileService fileService = new DefaultFileService(new DefaultFileSystemProvider());
    final JsonService jsonService;
    final ProfileServiceImpl service;
    final Path profilesDir;

    ProfileTestFiles(Path root) {
        this.root = root;
        this.mcFiles = TestFiles.mcFiles(root);
        this.appFiles = TestFiles.appFiles(root);
        this.gameDir = root.resolve("game dir");
        this.jsonService = new DefaultJacksonJsonService(new RelativePathService(List.of(
            new McFilesPathSource(McFilesService.of(mcFiles)),
            new AppFilesDataPathSource(appFiles)
        )));
        this.service = new ProfileServiceImpl(jsonService, fileService, appFiles);
        this.profilesDir = appFiles.getProfilesDir();
    }

    /**
     * Copies a profile from the test resources into the profiles directory.
     *
     * @param name the name of the resource without {@code .json}, which is also the name of the profile.
     * @return the file of the profile.
     */
    Path install(String name) {
        return install(name, mcFiles.getMcDir());
    }

    /**
     * @param mcDir the directory written as the location of the {@code .minecraft} directory at the time
     *              the profile has been saved.
     */
    Path install(String name, Path mcDir) {
        String content = read(name)
            .replace("@GAME_DIR_URI@", gameDir.toUri().toString())
            .replace("@MC_DIR@", escape(mcDir.toAbsolutePath().toString()))
            .replace("@DATA_DIR@", escape(appFiles.getDataDir().toAbsolutePath().toString()));
        try {
            Files.createDirectories(profilesDir);
            Path file = profilesDir.resolve(name + ".json");
            Files.writeString(file, content);
            return file;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    Profile load(String name) {
        install(name);
        return service.getProfile(name).orElseThrow(() -> new AssertionError("Failed to read profile " + name));
    }

    static String read(String name) {
        try (InputStream in = ProfileTestFiles.class.getResourceAsStream("/profiles/" + name + ".json")) {
            return new String(Objects.requireNonNull(in, name).readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Escapes a path for a json string, the separators of Windows paths have to be escaped. */
    private static String escape(String path) {
        return path.replace("\\", "\\\\").replace("\"", "\\\"");
    }

}
