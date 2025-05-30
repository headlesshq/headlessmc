package io.github.headlesshq.headlessmc.launcher.java;

import io.github.headlesshq.headlessmc.api.settings.Config;
import lombok.CustomLog;
import lombok.Getter;
import io.github.headlesshq.headlessmc.java.Java;
import io.github.headlesshq.headlessmc.java.JavaScanner;
import io.github.headlesshq.headlessmc.java.JavaVersionFinder;
import io.github.headlesshq.headlessmc.java.JavaVersionParser;
import io.github.headlesshq.headlessmc.java.download.JavaDownloadRequest;
import io.github.headlesshq.headlessmc.java.download.JavaDownloaderManager;
import io.github.headlesshq.headlessmc.launcher.Launcher;
import io.github.headlesshq.headlessmc.launcher.LazyService;
import io.github.headlesshq.headlessmc.launcher.util.PathUtil;
import io.github.headlesshq.headlessmc.os.OS;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import java.io.IOError;
import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

@CustomLog
public class JavaService extends LazyService<Java> implements JavaScanner {
    private final Object lock = new Object();
    @Getter
    private final JavaVersionParser parser;
    private final Config cfg;
    private final OS os;

    private volatile Java current;

    public JavaService(Config cfg, OS os) {
        this.cfg = cfg;
        this.os = os;
        boolean addFilePermissions = os.getType() == OS.Type.LINUX || os.getType() == OS.Type.OSX;
        addFilePermissions &= cfg.get(LauncherProperties.JAVA_ALWAYS_ADD_FILE_PERMISSIONS, false);
        this.parser = new JavaVersionParser(addFilePermissions);
    }

    @Override
    protected Set<Java> update() {
        long nanos = System.nanoTime();
        String[] array = cfg.getConfig().get(LauncherProperties.JAVA, new String[0]);
        Set<Java> newVersions = new LinkedHashSet<>((int) (array.length * 1.5));
        for (String path : array) {
            Java java = scanJava(path);
            if (java != null) {
                newVersions.add(java);
            }
        }

        JavaScanner javaScanner = JavaScanner.of(parser);
        JavaVersionFinder javaVersionFinder = new JavaVersionFinder();
        newVersions.addAll(javaVersionFinder.checkDirectory(javaScanner, cfg.getFileManager().getDir("java").toPath(), os));

        if (System.getenv("JAVA_HOME") != null) {
            try {
                Java java = scanJava(PathUtil.stripQuotes(System.getenv("JAVA_HOME")).resolve("bin").resolve("java").toAbsolutePath().toString());
                if (java != null) {
                    newVersions.add(java);
                }
            } catch (InvalidPathException e) {
                log.error(e);
            }
        }

        Java current = getCurrent();
        if (current != null && !current.isInvalid() && cfg.getConfig().get(LauncherProperties.USE_CURRENT_JAVA, true)) {
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
        versions.addAll(javaVersionFinder.checkDirectory(javaScanner, cfg.getFileManager().getDir("java").toPath(), os, versions));
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

    public @Nullable Java findBestVersion(Integer version) {
        return findBestVersion(null, version);
    }

    public @Nullable Java findBestVersion(@Nullable Launcher launcher, Integer version) {
        return findBestVersion(launcher, version, false);
    }

    public @Nullable Java findBestVersion(@Nullable Launcher launcher, Integer version, boolean canFallbackToOtherVersion) {
        ensureInitialized();
        if (version == null) {
            log.error("Version was null, assuming Java 8 is needed!");
            return findBestVersion(launcher, 8, canFallbackToOtherVersion);
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

        if (!canFallbackToOtherVersion && launcher != null && cfg.getConfig().get(LauncherProperties.AUTO_DOWNLOAD_JAVA, true)) {
            Java java = download(launcher, version);
            if (java != null) {
                return java;
            }
        }

        if (cfg.getConfig().get(LauncherProperties.REQUIRE_EXACT_JAVA, false)) { // TODO: false for legacy reasons
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

    private @Nullable Java download(Launcher launcher, int version) {
        JavaDownloadRequest javaDownloadRequest = new JavaDownloadRequest(
                launcher.getDownloadService(),
                launcher.getCommandLine(),
                version,
                cfg.getConfig().get(LauncherProperties.JAVA_DISTRIBUTION, JavaDownloaderManager.DEFAULT_DISTRIBUTION),
                launcher.getProcessFactory().getOs(),
                false
        );

        try {
            launcher.getJavaDownloaderManager().download(launcher.getFileManager().getBase().toPath().resolve("java"), javaDownloadRequest);
            refreshHeadlessMcJavaVersions();
            Java java = contents.stream().filter(j -> j.getVersion() == version).findFirst().orElse(null);
            if (java == null) {
                throw new IOException("Failed to download Java version " + version);
            }

            return java;
        } catch (IOException e) {
            if (cfg.getConfig().get(LauncherProperties.AUTO_DOWNLOAD_JAVA_THROW_EXCEPTION, true)) {
                throw new IOError(e);
            }

            log.error("Failed to download Java " + version, e);
        }

        return null;
    }

}
