package io.github.headlesshq.headlessmc.java.launcher;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.java.Java;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// TODO: AppCDS support
@Data
@EqualsAndHashCode(callSuper = true)
final class ProcessBuilderImpl extends AbstractJavaProcessBuilder {
    private final JavaLauncherServiceImpl service;

    @Override
    public JavaProcess start() throws HeadlessMcException {
        List<String> command = buildCommand();
        Java java = findJava();
        Path executable = java.executable().get(service.fs.getFileSystem()).orElse(null);
        if (executable == null) {
            throw new JavaProcessException(java + " is not available on filesystem " + service.fs.getFileSystem());
        }

        List<String> fullCommand = new ArrayList<>(command.size() + 1);
        fullCommand.add(executable.toAbsolutePath().toString());
        fullCommand.addAll(command);

        ProcessBuilder processBuilder = new ProcessBuilder(fullCommand);

        if (isPipeIO()) {
            processBuilder
                .redirectError(ProcessBuilder.Redirect.PIPE)
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .redirectInput(ProcessBuilder.Redirect.PIPE);
        } else {
            processBuilder.inheritIO();
        }

        try {
            Path dir = getDirectory();
            if (dir != null) {
                Files.createDirectories(dir.toAbsolutePath());
                processBuilder.directory(dir.toAbsolutePath().toFile());
            }

            Process process = processBuilder.start();
            return new JavaProcessImpl(
                getId(),
                systemPropertiesToMap(),
                new ArrayList<>(getClassPath()),
                new ArrayList<>(getJvmArgs()),
                new ArrayList<>(getArgs()),
                Optional.of(java),
                Optional.of(java.version()),
                Optional.ofNullable(getMainClass()),
                Optional.ofNullable(dir),
                Optional.ofNullable(getJar()),
                Optional.of(process)
            );
        } catch (IOException e) {
            throw new JavaProcessException(e);
        }
    }

    private Java findJava() {
        Java java = getJava();
        if (java != null) {
            return java;
        }

        Integer version = getVersion();
        if (version == null) {
            throw new JavaProcessException("Failed to launch, no java version specified");
        }

        return service.javaFinder.findJava(version);
    }

}
