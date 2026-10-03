package io.github.headlesshq.headlessmc.platform;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.exceptions.HeadlessMcIOException;
import io.github.headlesshq.headlessmc.platform.mods.Mod;
import io.github.headlesshq.headlessmc.platform.mods.ModReader;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

@Getter
@RequiredArgsConstructor
public abstract class AbstractJarEntryModReader implements ModReader {
    private final Set<String> entryNames;

    public AbstractJarEntryModReader(String... names) {
        this(Collections.unmodifiableSequencedSet(new LinkedHashSet<>(List.of(names))));
    }

    @Override
    public List<Mod> read(Path jar) throws HeadlessMcException {
        List<Mod> result = new ArrayList<>();
        try (JarFile jarFile = new JarFile(jar.toFile())) {
            for (String name : getEntryNames()) {
                JarEntry entry = jarFile.getJarEntry(name);
                if (entry == null) {
                    continue;
                }

                try (InputStream entryInputStream = jarFile.getInputStream(entry)) {
                    Optional<List<Mod>> parsed = readEntry(entryInputStream);
                    parsed.ifPresent(result::addAll);
                    if (parsed.isPresent() && !parsed.get().isEmpty() && stopAfterFoundEntry()) {
                        return result;
                    }
                }
            }
        } catch (IOException e) {
            throw new HeadlessMcIOException("Failed to read jar file " + jar, e);
        }

        return result;
    }

    protected boolean stopAfterFoundEntry() {
        return false;
    }

}
