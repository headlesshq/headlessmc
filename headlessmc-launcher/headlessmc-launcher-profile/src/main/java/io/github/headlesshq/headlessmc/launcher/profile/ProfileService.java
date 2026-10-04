package io.github.headlesshq.headlessmc.launcher.profile;

import io.github.headlesshq.headlessmc.version.arg.VersionArg;

import java.util.List;
import java.util.Optional;

public interface ProfileService {
    Profile getProfile(VersionArg arg);

    Optional<Profile> getProfile(String name);

    List<Profile> getProfiles();

    Profile save(Profile profile);

    void remove(Profile profile);

    Profile createDefault(String name, VersionArg version);

}
