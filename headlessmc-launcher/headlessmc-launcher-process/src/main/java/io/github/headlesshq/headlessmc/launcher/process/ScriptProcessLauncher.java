package io.github.headlesshq.headlessmc.launcher.process;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.java.launcher.JavaProcessBuilder;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

@RequiredArgsConstructor
final class ScriptProcessLauncher implements ProcessLauncher {
    private final ProcessBuilder processBuilder;
    private final String id;
    private final Path gameDir;

    @Override
    public synchronized McProcess launch(boolean pipeIO) throws HeadlessMcException {
        if (pipeIO) {
            processBuilder
                .redirectError(ProcessBuilder.Redirect.PIPE)
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .redirectInput(ProcessBuilder.Redirect.PIPE);
        }

        try {
            Process process = processBuilder.start();
            return new McProcess(id, Optional.empty(), Optional.of(process));
        } catch (IOException e) {
            throw new LaunchException("Failed to launch " + id, e);
        }
    }

    @Override
    public Optional<JavaProcessBuilder> getJavaProcessBuilder() {
        return Optional.empty();
    }

    @Override
    public Optional<ProcessBuilder> getProcessBuilder() {
        return Optional.of(processBuilder);
    }

    @Override
    public Path getGameDir() {
        return gameDir;
    }

    @Override
    public String getId() {
        return id;
    }

}
