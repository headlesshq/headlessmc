package io.github.headlesshq.headlessmc.java.service;

import io.github.headlesshq.headlessmc.api.settings.Config;
import io.github.headlesshq.headlessmc.commons.LazyService;
import io.github.headlesshq.headlessmc.commons.files.FileManager;
import io.github.headlesshq.headlessmc.commons.files.HeadlessMcDir;
import io.github.headlesshq.headlessmc.commons.files.PathUtil;
import io.github.headlesshq.headlessmc.java.*;
import io.github.headlesshq.headlessmc.java.distribution.JavaDistributionService;
import io.github.headlesshq.headlessmc.java.distribution.JavaDownloadRequest;
import io.github.headlesshq.headlessmc.os.OS;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.CustomLog;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import java.io.IOError;
import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@CustomLog
@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class JavaService extends LazyService<Java> implements JavaScanner {
    private final Object lock = new Object();

    @Getter
    private final FilePermissionJavaParser parser;
    private final @HeadlessMcDir FileManager fileManager;
    private final JavaDistributionService distributionService;
    private final JavaSettings settings;
    private final Config cfg;
    private final OS os;

    private volatile @Nullable Java current;

    @Override
    protected Set<Java> update() {
        long nanos = System.nanoTime();
        List<String> versions = cfg.get(settings.versions());
        Set<Java> newVersions = new LinkedHashSet<>((int) (versions.size() * 1.5));
        for (String path : versions) {
            Java java = scanJava(path);
            if (java != null) {
                newVersions.add(java);
            }
        }

        JavaScanner javaScanner = JavaScanner.of(parser);
        JavaVersionFinder javaVersionFinder = new JavaVersionFinder();
        newVersions.addAll(javaVersionFinder.checkDirectory(javaScanner, fileManager.get("java"), os));

        String javaHome = System.getenv("JAVA_HOME");
        if (javaHome != null) {
            try {
                // TODO: java.exe on windows?!?!?!?!!
                Java java = scanJava(PathUtil.stripQuotes(javaHome).resolve("bin").resolve("java").toAbsolutePath().toString());
                if (java != null) {
                    newVersions.add(java);
                }
            } catch (InvalidPathException e) {
                log.error(e);
            }
        }

        Java current = getCurrent();
        if (current != null && !current.isInvalid() && cfg.get(settings.useCurrentJava())) {
            newVersions.add(current);
        }

        nanos = System.nanoTime() - nanos;
        log.debug("Java refresh took " + (nanos / 1_000_000.0) + "ms.");
        return newVersions;
    }

    public void refreshHeadlessMcJavaVersions() {
        Set<Java> versions = new HashSet<>(contents);
        boolean addFilePermissions = os.getType() == OS.Type.LINUX || os.getType() == OS.Type.OSX;
        JavaScanner javaScanner = JavaScanner.of(new JavaVersionParser(addFilePermissions));
        JavaVersionFinder javaVersionFinder = new JavaVersionFinder();
        versions.addAll(javaVersionFinder.checkDirectory(javaScanner, fileManager.get("java"), os, versions));
        contents = versions;
    }

    public @Nullable Java scanJava(String path) {
        log.debug("Reading Java version at path: " + path);
        if (path.trim().isEmpty()) {
            return null;
        }

        try {
            int majorVersion = parser.parseVersionCommand(path);
            Java java = new Java(path.replace("\\", "/"), majorVersion);
            log.debug("Found Java: " + java);
            return java;
        } catch (IOException e) {
            log.warn("Couldn't parse Java Version for path " + path, e);
        }

        return null;
    }

    public @Nullable Java findBestVersion(@Nullable Integer version) {
        return findBestVersion(version, false);
    }

    public @Nullable Java findBestVersion(@Nullable Integer version, boolean canFallbackToOtherVersion) {
        ensureInitialized();
        if (version == null) {
            log.error("Version was null, assuming Java 8 is needed!");
            return findBestVersion(8, canFallbackToOtherVersion);
        }

        Java best = null;
        for (Java java : this) {
            if ("current".equals(java.getPath())) {
                continue;
            }

            if (version == java.getVersion()) {
                return java;
            }

            if (java.getVersion() > version // find the one thats closest to the wanted version
                    && (best == null || best.getVersion() > java.getVersion())) {
                best = java;
            }
        }

        if (!canFallbackToOtherVersion && cfg.get(settings.autoDownload())) {
            Java java = download(version);
            if (java != null) {
                return java;
            }
        }

        if (cfg.get(settings.requireExactJava())) { // TODO: false for legacy reasons
            return null;
        }

        if (best == null) {
            log.error("Couldn't find a Java Version >= " + version + "!");
        } else {
            // this is kinda dangerous, running mc with a higher java version?!
            log.warning("Couldn't find Java Version " + version
                            + " falling back to " + best.getVersion());
        }

        return best;
    }

    public @Nullable Java getCurrent() {
        if (current == null) {
            synchronized (lock) {
                if (current == null) {
                    String javaHome = System.getProperty("java.home");
                    boolean javaHomeNull = false;
                    if (javaHome == null) {
                        javaHome = "current";
                        javaHomeNull = true;
                    }

                    String executable = PathUtil.stripQuotesAtStartAndEnd(javaHome).replace("\"", "").concat("/bin/java");
                    String version = System.getProperty("java.version");
                    if (version == null) {
                        if (javaHomeNull) {
                            throw new IllegalStateException("Failed to parse current Java version!");
                        }

                        current = scanJava(executable);
                    } else {
                        if (!javaHomeNull) {
                            current = scanJava(executable);
                        }

                        if (current == null) {
                            current = new Java(executable, parseSystemProperty(version), true);
                        }
                    }
                }
            }
        }

        return current;
    }

    @VisibleForTesting
    int parseSystemProperty(String versionIn) {
        String version = versionIn;
        if (version.startsWith("1.")) {
            version = version.substring(2, 3);
        } else {
            int dot = version.indexOf(".");
            if (dot != -1) {
                version = version.substring(0, dot);
            }

            int hyphen = version.indexOf("-"); // 21-internal
            if (hyphen != -1) {
                version = version.substring(0, hyphen);
            }
        }

        return Integer.parseInt(version);
    }

    private @Nullable Java download(int version) {
        JavaDownloadRequest javaDownloadRequest = new JavaDownloadRequest(
                version,
                cfg.get(settings.distribution()),
                os,
                false
        );

        try {
            distributionService.download(fileManager.resolve("java"), javaDownloadRequest);
            refreshHeadlessMcJavaVersions();
            Java java = contents.stream().filter(j -> j.getVersion() == version).findFirst().orElse(null);
            if (java == null) {
                throw new IOException("Failed to download Java version " + version);
            }

            return java;
        } catch (IOException e) {
            if (cfg.get(settings.autoDownloadThrowExceptions())) {
                throw new IOError(e);
            }

            log.error("Failed to download Java " + version, e);
        }

        return null;
    }

}
