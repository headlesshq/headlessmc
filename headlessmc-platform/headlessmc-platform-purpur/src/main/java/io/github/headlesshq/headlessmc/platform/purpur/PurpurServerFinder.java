package io.github.headlesshq.headlessmc.platform.purpur;

import io.github.headlesshq.headlessmc.platform.server.ServerFinder;
import jakarta.enterprise.context.ApplicationScoped;

import java.nio.file.Path;

/**
 * For Purpur, we do not need a vanilla-server jar,
 * so we just download the jar to {@link ServerFinder#DEFAULT_JAR}.
 *
 * @see PurpurInstaller
 */
@Purpur
@ApplicationScoped
public class PurpurServerFinder implements ServerFinder {
    @Override
    public Path findExecutable(Path serverDir) {
        return serverDir.resolve(ServerFinder.DEFAULT_JAR);
    }

}
