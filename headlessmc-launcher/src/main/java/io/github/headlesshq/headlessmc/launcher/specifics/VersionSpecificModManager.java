package io.github.headlesshq.headlessmc.launcher.specifics;

import lombok.CustomLog;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import io.github.headlesshq.headlessmc.api.HasName;
import io.github.headlesshq.headlessmc.launcher.download.DownloadService;
import io.github.headlesshq.headlessmc.launcher.files.FileManager;
import io.github.headlesshq.headlessmc.launcher.files.LauncherConfig;
import io.github.headlesshq.headlessmc.launcher.util.IOUtil;
import io.github.headlesshq.headlessmc.launcher.version.Version;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Manages multiple {@link VersionSpecificModRepository}s.
 * Specifically, downloads mods for mc versions, caches them, installs them and deletes them.
 */
// TODO: publish checksums with release action and verify that the mods have the correct one
@Getter
@CustomLog
@RequiredArgsConstructor
public class VersionSpecificModManager {
    private final List<VersionSpecificModRepository> specificMods = new ArrayList<>();
    private final DownloadService downloadService;
    private final LauncherConfig directories;

    public VersionSpecificModRepository getRepository(String name) throws VersionSpecificException {
        VersionSpecificModRepository repository = HasName.getByName(name, specificMods);
        if (repository == null) {
            throw new VersionSpecificException("Failed to find version specific mod " + name);
        }

        return repository;
    }

    public void download(Version version, VersionSpecificModRepository repository) throws VersionSpecificException, IOException {
        VersionInfo info = VersionInfo.requireModLauncher(version);
        File file = getFileManager().createRelative(repository.getName()).get(false, false, repository.getFileName(repository.getVersion(downloadService), info));
        if (file.exists()) {
            return;
        }

        URL url = repository.getDownloadURL(repository.getVersion(downloadService), info);
        log.info("Downloading " + file.getName() + " from " + url);
        downloadService.download(url.toString(), file.toPath());
    }

    public void install(Version version, VersionSpecificModRepository repository, Path modsFolder) throws VersionSpecificException, IOException {
        VersionInfo info = VersionInfo.requireModLauncher(version);
        File file = getFileManager().createRelative(repository.getName()).get(false, false, repository.getFileName(repository.getVersion(downloadService), info));
        if (!file.exists()) {
            throw new VersionSpecificException("Failed to find " + repository.getName() + " for version " + info.getDescription());
        }

        Path outputFile = modsFolder.resolve(repository.getFileName(repository.getVersion(downloadService), info));
        if (!Files.exists(outputFile)) {
            Files.createDirectories(outputFile.getParent());
            try (InputStream is = Files.newInputStream(file.toPath()); OutputStream os = Files.newOutputStream(outputFile)) {
                IOUtil.copy(is, os);
            }
        }
    }

    public void deleteSpecificsOfOtherVersions(Version version, VersionSpecificModRepository repository, Path modsFolder) throws VersionSpecificException, IOException {
        VersionInfo info = VersionInfo.requireModLauncher(version);
        String expectedFileName = repository.getFileName(repository.getVersion(downloadService), info);
        Pattern pattern = repository.getFileNamePattern();
        if (Files.exists(modsFolder)) {
            try (Stream<Path> stream = Files.list(modsFolder)) {
                Iterator<Path> itr = stream.iterator();
                while (itr.hasNext()) {
                    Path modFile = itr.next();
                    String modFilename = modFile.getFileName().toString();
                    if (pattern.matcher(modFilename).find() && !expectedFileName.equals(modFilename)) {
                        log.info("Deleting outdated mod " + modFilename);
                        Files.delete(modFile);
                    }
                }
            }
        }
    }

    public void deleteAll(Path modsFolder) throws IOException {
        if (Files.exists(modsFolder)) {
            List<Pattern> patterns = specificMods.stream().map(VersionSpecificModRepository::getFileNamePattern).collect(Collectors.toList());
            try (Stream<Path> stream = Files.list(modsFolder)) {
                Iterator<Path> itr = stream.iterator();
                while (itr.hasNext()) {
                    Path modFile = itr.next();
                    String modFilename = modFile.getFileName().toString();
                    if (patterns.stream().map(p -> p.matcher(modFilename)).anyMatch(Matcher::find)) {
                        log.info("Deleting outdated mod " + modFilename);
                        Files.delete(modFile);
                    }
                }
            }
        }
    }

    public void addRepository(VersionSpecificModRepository repository) {
        specificMods.add(repository);
    }

    public void removeRepository(VersionSpecificModRepository repository) {
        specificMods.remove(repository);
    }

    public FileManager getFileManager() {
        return directories.getFileManager().createRelative("specifics");
    }

}
