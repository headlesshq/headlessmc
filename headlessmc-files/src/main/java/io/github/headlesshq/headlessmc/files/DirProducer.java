package io.github.headlesshq.headlessmc.files;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;

import java.nio.file.Paths;

@ApplicationScoped
public class DirProducer {
    @Produces
    @Dependent
    @HeadlessMcDir
    public FileManager getHeadlessMcDir() {
        return FileManager.of(Paths.get("HeadlessMc"));
    }

    @Produces
    @Dependent
    @McDir
    public FileManager getMcDir() {
        // TODO: from setting?!
        return FileManager.of(Paths.get("HeadlessMc"));
    }

}
