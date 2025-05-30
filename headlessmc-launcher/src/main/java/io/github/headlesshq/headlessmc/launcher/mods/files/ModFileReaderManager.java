package io.github.headlesshq.headlessmc.launcher.mods.files;

import io.github.headlesshq.headlessmc.launcher.api.Platform;
import io.github.headlesshq.headlessmc.launcher.mods.ModdableGame;
import jakarta.enterprise.context.ApplicationScoped;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ApplicationScoped
public class ModFileReaderManager {
    private final Map<Platform, ModFileReader> readers = new HashMap<>();

    public @Nullable ModFileReader getReader(Platform platform) {
        return readers.get(platform);
    }

    public void add(Platform platform, ModFileReader reader) {
        readers.put(platform, reader);
    }

    public ModFileReadResult read(ModdableGame game) throws IOException {
        try {
            Files.createDirectories(game.getModsDirectory());
            try (Stream<Path> stream = Files.list(game.getModsDirectory())) {
                List<Path> modFiles = stream
                        .filter(p -> !Files.isDirectory(p))
                        .filter(p -> p.toString().toLowerCase(Locale.ENGLISH).endsWith(".jar"))
                        .collect(Collectors.toList());

                ModFileReader reader = getReader(game.getPlatform());
                if (reader == null) {
                    throw new IOException("Unsupported platform: " + game.getPlatform());
                }

                int nonModFiles = 0;
                List<ModFile> mods = new ArrayList<>();
                AtomicInteger counter = new AtomicInteger();
                for (Path modFile : modFiles) {
                    List<ModFile> modsInFile = reader.read(counter::getAndIncrement, modFile);
                    if (modsInFile.isEmpty()) {
                        nonModFiles++;
                    } else {
                        mods.addAll(modsInFile);
                    }
                }

                return new ModFileReadResult(mods, nonModFiles);
            }
        } catch (IllegalArgumentException e) {
            throw new IOException(e);
        }
    }

    public static ModFileReaderManager create() {
        ModFileReaderManager manager = new ModFileReaderManager();
        manager.add(Platform.FABRIC, new FabricModFileReader());
        manager.add(Platform.NEOFORGE, ForgeModFileReader.neoforge());
        manager.add(Platform.FORGE, ForgeModFileReader.forge());
        manager.add(Platform.PAPER, PaperModFileReader.paper());
        manager.add(Platform.PURPUR, PaperModFileReader.paper());
        return manager;
    }

}
