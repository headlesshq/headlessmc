package io.github.headlesshq.headlessmc.patcher.asm;

import io.github.headlesshq.headlessmc.patcher.*;
import org.jspecify.annotations.Nullable;

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
    private final PatchResult initialPatchResult;
    private final Path baseDir;
    private PatchResult currentPatchResult;

    FakePatchContext(Path baseDir, PatchResult patchResult, List<HelperService> services) {
        this.baseDir = baseDir;
        this.initialPatchResult = patchResult;
        this.currentPatchResult = patchResult;
        this.services = services;
    }

    @Override
    public PatchResult getInitialPatchResult() {
        return initialPatchResult;
    }

    @Override
    public PatchResult getCurrentPatchResult() {
        return currentPatchResult;
    }

    @Override
    public OutputStream add(String library, Patcher patcher) throws IOException {
        Path file = baseDir.resolve(patcher.name()).resolve(library + ".jar");
        Files.createDirectories(file.getParent());
        currentPatchResult = currentPatchResult.withFile(file);
        return Files.newOutputStream(file);
    }

    @Override
    public OutputStream addAgent(String library, Patcher patcher) throws IOException {
        Path file = baseDir.resolve(patcher.name()).resolve(library + ".jar");
        Files.createDirectories(file.getParent());
        currentPatchResult = currentPatchResult.withAgent(file);
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
                SequencedSet<Path> files = new LinkedHashSet<>(currentPatchResult.files());
                files.remove(library);
                files.add(out);
                currentPatchResult = new PatchResult(files, currentPatchResult.javaAgents(), currentPatchResult.systemProperties());
            } else {
                Files.deleteIfExists(out);
            }
        } catch (IOException e) {
            throw new PatchException("Failed to patch " + library, e);
        }
    }

    @Override
    public void addSystemProperty(String key, @Nullable String value) {
        currentPatchResult = currentPatchResult.withSystemProperty(key, value);
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
