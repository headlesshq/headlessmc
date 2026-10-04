package io.github.headlesshq.headlessmc.patcher.asm;

import io.github.headlesshq.headlessmc.patcher.Classpath;
import io.github.headlesshq.headlessmc.patcher.HelperService;
import io.github.headlesshq.headlessmc.patcher.PatchCache;
import io.github.headlesshq.headlessmc.patcher.PatchContext;
import io.github.headlesshq.headlessmc.patcher.PatchException;
import io.github.headlesshq.headlessmc.patcher.Patcher;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.SequencedSet;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.stream.Stream;

/**
 * Minimal PatchContext for tests, patching jars below a base dir
 * like the real implementation does.
 */
final class FakePatchContext implements PatchContext {
    private final List<HelperService> services;
    private final Classpath initialClasspath;
    private final Path baseDir;
    private Classpath currentClasspath;

    FakePatchContext(Path baseDir, Classpath classpath, List<HelperService> services) {
        this.baseDir = baseDir;
        this.initialClasspath = classpath;
        this.currentClasspath = classpath;
        this.services = services;
    }

    @Override
    public Classpath getInitialClasspath() {
        return initialClasspath;
    }

    @Override
    public Classpath getCurrentClasspath() {
        return currentClasspath;
    }

    @Override
    public OutputStream add(String library, Patcher patcher) throws IOException {
        Path file = baseDir.resolve(patcher.name()).resolve(library + ".jar");
        Files.createDirectories(file.getParent());
        currentClasspath = currentClasspath.withFile(file);
        return Files.newOutputStream(file);
    }

    @Override
    public OutputStream addAgent(String library, Patcher patcher) throws IOException {
        Path file = baseDir.resolve(patcher.name()).resolve(library + ".jar");
        Files.createDirectories(file.getParent());
        currentClasspath = currentClasspath.withAgent(file);
        return Files.newOutputStream(file);
    }

    @Override
    public void patch(Path library, Patcher patcher, PatchAction action) {
        try {
            Path out = baseDir.resolve(patcher.name()).resolve(library.getFileName().toString());
            Files.createDirectories(out.getParent());
            boolean changed;
            try (JarFile source = new JarFile(library.toFile());
                 JarOutputStream destination = new JarOutputStream(Files.newOutputStream(out))) {
                changed = action.apply(source, destination);
            }

            if (changed) {
                SequencedSet<Path> files = new LinkedHashSet<>(currentClasspath.files());
                files.remove(library);
                files.add(out);
                currentClasspath = new Classpath(files, currentClasspath.javaAgents());
            } else {
                Files.deleteIfExists(out);
            }
        } catch (IOException e) {
            throw new PatchException("Failed to patch " + library, e);
        }
    }

    @Override
    public List<Patcher> getPatchers() {
        return List.of();
    }

    @Override
    public int getJavaVersion() {
        return 21;
    }

    @Override
    public <C extends HelperService> Stream<C> services(Class<C> type) {
        return services.stream().filter(type::isInstance).map(type::cast);
    }

    @Override
    public PatchCache.Key getCacheKey() {
        return new PatchCache.Key("test", 0L, Map.of(), getJavaVersion(), 0L);
    }

}
