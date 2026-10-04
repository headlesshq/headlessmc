package io.github.headlesshq.headlessmc.launcher.server;

import io.github.headlesshq.headlessmc.launcher.profile.EulaStatus;
import io.github.headlesshq.headlessmc.launcher.profile.Profile;
import io.github.headlesshq.headlessmc.launcher.profile.ProfileService;
import io.github.headlesshq.headlessmc.platform.VersionID;
import io.github.headlesshq.headlessmc.version.arg.Side;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * A {@link ServerService} over an in-memory {@link ProfileService}
 * (e.g. {@link io.github.headlesshq.headlessmc.launcher.profile.FakeProfileService}).
 */
public class FakeServerService implements ServerService {
    private final ProfileService profileService;

    public FakeServerService(ProfileService profileService) {
        this.profileService = profileService;
    }

    @Override
    public Optional<Profile> getServer(String name) {
        return profileService.getProfile(name)
            .filter(profile -> Side.SERVER.equals(profile.side()));
    }

    @Override
    public Profile add(Path dir, VersionID id, @Nullable String nameIn, int javaVersion) {
        String name = nameIn == null ? getServerName(id.asArg()) : nameIn;
        Profile profile = new Profile(name, id.asArg().withSide(Side.SERVER), dir).withJavaVersion(javaVersion);
        return profileService.save(profile);
    }

    @Override
    public Profile save(Profile profile) {
        return profileService.save(profile);
    }

    @Override
    public void remove(Profile profile) {
        profileService.remove(profile);
    }

    @Override
    public List<Profile> listServers() {
        return profileService.getProfiles().stream()
            .filter(profile -> Side.SERVER.equals(profile.side()))
            .toList();
    }

    @Override
    public Profile setEulaStatus(Profile profile, EulaStatus eulaStatus) {
        return profileService.save(profile.withEulaStatus(eulaStatus));
    }

}
