package io.github.headlesshq.headlessmc.launcher.libraries;

import io.github.headlesshq.headlessmc.files.McFiles;
import io.github.headlesshq.headlessmc.util.maven.MavenRepository;
import io.github.headlesshq.headlessmc.version.Version;
import io.github.headlesshq.headlessmc.version.util.Features;
import io.github.headlesshq.headlessmc.version.util.TemplateString;
import io.github.headlesshq.headlessmc.version.util.TemplateStrings;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class ClasspathService {
    private final LibraryDownloader libraryDownloader;
    private final LibraryResolver libraryResolver;
    private final NativeLibraryHandler nativeLibraryHandler;
    private final McJarDownloader mcJarDownloader;

    /**
     * Resolves and downloads everything on the classpath of a version.
     *
     * @param mcFiles      the {@code .minecraft} directory of the version, libraries and the jar are placed in it.
     * @param templates    the templates to resolve libraries with, the natives directory is added to them.
     * @param features     the features to check library rules against.
     * @param version      the version to build the classpath for.
     * @param repositories the repositories to download libraries without a download url from.
     * @return the classpath of the version.
     */
    public List<Path> buildClasspath(
        McFiles mcFiles,
        TemplateStrings templates,
        Features features,
        Version version,
        List<MavenRepository> repositories
    ) {
        List<LibraryFile> libraryFiles = libraryResolver.resolveLibraries(mcFiles, templates, features, version);
        libraryDownloader.download(repositories, libraryFiles);

        Path nativeDir = nativeLibraryHandler.handleNativeLibraries(libraryFiles);
        templates.add(TemplateString.NATIVES_DIRECTORY, nativeDir.toAbsolutePath().toString());

        Path mcJar = mcJarDownloader.downloadMcJar(mcFiles, Version.DOWNLOAD_CLIENT, version);

        List<Path> classpath = new ArrayList<>(libraryFiles.size() + 1);
        libraryFiles.stream().map(LibraryFile::path).forEach(classpath::add);
        classpath.add(mcJar);
        return classpath;
    }

}
