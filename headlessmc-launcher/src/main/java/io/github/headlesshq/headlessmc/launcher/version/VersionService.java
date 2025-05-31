package io.github.headlesshq.headlessmc.launcher.version;

import com.google.gson.JsonElement;
import io.github.headlesshq.headlessmc.launcher.LazyService;
import io.github.headlesshq.headlessmc.launcher.util.JsonUtil;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.val;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Setter
@CustomLog
@RequiredArgsConstructor
public final class VersionService extends LazyService<Version> {
    private final ParentVersionResolver resolver = new ParentVersionResolver();
    private final LauncherConfig launcherConfig;

    private int retries = 0;

    public @Nullable Version getVersionByName(String name) {
        return stream()
                .filter(v -> v.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    @Override
    protected Collection<Version> update() {
        long nanos = System.nanoTime();
        val versionFolders = launcherConfig.getMcFiles().getDir("versions").listFiles();
        if (versionFolders == null) {
            log.warning("No Minecraft Version folder found!");
            return Collections.emptyList();
        }

        val fact = new DefaultVersionFactory();
        val versions = new LinkedHashMap<String, Version>();
        val id = new AtomicInteger();
        for (val versionFolder : versionFolders) {
            if (!versionFolder.isDirectory()) {
                continue;
            }

            val files = versionFolder.listFiles();
            if (files != null) {
                for (val versionFile : files) {
                    if (versionFile.getName().endsWith(".json")) {
                        read(versionFile, versionFolder, versions, id, fact);
                    }
                }
            } else {
                log.warning("Couldn't read " + versionFolder.getAbsolutePath());
            }
        }

        resolver.resolveParentVersions(versions);
        nanos = System.nanoTime() - nanos;
        log.debug("Version refresh took " + (nanos / 1_000_000.0) + "ms.");
        return versions.values();
    }

    private void read(File file, File folder, Map<String, Version> versions, AtomicInteger id, VersionFactory factory) {
        for (int i = 0; i < retries + 1; i++) {
            try {
                log.debug("Reading " + file.getAbsolutePath());
                JsonElement je = JsonUtil.fromFile(file);
                val version = factory.parse(je.getAsJsonObject(), folder, id::getAndIncrement);
                if (version.getName() == null) {
                    log.warning("Failed to read version " + file.getName() + ", it did not provide a name!");
                } else {
                    versions.put(version.getName(), version);
                }

                return;
            } catch (Exception e) {
                if (i == retries) {
                    log.warning("Failed to read " + file.getName() + ", " + e.getClass() + ": " + e.getMessage());
                } else {
                    try {
                        log.debug("Retrying " + file.getName() + " : " + e.getMessage());
                        //noinspection BusyWait
                        Thread.sleep(100);
                    } catch (InterruptedException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            }
        }
    }

}
