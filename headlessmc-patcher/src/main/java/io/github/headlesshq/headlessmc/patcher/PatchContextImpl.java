package io.github.headlesshq.headlessmc.patcher;

import io.github.headlesshq.headlessmc.files.FileService;
import io.github.headlesshq.headlessmc.files.McFiles;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Supplier;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.stream.Stream;

@Data
@AllArgsConstructor
@RequiredArgsConstructor(access = AccessLevel.NONE)
final class PatchContextImpl implements PatchContext {
    // maps a patched library to relative path
    // cache/patches/hash/patcher/path/to/library -> /path/to/library
    private final Map<Path, Path> relativePaths = new HashMap<>();

    private final Supplier<Stream<HelperService>> services;
    private final PatchResult initialPatchResult;
    private final FileService fileService;
    private final PatchCache.Key cacheKey;
    private final List<Patcher> patchers;
    private final McFiles mcFiles;
    private final int javaVersion;
    private final Path baseDir;
    private PatchResult currentPatchResult;

    @Override
    public OutputStream add(String library, Patcher patcher) throws IOException {
        Path patcherDir = baseDir.resolve(patcher.name());
        Files.createDirectories(patcherDir);
        Path libraryFile = patcherDir.resolve(library + ".jar");
        return new ClasspathOutputStream(Files.newOutputStream(libraryFile), libraryFile, false);
    }

    @Override
    public OutputStream addAgent(String library, Patcher patcher) throws IOException {
        Path patcherDir = baseDir.resolve(patcher.name());
        Files.createDirectories(patcherDir);
        Path libraryFile = patcherDir.resolve(library + ".jar");
        return new ClasspathOutputStream(Files.newOutputStream(libraryFile), libraryFile, true);
    }

    @Override
    public void patch(Path library, Patcher patcher, PatchAction action) {
        Path relative = library.getFileName();
        if (library.startsWith(mcFiles.getLibraryDir())) {
            relative = mcFiles.getLibraryDir().relativize(library);
        } else if (relativePaths.containsKey(library)) {
            relative = relativePaths.get(library);
        }

        Path patchedFile = baseDir.resolve(patcher.name()).resolve(relative);
        try {
            Files.createDirectories(patchedFile.getParent());
            try (
                JarFile source = new JarFile(library.toFile());
                JarOutputStream destination = new JarOutputStream(Files.newOutputStream(patchedFile));
            ) {
                if (action.apply(source, destination)) {
                    if (library.startsWith(baseDir)) {
                        fileService.deleteFileAndEmptyParentDirs(library);
                    }

                    SequencedSet<Path> classpath = new LinkedHashSet<>(currentPatchResult.files());
                    classpath.remove(library);
                    classpath.add(patchedFile);
                    currentPatchResult = new PatchResult(
                        classpath, currentPatchResult.javaAgents(), currentPatchResult.systemProperties()
                    );
                    relativePaths.put(patchedFile, relative);
                } else {
                    fileService.deleteFileAndEmptyParentDirs(patchedFile);
                }
            }
        } catch (IOException e) {
            throw new PatchException("Failed to patch library " + library + " with patcher " + patcher.name(), e);
        }
    }

    @Override
    public void addSystemProperty(String key, @Nullable String value) {
        currentPatchResult = currentPatchResult.withSystemProperty(key, value);
    }

    @Override
    public <C extends HelperService> Stream<C> services(Class<C> type) {
        return services.get().filter(type::isInstance).map(type::cast);
    }

    private final class ClasspathOutputStream extends FilterOutputStream {
        private final Path file;
        private final boolean agent;

        public ClasspathOutputStream(OutputStream out, Path file, boolean agent) {
            super(out);
            this.file = file;
            this.agent = agent;
        }

        @Override
        public void close() throws IOException {
            super.close();
            currentPatchResult = agent ? currentPatchResult.withAgent(file) : currentPatchResult.withFile(file);
            relativePaths.put(file, file.getFileName());
        }
    }

}
