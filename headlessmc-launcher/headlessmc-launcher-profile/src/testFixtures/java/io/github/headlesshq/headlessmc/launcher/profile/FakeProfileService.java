package io.github.headlesshq.headlessmc.launcher.profile;

import io.github.headlesshq.headlessmc.version.arg.VersionArg;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * An in-memory {@link ProfileService}.
 */
public class FakeProfileService implements ProfileService {
    private final Map<String, Profile> profiles = new LinkedHashMap<>();
    private final Path root;

    public FakeProfileService(Path root) {
        this.root = root;
    }

    @Override
    public Profile getProfile(VersionArg arg) {
        return getProfiles().stream()
            .filter(profile -> profile.version().equals(arg))
            .findFirst()
            .orElseGet(() -> createDefault(arg.toString("-"), arg));
    }

    @Override
    public Optional<Profile> getProfile(String name) {
        return Optional.ofNullable(profiles.get(name));
    }

    @Override
    public List<Profile> getProfiles() {
        return new ArrayList<>(profiles.values());
    }

    @Override
    public Profile save(Profile profile) {
        profiles.put(profile.name(), profile);
        return profile;
    }

    @Override
    public void remove(Profile profile) {
        profiles.remove(profile.name());
    }

    @Override
    public Profile createDefault(String name, VersionArg version) {
        return save(new Profile(name, version, root.resolve(name)));
    }

}
