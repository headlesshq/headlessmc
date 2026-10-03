package io.github.headlesshq.headlessmc.launcher.libraries;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.files.FileService;
import io.github.headlesshq.headlessmc.files.McFiles;
import io.github.headlesshq.headlessmc.os.CPU;
import io.github.headlesshq.headlessmc.os.OS;
import io.github.headlesshq.headlessmc.util.maven.Artifact;
import io.github.headlesshq.headlessmc.version.Version;
import io.github.headlesshq.headlessmc.version.util.Features;
import io.github.headlesshq.headlessmc.version.util.Rules;
import io.github.headlesshq.headlessmc.version.util.TemplateStrings;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class LibraryResolver {
    /**
     * <a href=https://stackoverflow.com/questions/2438800/what-is-the-smallest-legal-zip-jar-file>
     * https://stackoverflow.com/questions/2438800/what-is-the-smallest-legal-zip-jar-file
     * </a>
     * E.g. the artifact download for lwjgl-platform in 1.9.
     * Version.jsons may specify an artifact of this size if natives
     * are also available.
     */
    private static final Long EMPTY_JAR_SIZE = 22L;

    private final NativeLibraryResolver nativeLibraryResolver;
    private final FileService fileService;
    private final McFiles mcFiles;
    private final CPU cpu;
    private final OS os;

    public List<LibraryFile> resolveLibraries(
        TemplateStrings templateStrings,
        Features features,
        Version version
    ) throws HeadlessMcException {
        List<LibraryFile> files = new ArrayList<>(version.getLibraries().size());
        for (Version.Library library : version.getLibraries()) {
            Artifact artifact = Artifact.of(library.getName());
            Path path = artifact.jar(mcFiles.getLibraryDir());

            Rules rules = Rules.of(library.getRules());
            if (rules.disallow(cpu, os, features)) {
                log.debug("Skipping library due to rules: {}, {}, {}, {}, {}", cpu, os, rules, features, library);
                continue;
            }

            Version.LibraryDownloads downloads = library.getDownloads();
            if (downloads == null) {
                files.add(new LibraryFile(library, path, null));
                continue;
            }

            Map<String, Version.Download> classifiers = downloads.getClassifiers();
            if (classifiers != null) {
                files.add(nativeLibraryResolver.handleNatives(library, templateStrings, classifiers, version));
            }

            Version.Download artifactDownload = downloads.getArtifact();
            if (artifactDownload != null) {
                if (EMPTY_JAR_SIZE.equals(artifactDownload.getSize())) {
                    continue;
                }

                String downloadPath = artifactDownload.getPath();
                if (downloadPath != null) {
                    String[] split = downloadPath.split("/");
                    Path resolvedDownloadPath = fileService.getPath(mcFiles.getLibraryDir(), split);
                    if (!path.equals(resolvedDownloadPath)) {
                        log.warn("Library download path is not artifact path: {} - {}", path, resolvedDownloadPath);
                    }

                    path = resolvedDownloadPath;
                }

                files.add(new LibraryFile(library, path, artifactDownload));
            }
        }

        return files;
    }

}
