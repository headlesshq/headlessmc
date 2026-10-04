package io.github.headlesshq.headlessmc.launcher.libraries;

import io.github.headlesshq.headlessmc.files.AppFiles;
import io.github.headlesshq.headlessmc.net.hash.HashService;
import io.github.headlesshq.headlessmc.util.maven.Artifact;
import io.github.headlesshq.headlessmc.version.Version;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class NativeLibraryHandler {
    private final LibraryExtractor extractor;
    private final HashService hashService;
    private final AppFiles appFiles;

    public Path handleNativeLibraries(List<LibraryFile> libraryFiles) {
        // TODO: it would be nice if this is okay, and how mc calculates .minecraft/bin/ filenames
        MessageDigest digest = hashService.getAlgorithm(HashService.SHA256);
        List<LibraryFile> nativeLibraries = new ArrayList<>();
        for (LibraryFile libraryFile : libraryFiles) {
            if (isNativeLibrary(libraryFile)) {
                if (libraryFile.download() != null && libraryFile.download().getSha1() != null) {
                    digest.update(libraryFile.download().getSha1().getBytes());
                }

                digest.update(libraryFile.library().getName().getBytes(StandardCharsets.UTF_8));
                nativeLibraries.add(libraryFile);
            }
        }

        String hexString = hashService.toHexString(digest.digest());
        Path dir = appFiles.getNatives().resolve(hexString.substring(0, 64));
        for (LibraryFile nativeLibrary : nativeLibraries) {
            Version.Library library = nativeLibrary.library();
            Version.Extract extract = library.getExtract();
            if (extract != null) {
                extractor.extract(library, extract, nativeLibrary.path(), dir);
            } // on newer versions of mc this is done by LWJGL itself
        }

        return dir;
    }

    private boolean isNativeLibrary(LibraryFile libraryFile) {
        Artifact artifact = libraryFile.library().getArtifact();
        String classifier = artifact.classifier() == null ? "" : artifact.classifier().toLowerCase(Locale.ENGLISH);
        return libraryFile.library().getExtract() != null
            || classifier.startsWith("natives");
    }

}
