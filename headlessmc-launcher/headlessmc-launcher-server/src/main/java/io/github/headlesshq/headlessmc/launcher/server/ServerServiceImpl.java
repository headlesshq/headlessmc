package io.github.headlesshq.headlessmc.launcher.server;

import io.github.headlesshq.headlessmc.launcher.profile.EulaStatus;
import io.github.headlesshq.headlessmc.launcher.profile.Profile;
import io.github.headlesshq.headlessmc.launcher.profile.ProfileService;
import io.github.headlesshq.headlessmc.platform.VersionID;
import io.github.headlesshq.headlessmc.version.arg.Side;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class ServerServiceImpl implements ServerService {
    private final ProfileService profileService;

    @Override
    public Optional<Profile> getServer(String name) {
        return listServers().stream()
            .filter(server -> server.name().equalsIgnoreCase(name))
            .findFirst();
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
        if (eulaStatus.equals(profile.eulaStatus())) {
            return profile;
        }

        Profile result = profile.withEulaStatus(eulaStatus);
        return profileService.save(result);
    }

}
