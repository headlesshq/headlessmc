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

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Finds the Java installations of a Linux system, in {@code /usr/lib/jvm} and {@code /usr/local},
 * and the Java installed into the profiles of Nix and Guix, e.g. on NixOS, where they live in the store instead.
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

        result.addAll(scanProfiles(profiles()));
        return result;
    }

    @Override
    public int sort() {
        return JavaSource.SORT_GENERAL;
    }

    /**
     * Scans the {@code bin/java} of Nix or Guix profiles.
     * The executable in a profile is a symlink into the store, it is resolved to find the actual Java home,
     * e.g. {@code /nix/store/...-openjdk-21/lib/openjdk}.
     * A Java installed into multiple profiles is only returned once.
     *
     * @param profiles the profile directories to scan.
     * @return the Java installations found in the given profiles.
     */
    List<Java> scanProfiles(List<Path> profiles) {
        Set<Path> homes = new LinkedHashSet<>();
        for (Path profile : profiles) {
            Path executable = profile.resolve("bin").resolve("java");
            if (!Files.isRegularFile(executable)) {
                continue;
            }

            try {
                Path bin = executable.toRealPath().getParent();
                Path home = bin == null ? null : bin.getParent();
                if (home != null) {
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

    private static List<Path> profiles() {
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
