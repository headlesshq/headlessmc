package io.github.headlesshq.headlessmc.launcher.server;

import io.github.headlesshq.headlessmc.launcher.profile.EulaStatus;
import io.github.headlesshq.headlessmc.launcher.profile.Profile;
import io.github.headlesshq.headlessmc.platform.VersionID;
import io.github.headlesshq.headlessmc.platform.server.ServerInstaller;
import io.github.headlesshq.headlessmc.version.arg.Side;
import io.github.headlesshq.headlessmc.version.arg.VersionArg;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public interface ServerService {
    Optional<Profile> getServer(String name);

    /**
     * Creates and saves a server profile for an installed server.
     *
     * @param dir          the directory the server has been installed in.
     * @param id           the version of the server.
     * @param name         the name of the server, if {@code null} a name is derived from the version.
     * @param installation the result of installing the server, provides java version and default jvm args.
     * @return the saved server profile.
     */
    Profile add(Path dir, VersionID id, @Nullable String name, ServerInstaller.Installation installation);

    Profile save(Profile profile);

    void remove(Profile profile);

    List<Profile> listServers();

    Profile setEulaStatus(Profile profile, EulaStatus eulaStatus);

    default String getServerName(VersionArg arg) {
        return arg.withSide(Side.SERVER).toString("-");
    }

}
