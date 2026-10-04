package io.github.headlesshq.headlessmc.platform.vanilla;

import io.github.headlesshq.headlessmc.platform.Vanilla;
import io.github.headlesshq.headlessmc.platform.server.ServerFinder;
import jakarta.enterprise.context.ApplicationScoped;

import java.nio.file.Path;

@Vanilla
@ApplicationScoped
public class VanillaServerFinder implements ServerFinder {
    @Override
    public Path findExecutable(Path serverDir) {
        return serverDir.resolve(ServerFinder.DEFAULT_JAR);
    }

}
