package io.github.headlesshq.headlessmc.platform.server;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.platform.VersionID;
import io.github.headlesshq.headlessmc.version.arg.VersionArg;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * What a {@link ServerInstaller} has installed.
 * When installing a server we need to resolve the Java
 * version to launch (and maybe run the platform installer with)
 * for the server.
 *
 * @param javaVersion the java version to launch the server with.
 * @param vmArgs      the default jvm args the server profile should be created with,
 *                    e.g. the flags recommended by Paper.
 * @param build       the name of the build that has been installed, as in {@link VersionArg#build()}.
 *                    A server is often installed for "any build" of a version, this is the build it became.
 *                    {@code null} if the platform has no builds, like vanilla, or if the installer cannot tell
 *                    which build it installed.
 */
// TODO: maybe the ServerInstaller should actually get the java version from the outside?
public record ServerInstallation(int javaVersion, List<String> vmArgs, @Nullable String build) {
    public ServerInstallation(int javaVersion) {
        this(javaVersion, List.of());
    }

    public ServerInstallation(int javaVersion, List<String> vmArgs) {
        this(javaVersion, vmArgs, null);
    }

    /**
     * @param id the version that has been asked for, see {@link ServerInstaller#installServer}.
     * @return the version that has been installed: the given version with the {@link #build()} that has been
     * installed, or the given version itself if the build is not known, or is not a build of the platform.
     */
    public VersionArg installed(VersionID id) {
        VersionArg arg = id.asArg();
        if (build == null || id.getBuild().filter(specified -> specified.getName().equals(build)).isPresent()) {
            return arg;
        }

        // a version is stored to be resolved again, which only works with a build the platform knows by that name
        try {
            boolean known = id.getPlatform().getVersionService().getBuilds(id.getVersion()).stream()
                .anyMatch(platformBuild -> platformBuild.getName().equals(build));
            return known ? arg.withBuild(build) : arg;
        } catch (HeadlessMcException e) {
            return arg;
        }
    }
}
