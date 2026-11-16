package io.github.headlesshq.headlessmc.launcher.profile;

import io.github.headlesshq.headlessmc.version.arg.VersionArg;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
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
    public Optional<Profile> getProfile(String name) {
        return Optional.ofNullable(profiles.get(name.toLowerCase(Locale.ENGLISH)));
    }

    @Override
    public List<Profile> getProfiles() {
        return new ArrayList<>(profiles.values());
    }

    @Override
    public Profile save(Profile profile) {
        profiles.put(profile.name().toLowerCase(Locale.ENGLISH), profile);
        return profile;
    }

    @Override
    public void remove(Profile profile) {
        profiles.remove(profile.name().toLowerCase(Locale.ENGLISH));
    }

    /**
     * Saves a new, unresolved profile for a version, see {@link Profile#Profile(String, VersionArg, Path)}.
     *
     * @param name    the name of the profile.
     * @param version the version of the profile, a server if it has that side, otherwise a client.
     * @return the saved profile, its game directory is {@code <root>/<name>}.
     */
    public Profile createDefault(String name, VersionArg version) {
        return save(new Profile(name, version, root.resolve(name)));
    }

}
