package io.github.headlesshq.headlessmc.platform.paper;

import io.github.headlesshq.headlessmc.platform.server.ServerFinder;
import jakarta.enterprise.context.ApplicationScoped;

import java.nio.file.Path;

/**
 * For Paper, we do not need a vanilla-server jar,
 * so we just download the jar to {@link ServerFinder#DEFAULT_JAR}.
 *
 * @see PaperInstaller
 */
@Paper
@ApplicationScoped
public class PaperServerFinder implements ServerFinder {
    @Override
    public Path findExecutable(Path serverDir) {
        return serverDir.resolve(ServerFinder.DEFAULT_JAR);
    }

}
