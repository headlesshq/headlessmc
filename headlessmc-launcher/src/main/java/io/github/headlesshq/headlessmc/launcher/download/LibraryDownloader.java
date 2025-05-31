package io.github.headlesshq.headlessmc.launcher.download;

import io.github.headlesshq.headlessmc.api.settings.Config;
import io.github.headlesshq.headlessmc.launcher.settings.LauncherSettings;
import io.github.headlesshq.headlessmc.launcher.settings.LibrarySettings;
import io.github.headlesshq.headlessmc.launcher.version.Library;
import io.github.headlesshq.headlessmc.os.OS;
import jakarta.inject.Inject;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;

@CustomLog
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class LibraryDownloader {
    private static final URI MAVEN_CENTRAL = URI.create("https://repo1.maven.org/maven2/");

    private final LibrarySettings librarySettings;
    private final DownloadService downloadService;
    private final LauncherSettings settings;
    private final Config config;
    private final OS os;

    @Setter
    private boolean shouldLog = true;

    public void download(Library library, Path to) throws IOException {
        String libPath = library.getPath(os);
        if (fixArmLibrary(library, libPath, to)) {
            return;
        }

        URI url = library.getUrl(libPath);
        if (shouldLog) {
            log.info(libPath + " is missing, downloading from " + url);
        }

        download(url, to, library.getSha1(), library.getSize());
    }

    private boolean fixArmLibrary(Library library, String libPathIn, Path to) {
        if (os.isArm()
                && os.getType() == OS.Type.LINUX
                && library.isOrContainsNatives(os)
                && config.get(settings.fixArmLibraries())) {
            String libPath = libPathIn.replace(File.separatorChar, '/');
            if (libPath.contains("com/mojang/jtracy/")) {
                return false; // they do not have other natives
            }

            String fixedLibPath = libPath
                    .replace("x64", "arm64")
                    .replace("x86", "arm32");

            if (fixedLibPath.endsWith("-natives-linux.jar")) {
                fixedLibPath = fixedLibPath
                        .substring(0, fixedLibPath.length() - 4)
                        .concat(os.is64bit() ? "-arm64.jar" : "-arm32.jar");
            }

            if (!fixedLibPath.equals(libPath)) {
                try {
                    log.debug("Fixing library " + libPath + " to " + fixedLibPath);
                    download(URI.create(MAVEN_CENTRAL + fixedLibPath), to, null, null);
                    return true;
                } catch (IOException e) {
                    log.error("Failed to fix library " + fixedLibPath, e);
                }
            }
        }

        return false;
    }

    public void download(URI url, Path to, @Nullable String hash, @Nullable Long size) throws IOException {
        boolean checkHash = config.get(librarySettings.checkHash());
        boolean checkSize = checkHash || config.get(librarySettings.checkSize());
        Long expectedSize = checkSize ? size : null;
        String expectedHash = checkHash ? hash : null;
        downloadService.download(url, to, expectedSize, expectedHash);
    }

}
