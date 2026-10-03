package io.github.headlesshq.headlessmc.launcher.libraries;

import io.github.headlesshq.headlessmc.files.FileService;
import io.github.headlesshq.headlessmc.files.McFiles;
import io.github.headlesshq.headlessmc.os.OS;
import io.github.headlesshq.headlessmc.version.Version;
import io.github.headlesshq.headlessmc.version.util.TemplateStrings;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;

import java.nio.file.Path;
import java.util.Map;

@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class NativeLibraryResolver {
    private final FileService fileService;
    private final McFiles mcFiles;
    private final OS os;

    public LibraryFile handleNatives(
        Version.Library library,
        TemplateStrings templateStrings,
        Map<String, Version.Download> classifiers,
        Version version
    ) {
        String classifier = getClassifier(library, classifiers, version);
        // e.g. in 1.7.5: "windows": "natives-windows-${arch}"
        classifier = templateStrings.process(classifier);
        Version.Download download = classifiers.get(classifier);
        if (download == null) {
            throw new LibraryException(
                "Failed to to find download classifier %s in %s, library: %s, version: %s".formatted(
                    classifier,
                    classifiers,
                    library,
                    version
                ));
        }

        String libraryPath = download.getPath();
        if (libraryPath == null) {
            throw new LibraryException(
                "Failed to get library path for native library %s (%s), in library %s, version %s".formatted(
                    download,
                    classifier,
                    library,
                    version
                )
            );
        }

        String[] split = libraryPath.split("/");
        Path resolvedLibraryPath = fileService.getPath(mcFiles.getLibraryDir(), split);
        return new LibraryFile(library, resolvedLibraryPath, download);
    }

    private String getClassifier(
        Version.Library library,
        Map<String, Version.Download> classifiers,
        Version version
    ) {
        Map<String, String> natives = library.getNatives();
        if (natives == null) {
            throw new LibraryException(
                "Failed to to find natives entry for classifiers %s, library: %s, version: %s".formatted(
                    classifiers,
                    library,
                    version
                ));
        }

        String classifier = natives.get(os.type().mcType().name());
        if (classifier == null) {
            throw new LibraryException(
                "Failed to to find natives entry for os %s in %s, library: %s, version: %s".formatted(
                    os,
                    natives,
                    library,
                    version
                ));
        }

        return classifier;
    }
    
}
