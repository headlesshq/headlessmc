package io.github.headlesshq.headlessmc.platform.fabric;

import io.github.headlesshq.headlessmc.platform.server.ServerFinder;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@Fabric
@ApplicationScoped
public class FabricServerFinder implements ServerFinder {
    public static final String FABRIC_SERVER_LAUNCH_JAR = "fabric-server-launch.jar";

    @Override
    public Path findExecutable(Path serverDir) {
        Path fabricJar = serverDir.resolve(FABRIC_SERVER_LAUNCH_JAR);
        if (Files.exists(fabricJar)) {
            return fabricJar;
        }

        log.warn("Failed to find {} in {}", FABRIC_SERVER_LAUNCH_JAR, serverDir);
        return serverDir.resolve(ServerFinder.DEFAULT_JAR);
    }

}
