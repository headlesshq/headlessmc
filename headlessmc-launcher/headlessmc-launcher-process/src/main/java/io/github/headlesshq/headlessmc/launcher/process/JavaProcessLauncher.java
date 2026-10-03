package io.github.headlesshq.headlessmc.launcher.process;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.java.launcher.JavaProcess;
import io.github.headlesshq.headlessmc.java.launcher.JavaProcessBuilder;
import lombok.RequiredArgsConstructor;

import java.nio.file.Path;
import java.util.Optional;

@RequiredArgsConstructor
final class JavaProcessLauncher implements ProcessLauncher {
    private final JavaProcessBuilder processBuilder;
    private final Path gameDir;

    @Override
    public synchronized McProcess launch(boolean pipeIO) throws HeadlessMcException {
        JavaProcess process = processBuilder.pipeIO(pipeIO).start();
        return new McProcess(process.getId(), Optional.of(process), process.getProcess());
    }

    @Override
    public Optional<JavaProcessBuilder> getJavaProcessBuilder() {
        return Optional.of(processBuilder);
    }

    @Override
    public Optional<ProcessBuilder> getProcessBuilder() {
        return Optional.empty();
    }

    @Override
    public Path getGameDir() {
        return gameDir;
    }

    @Override
    public String getId() {
        return processBuilder.id();
    }

}
