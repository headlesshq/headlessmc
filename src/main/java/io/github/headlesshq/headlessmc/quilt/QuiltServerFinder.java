package io.github.headlesshq.headlessmc.quilt;

import io.github.headlesshq.headlessmc.platform.server.ServerFinder;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;

@Quilt
@ApplicationScoped
public class QuiltServerFinder implements ServerFinder {
    private static final Logger log = LoggerFactory.getLogger(QuiltServerFinder.class);
    public static final String QUILT_SERVER_LAUNCH_JAR = "quilt-server-launch.jar";

    @Override
    public Path findExecutable(Path serverDir) {
        Path quiltJar = serverDir.resolve(QUILT_SERVER_LAUNCH_JAR);
        if (Files.exists(quiltJar)) {
            return quiltJar;
        }

        log.warn("Failed to find {} in {}", QUILT_SERVER_LAUNCH_JAR, serverDir);
        return serverDir.resolve(ServerFinder.DEFAULT_JAR);
    }

}
