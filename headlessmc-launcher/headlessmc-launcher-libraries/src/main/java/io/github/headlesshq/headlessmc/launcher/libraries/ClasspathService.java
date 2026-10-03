package io.github.headlesshq.headlessmc.launcher.libraries;

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

    public List<Path> buildClasspath(
        TemplateStrings templates,
        Features features,
        Version version,
        List<MavenRepository> repositories
    ) {
        List<LibraryFile> libraryFiles = libraryResolver.resolveLibraries(templates, features, version);
        libraryDownloader.download(repositories, libraryFiles);

        Path nativeDir = nativeLibraryHandler.handleNativeLibraries(libraryFiles);
        templates.add(TemplateString.NATIVES_DIRECTORY, nativeDir.toAbsolutePath().toString());

        Path mcJar = mcJarDownloader.downloadMcJar(Version.DOWNLOAD_CLIENT, version);

        List<Path> classpath = new ArrayList<>(libraryFiles.size() + 1);
        libraryFiles.stream().map(LibraryFile::path).forEach(classpath::add);
        classpath.add(mcJar);
        return classpath;
    }

}
