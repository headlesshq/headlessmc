package io.github.headlesshq.headlessmc.launcher.server;

import io.github.headlesshq.headlessmc.launcher.profile.EulaStatus;
import io.github.headlesshq.headlessmc.launcher.profile.Profile;
import io.github.headlesshq.headlessmc.platform.VersionID;
import io.github.headlesshq.headlessmc.version.arg.Side;
import io.github.headlesshq.headlessmc.version.arg.VersionArg;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public interface ServerService {
    Optional<Profile> getServer(String name);

    Profile add(Path dir, VersionID id, @Nullable String name, int javaVersion);

    Profile save(Profile profile);

    void remove(Profile profile);

    List<Profile> listServers();

    Profile setEulaStatus(Profile profile, EulaStatus eulaStatus);

    default String getServerName(VersionArg arg) {
        return arg.withSide(Side.SERVER).toString("-");
    }

}
