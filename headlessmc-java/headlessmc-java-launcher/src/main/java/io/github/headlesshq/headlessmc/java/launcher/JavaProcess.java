package io.github.headlesshq.headlessmc.java.launcher;

import io.github.headlesshq.headlessmc.java.Java;
import io.github.headlesshq.headlessmc.java.service.JavaService;
import lombok.*;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;

// TODO: inmemory?
@Data
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(builderMethodName = "", buildMethodName = "launch")
public final class JavaProcess {
    @Singular
    private final List<Consumer<ProcessBuilder>> configurations;
    @Singular
    private final List<String> jvmArgs;
    @Singular
    private final List<String> args;
    @Singular(value = "classpath")
    private final List<String> classpath;
    private final Map<String, String> systemProperties;
    private final @Nullable Process process;
    private final @Nullable String mainClass;
    private final @Nullable Path jar;
    private final @Nullable Path processDir;
    private final int version;
    private final Java java;

    public void waitForSuccess() throws IOException {
        try {
            int result = waitFor();
            if (result != 0) {
                throw new IOException("Process exited with code " + result);
            }
        } catch (InterruptedException e) {
            throw new IOException(e);
        }
    }

    public int waitFor() throws InterruptedException {
        return process == null ? 0 : process.waitFor();
    }

    public static final class JavaProcessBuilder {
        private final Map<String, @Nullable String> systemProperties = new LinkedHashMap<>();
        private final JavaLauncher launcher;
        private final JavaService service;
        private @Nullable Integer version;
        private @Nullable Java java;

        JavaProcessBuilder(JavaLauncher launcher, JavaService service) {
            this.launcher = launcher;
            this.service = service;
        }

        @SuppressWarnings("unused")
        private JavaProcessBuilder() {
            throw new AssertionError("Should not be called");
        }

        public JavaProcessBuilder systemProperty(String key, @Nullable String value) {
            this.systemProperties.put(key, value);
            return this;
        }

        public JavaProcessBuilder inheritIO() {
            return redirectIn(ProcessBuilder.Redirect.INHERIT)
                    .redirectOutAndErr(ProcessBuilder.Redirect.INHERIT);
        }

        public JavaProcessBuilder redirectOutAndErr(ProcessBuilder.Redirect redirect) {
            return configuration(builder -> builder.redirectError(redirect).redirectOutput(redirect));
        }

        public JavaProcessBuilder redirectIn(ProcessBuilder.Redirect redirect) {
            return configuration(builder -> builder.redirectInput(redirect));
        }

        public JavaProcess launch() throws IOException {
            if (java == null) {
                if (version == null) {
                    throw new IllegalStateException("Version must be set");
                }

                java = service.findBestVersion(version);
                if (java == null) {
                    throw new IOException("Unable to find Java version " + version);
                }
            }

            List<String> command = buildCommand();
            ProcessBuilder builder = new ProcessBuilder(command);
            if (processDir != null) {
                builder.directory(processDir.toFile());
            }

            for (Consumer<ProcessBuilder> configuration : configurations) {
                configuration.accept(builder);
            }

            Process process = builder.start();
            JavaProcess javaProcess = new JavaProcess(
                configurations,
                jvmArgs,
                args,
                classpath,
                systemProperties,
                process,
                mainClass,
                jar,
                processDir,
                java.getVersion(),
                java
            );

            launcher.addProcess(javaProcess);
            return javaProcess;
        }

        private List<String> buildCommand() {
            List<String> command = new ArrayList<>();
            command.add(Objects.requireNonNull(java).getExecutable());
            command.addAll(jvmArgs);
            for (Map.Entry<String, String> systemProperty : systemProperties.entrySet()) {
                if (systemProperty.getValue() == null) {
                    command.add("-D" + systemProperty.getKey());
                } else {
                    command.add("-D" + systemProperty.getKey() + "=" + systemProperty.getValue());
                }
            }

            if (classpath.isEmpty()) {
                command.add("-jar");
                command.add(Objects.requireNonNull(jar, "Jar or classpath need to be set").toAbsolutePath().toString());
                if (mainClass != null) {
                    command.add(mainClass);
                }
            } else {
                command.add("-cp");
                command.add(String.join("" + File.pathSeparatorChar, classpath));
                command.add(Objects.requireNonNull(mainClass, "Main class need to be set for classpath"));
            }

            return command;
        }
    }

}
