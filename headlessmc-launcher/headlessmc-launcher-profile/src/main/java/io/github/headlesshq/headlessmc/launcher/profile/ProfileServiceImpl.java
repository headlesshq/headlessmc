package io.github.headlesshq.headlessmc.launcher.profile;

import io.github.headlesshq.headlessmc.files.AppFiles;
import io.github.headlesshq.headlessmc.files.FileService;
import io.github.headlesshq.headlessmc.reflection.ReflectionRegistered;
import io.github.headlesshq.headlessmc.util.json.JsonParseException;
import io.github.headlesshq.headlessmc.util.json.JsonService;
import io.github.headlesshq.headlessmc.version.arg.VersionArg;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class ProfileServiceImpl implements ProfileService {
    private final GameDirService gameDirService;
    private final JsonService jsonService;
    private final FileService fileService;
    private final AppFiles files;

    @Override
    public Profile getProfile(VersionArg arg) {
        return getProfiles().stream()
            .filter(profile -> profile.version().equals(arg))
            .findFirst()
            .orElseGet(() -> createDefault(arg.toString("-"), arg));
    }

    @Override
    public Optional<Profile> getProfile(String name) {
        Path json = getProfilePath(name);
        if (!Files.exists(json)) {
            return Optional.empty();
        }

        return readProfile(json);
    }

    @Override
    public List<Profile> getProfiles() {
        Path dir = files.getProfilesDir();
        if (!Files.exists(dir)) {
            return List.of();
        }

        try (Stream<Path> paths = Files.list(dir)) {
            List<Profile> profiles = new ArrayList<>();
            paths.filter(path -> path.toString().endsWith(".json"))
                .forEach(path -> readProfile(path).ifPresent(profiles::add));

            return profiles;
        } catch (IOException e) {
            log.error("Failed to list profiles in {}", dir, e);
            return List.of();
        }
    }

    @Override
    public Profile save(Profile profile) {
        Path json = getProfilePath(profile.name());
        jsonService.write(json, profile, true);
        return profile;
    }

    @Override
    public void remove(Profile profile) {
        Path json = getProfilePath(profile.name());
        fileService.delete(json);
    }

    @Override
    public Profile createDefault(String name, VersionArg version) {
        return new Profile(name, version, gameDirService.getGameDir(version));
    }

    private Optional<Profile> readProfile(Path json) {
        try {
            ProfileVersionObject version = jsonService.parse(json, ProfileVersionObject.class);
            if (version.hmcVersion() > Profile.PROFILE_VERSION) {
                log.warn(
                    "Profile {} has is from a newer version {}, expected {}, ignoring, update HeadlessMc",
                    json, version.hmcVersion(), Profile.PROFILE_VERSION
                );

                return Optional.empty();
            }

            return Optional.of(jsonService.parse(json, Profile.class));
        } catch (JsonParseException e) {
            log.error("Failed to read profile {}", json, e);
            return Optional.empty();
        }
    }

    private Path getProfilePath(String name) {
        return fileService.getPath(files.getProfilesDir(), name.toLowerCase(Locale.ENGLISH) + ".json");
    }

    // check first to see if we are an outdated headlessmc version (Profile.PROFILE_VERSION)
    @RegisterForReflection
    private record ProfileVersionObject(int hmcVersion) implements ReflectionRegistered {

    }

}
