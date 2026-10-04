package io.github.headlesshq.headlessmc.files;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;

import java.io.IOException;
import java.nio.file.Path;

@FunctionalInterface
public interface FileConsumer {
    void accept(Path path) throws IOException, InterruptedException, HeadlessMcException;

}
