package io.github.headlesshq.headlessmc.plugins;

import io.github.headlesshq.headlessmc.console.Console;
import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.files.FileService;
import io.github.headlesshq.headlessmc.java.Java;
import io.github.headlesshq.headlessmc.java.JavaService;
import io.github.headlesshq.headlessmc.java.launcher.JavaFinder;
import io.github.headlesshq.headlessmc.java.launcher.JavaLauncherService;
import io.github.headlesshq.headlessmc.os.OS;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Compiles a build of HeadlessMc with plugins into a native executable, which starts in milliseconds
 * instead of the ~0.5 s the JVM needs.
 * <ol>
 *     <li>The mutable jar is copied and the plugins and libraries of the build are placed into its providers.</li>
 *     <li>The {@code NativeAugmenterMain} of headlessmc-plugins-augmenter, which the mutable jar contains at
 *     {@value #AUGMENTER}, generates the sources of the native image.</li>
 *     <li>{@code native-image} compiles them, either with a local GraalVM 25,
 *     or in a container with Docker or Podman.</li>
 * </ol>
 * The executable is placed into {@code <build>/native}, the {@link PluginBootstrap} launches it instead of
 * the JVM for as long as the plugins do not change.
 */
@Slf4j
@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class PluginNativeCompiler {
    /**
     * The path of the augmenter jar inside the {@code quarkus-app} directory of the mutable jar.
     */
    public static final String AUGMENTER = "hmc/headlessmc-plugins-augmenter.jar";
    /**
     * The GraalVM image used with Docker or Podman, its entrypoint is {@code native-image}.
     */
    public static final String DEFAULT_IMAGE = "ghcr.io/graalvm/native-image-community:25";

    private final MutableJarProvider mutableJarProvider;
    private final JavaLauncherService javaLauncherService;
    private final JavaService javaService;
    private final JavaFinder javaFinder;
    private final FileService fileService;
    private final Console console;
    private final OS os;

    /**
     * How {@code native-image} is run.
     */
    public enum Mode {
        /**
         * A local GraalVM if one can be found, otherwise a container.
         */
        AUTO,
        /**
         * A local GraalVM.
         */
        LOCAL,
        /**
         * A container with Docker or Podman, only possible on Linux, because the executable is a Linux executable.
         */
        CONTAINER
    }

    /**
     * @param build the build directory.
     * @return the native executable of the build, it does not need to exist.
     */
    public Path getExecutable(Path build) {
        return build.resolve("native").resolve(OS.Type.WINDOWS.equals(os.type()) ? "headlessmc.exe" : "headlessmc");
    }

    /**
     * Compiles the given build into a native executable.
     *
     * @param build   the build directory, containing quarkus-app with the plugins in its providers directory.
     * @param mode    how to run native-image.
     * @param graalVm the home of the GraalVM to use, or {@code null} to search for one.
     * @param image   the container image to use, or {@code null} for {@value #DEFAULT_IMAGE}.
     * @return the native executable.
     * @throws HeadlessMcException if the compilation fails.
     */
    public Path compile(Path build, Mode mode, @Nullable Path graalVm, @Nullable String image) throws HeadlessMcException {
        NativeImage nativeImage = findNativeImage(mode, graalVm, image);
        Path mutableJar = mutableJarProvider.get();
        Path augmenter = mutableJar.resolve(AUGMENTER);
        if (!Files.isRegularFile(augmenter)) {
            throw new HeadlessMcException("The mutable jar " + mutableJar + " does not contain " + AUGMENTER
                + ", it is too old to compile native executables");
        }

        Path work = build.resolve("native.tmp");
        try {
            fileService.delete(work);
            Path quarkusApp = work.resolve(PluginBuilder.QUARKUS_APP);
            console.write("Generating the native image sources...");
            PluginFiles.copyDirectory(mutableJar, quarkusApp);
            fileService.delete(quarkusApp.resolve("providers"));
            PluginFiles.copyDirectory(build.resolve(PluginBuilder.QUARKUS_APP).resolve("providers"), quarkusApp.resolve("providers"));
            generateSources(quarkusApp, augmenter);

            Path sources = work.resolve("native-sources");
            List<String> args = readArgs(sources.resolve("native-image.args"));
            console.write("Compiling the native executable with " + nativeImage.description()
                + ", this takes a few minutes and a few GB of memory...");
            run(nativeImage.command(sources, args), sources, "native-image");

            Path output = findOutput(sources);
            Path executable = getExecutable(build);
            Files.createDirectories(executable.getParent());
            Files.move(output, executable, StandardCopyOption.REPLACE_EXISTING);
            if (!executable.toFile().setExecutable(true)) {
                log.warn("Failed to make {} executable", executable);
            }

            return executable;
        } catch (IOException e) {
            throw new HeadlessMcException("Failed to compile a native executable in " + work, e);
        } finally {
            try {
                fileService.delete(work);
            } catch (HeadlessMcException e) {
                log.warn("Failed to delete {}", work, e);
            }
        }
    }

    private void generateSources(Path quarkusApp, Path augmenter) throws HeadlessMcException {
        Java java = javaFinder.findJava(PluginBootstrap.MIN_JAVA_VERSION);
        int exitCode = javaLauncherService.buildProcess()
            .id("HeadlessMc native augmentation")
            .java(java)
            .directory(quarkusApp)
            .classpath(quarkusApp.resolve("quarkus-run.jar"), augmenter)
            .mainClass("io.github.headlesshq.headlessmc.plugins.augmenter.NativeAugmenterMain")
            .arg(quarkusApp.toAbsolutePath().toString())
            .start()
            .waitFor((Duration) null);
        if (exitCode != 0) {
            throw new HeadlessMcException("Generating the native image sources failed with exit code " + exitCode);
        }
    }

    private void run(List<String> command, Path directory, String name) throws HeadlessMcException {
        log.debug("Running {}", command);
        try {
            Process process = new ProcessBuilder(command).directory(directory.toFile()).inheritIO().start();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new HeadlessMcException(name + " failed with exit code " + exitCode);
            }
        } catch (IOException e) {
            throw new HeadlessMcException("Failed to run " + command.getFirst(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new HeadlessMcException("Interrupted while running " + name, e);
        }
    }

    private static List<String> readArgs(Path file) throws IOException {
        // written by Quarkus, space separated like scripts/build-native.sh expects
        return Arrays.stream(Files.readString(file, StandardCharsets.UTF_8).trim().split("\\s+"))
            .filter(arg -> !arg.isEmpty())
            .toList();
    }

    private static Path findOutput(Path sources) throws IOException {
        try (Stream<Path> files = Files.list(sources)) {
            return files.filter(path -> {
                    String name = path.getFileName().toString();
                    return name.endsWith("-runner") || name.endsWith("-runner.exe");
                })
                .findFirst()
                .orElseThrow(() -> new HeadlessMcException("native-image did not create an executable in " + sources));
        }
    }

    private NativeImage findNativeImage(Mode mode, @Nullable Path graalVm, @Nullable String image) throws HeadlessMcException {
        if (mode != Mode.CONTAINER) {
            Optional<Path> local = graalVm == null ? findLocalNativeImage() : Optional.of(nativeImageOf(graalVm));
            if (local.isPresent()) {
                if (!Files.isExecutable(local.get())) {
                    throw new HeadlessMcException(local.get() + " does not exist, is " + graalVm + " a GraalVM?");
                }

                Path executable = local.get();
                return new NativeImage("GraalVM " + executable.getParent().getParent(), (sources, args) -> {
                    List<String> command = new ArrayList<>();
                    command.add(executable.toString());
                    command.addAll(args);
                    return command;
                });
            }

            if (mode == Mode.LOCAL) {
                throw new HeadlessMcException("Failed to find a GraalVM 25, set GRAALVM_HOME, use --graalvm=<dir> or --container");
            }
        }

        if (!OS.Type.LINUX.equals(os.type())) {
            throw new HeadlessMcException("Containers build Linux executables, on " + os.type()
                + " a local GraalVM 25 is needed, set GRAALVM_HOME or use --graalvm=<dir>");
        }

        String runtime = findOnPath("docker").or(() -> findOnPath("podman")).map(Path::toString)
            .orElseThrow(() -> new HeadlessMcException("Found neither a GraalVM 25 nor docker or podman to compile a native executable"));
        String containerImage = image == null ? DEFAULT_IMAGE : image;
        String user = isRootless(runtime) ? null : getUser();
        return new NativeImage(containerImage + " (" + Path.of(runtime).getFileName() + ")", (sources, args) -> {
            List<String> command = new ArrayList<>(List.of(runtime, "run", "--rm", "-v", sources.toAbsolutePath() + ":/project:z", "-w", "/project"));
            if (user != null) {
                command.addAll(List.of("--user", user));
            }

            command.add(containerImage);
            command.addAll(args);
            return command;
        });
    }

    /**
     * Looks for native-image in GRAALVM_HOME, JAVA_HOME, the PATH and the Java installations HeadlessMc knows.
     */
    private Optional<Path> findLocalNativeImage() {
        List<Path> candidates = new ArrayList<>();
        Stream.of("GRAALVM_HOME", "JAVA_HOME")
            .map(System::getenv)
            .filter(home -> home != null && !home.isBlank())
            .forEach(home -> candidates.add(nativeImageOf(Path.of(home))));
        findOnPath("native-image").ifPresent(candidates::add);
        javaService.getJavaVersions().stream()
            .filter(java -> java.version() == PluginBootstrap.MIN_JAVA_VERSION)
            .forEach(java -> java.home().getUnchecked().ifPresent(home -> candidates.add(nativeImageOf(home))));
        return candidates.stream().filter(Files::isExecutable).findFirst();
    }

    private Path nativeImageOf(Path home) {
        return home.resolve("bin").resolve(OS.Type.WINDOWS.equals(os.type()) ? "native-image.cmd" : "native-image");
    }

    private Optional<Path> findOnPath(String name) {
        String path = System.getenv("PATH");
        if (path == null) {
            return Optional.empty();
        }

        List<String> names = OS.Type.WINDOWS.equals(os.type()) ? List.of(name + ".exe", name + ".cmd") : List.of(name);
        return Arrays.stream(path.split(File.pathSeparator))
            .filter(dir -> !dir.isBlank())
            .flatMap(dir -> names.stream().map(file -> Path.of(dir).resolve(file)))
            .filter(Files::isExecutable)
            .findFirst();
    }

    /**
     * Rootless Docker and Podman map root in the container to the user running them,
     * so the container must run as root to be able to write to the mounted directory.
     * Rootful Docker runs containers as the real root, so it has to be told the user.
     *
     * @param runtime the docker or podman executable.
     * @return {@code true} if containers run without real root privileges.
     */
    private static boolean isRootless(String runtime) {
        if (Path.of(runtime).getFileName().toString().startsWith("podman")) {
            return true;
        }

        try {
            return output(runtime, "info", "--format", "{{json .SecurityOptions}}").contains("rootless");
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }

            log.debug("Failed to determine if {} is rootless", runtime, e);
            return false;
        }
    }

    /**
     * @return {@code uid:gid}, so that files created in the mounted directory belong to the user, or {@code null}.
     */
    private static @Nullable String getUser() {
        try {
            return output("id", "-u") + ":" + output("id", "-g");
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }

            return null;
        }
    }

    private static String output(String... command) throws IOException, InterruptedException {
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
        if (process.waitFor() != 0 || output.isEmpty()) {
            throw new IOException(String.join(" ", command) + " failed: " + output);
        }

        return output;
    }

    private record NativeImage(String description, CommandFactory factory) {
        List<String> command(Path sources, List<String> args) {
            return factory.create(sources, args);
        }
    }

    @FunctionalInterface
    private interface CommandFactory {
        List<String> create(Path sources, List<String> args);
    }

}
