package io.github.headlesshq.headlessmc.java.sources;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.java.Java;
import io.github.headlesshq.headlessmc.java.JavaSource;
import io.github.headlesshq.headlessmc.os.OS;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Finds the Java installations of a Linux system, in {@code /usr/lib/jvm} and {@code /usr/local},
 * the Java installed into the profiles of Nix and Guix, e.g. on NixOS, where they live in the store instead,
 * and the Java in {@code JAVA_HOME} and on the {@code PATH}, e.g. from {@code nix shell}, a dev shell or direnv.
 */
@Slf4j
@ApplicationScoped
@Named("java:source:linux")
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class LinuxJavaSource implements JavaSource {
    private final JavaScannerService javaScannerService;
    private final OS os;

    @Override
    public String getName() {
        return OS.Type.LINUX.name();
    }

    @Override
    public List<Java> getJavas() {
        if (!OS.McType.LINUX.equals(os.type().mcType())) {
            return List.of();
        }

        List<Java> result = new ArrayList<>();
        Iterable<Path> rootDirectories = FileSystems.getDefault().getRootDirectories();
        for (Path rootPath : rootDirectories) {
            result.addAll(javaScannerService.scanSubDirsOf(this, rootPath.resolve("usr").resolve("lib").resolve("jvm")));
            result.addAll(javaScannerService.scanSubDirsOf(this, rootPath.resolve("usr").resolve("local")));
        }

        List<Path> executables = new ArrayList<>();
        for (Path profile : nixAndGuixProfiles()) {
            executables.add(profile.resolve("bin").resolve("java"));
        }

        executables.addAll(environmentExecutables(System.getenv("JAVA_HOME"), System.getenv("PATH")));
        result.addAll(scanExecutables(executables, realHomes(result)));
        return result;
    }

    @Override
    public int sort() {
        return JavaSource.SORT_GENERAL;
    }

    /**
     * Scans Java executables, e.g. the {@code bin/java} of Nix or Guix profiles or a {@code java} on the {@code PATH}.
     * An executable is usually a symlink, e.g. into the Nix store, it is resolved to find the actual Java home,
     * e.g. {@code /nix/store/...-openjdk-21/lib/openjdk}.
     * A Java found via multiple executables is only returned once.
     *
     * @param executables the Java executables to scan, missing ones are skipped.
     * @param knownHomes  the real paths of Java homes that have already been found, these are skipped.
     * @return the Java installations found for the given executables.
     */
    List<Java> scanExecutables(List<Path> executables, Set<Path> knownHomes) {
        Set<Path> homes = new LinkedHashSet<>();
        for (Path executable : executables) {
            if (!Files.isRegularFile(executable)) {
                continue;
            }

            try {
                Path bin = executable.toRealPath().getParent();
                Path home = bin == null ? null : bin.getParent();
                if (home != null && !knownHomes.contains(home)) {
                    homes.add(home);
                }
            } catch (IOException | SecurityException e) {
                log.debug("Failed to resolve {}", executable, e);
            }
        }

        List<Java> result = new ArrayList<>(homes.size());
        for (Path home : homes) {
            try {
                javaScannerService.scanDir(this, home).ifPresent(result::add);
            } catch (HeadlessMcException e) {
                log.error("Failed to scan java dir {}", home, e);
            }
        }

        return result;
    }

    /**
     * Returns the Java executables of the {@code JAVA_HOME} and of every directory on the {@code PATH}.
     *
     * @param javaHome the value of the {@code JAVA_HOME} environment variable.
     * @param path     the value of the {@code PATH} environment variable.
     * @return the possible Java executables, they do not necessarily exist.
     */
    static List<Path> environmentExecutables(@Nullable String javaHome, @Nullable String path) {
        List<Path> executables = new ArrayList<>();
        if (javaHome != null && !javaHome.isBlank()) {
            addExecutable(executables, javaHome, "bin", "java");
        }

        if (path != null) {
            for (String dir : path.split(File.pathSeparator)) {
                if (!dir.isBlank()) {
                    addExecutable(executables, dir, "java");
                }
            }
        }

        return executables;
    }

    private static void addExecutable(List<Path> executables, String first, String... more) {
        try {
            executables.add(Path.of(first, more));
        } catch (InvalidPathException e) {
            log.debug("Invalid path {}", first, e);
        }
    }

    private static Set<Path> realHomes(List<Java> javas) {
        Set<Path> homes = new HashSet<>();
        for (Java java : javas) {
            java.home().getUnchecked().ifPresent(home -> {
                try {
                    homes.add(home.toRealPath());
                } catch (IOException | SecurityException e) {
                    log.debug("Failed to resolve {}", home, e);
                }
            });
        }

        return homes;
    }

    /**
     * Returns the system, per-user and user profiles of Nix and Guix, e.g. on NixOS or Guix System.
     *
     * @return the possible profile directories, they do not necessarily exist.
     */
    private static List<Path> nixAndGuixProfiles() {
        List<Path> profiles = new ArrayList<>();
        profiles.add(Path.of("/run/current-system/sw"));
        profiles.add(Path.of("/run/current-system/profile"));
        String user = System.getProperty("user.name");
        if (user != null && !user.isBlank()) {
            profiles.add(Path.of("/etc/profiles/per-user", user));
        }

        String home = System.getProperty("user.home");
        if (home != null && !home.isBlank()) {
            Path userHome = Path.of(home);
            profiles.add(userHome.resolve(".nix-profile"));
            profiles.add(userHome.resolve(".local").resolve("state").resolve("nix").resolve("profile"));
            profiles.add(userHome.resolve(".guix-profile"));
            profiles.add(userHome.resolve(".guix-home").resolve("profile"));
        }

        return profiles;
    }

}
