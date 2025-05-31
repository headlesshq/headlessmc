package io.github.headlesshq.headlessmc.launcher.command.download;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.headlesshq.headlessmc.launcher.util.JsonUtil;
import lombok.CustomLog;
import lombok.Getter;
import lombok.experimental.Delegate;
import lombok.val;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@CustomLog
public class VersionInfoCache implements Collection<VersionInfo> {
    private static final List<VersionInfo> EMPTY = new ArrayList<>(0);
    private static final URI URL = URI.create("https://launchermeta.mojang.com/mc/game/version_manifest.json");
    @Delegate
    private List<VersionInfo> infos = EMPTY;
    @Getter
    private String latestSnapshot = "unknown";
    @Getter
    private String latestRelease = "unknown";
    @Getter
    private boolean cached = false;

    public List<VersionInfo> cache(boolean force) {
        if (infos != EMPTY && !force) {
            return infos;
        }

        try (val is = URL.openStream()) {
            val jo = JsonUtil.fromInput(is);
            if (!jo.isJsonObject()) {
                throw new IOException(jo + " is not a JsonObject!");
            }

            read(jo.getAsJsonObject());
        } catch (IOException e) {
            log.error("Couldn't download versions", e);
            infos = new ArrayList<>(0);
        } finally {
            cached = true;
        }

        return infos;
    }

    public @Nullable VersionInfo getByName(String name) {
        return cache(false)
                .stream()
                .filter(v -> v.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

    public void read(JsonObject jo) throws IOException {
        val latestR = JsonUtil.getString(jo, "latest", "release");
        val latestS = JsonUtil.getString(jo, "latest", "snapshot");
        log.debug("Latest: " + latestR + ", " + latestS);
        latestRelease = latestR == null ? latestRelease : latestR;
        latestSnapshot = latestS == null ? latestSnapshot : latestS;

        val versions = JsonUtil.getArray(jo, "versions");
        if (versions == null) {
            throw new IOException("Couldn't find JsonObject versions!");
        }

        val infos = new ArrayList<VersionInfo>(versions.size());
        for (int i = 0; i < versions.size(); i++) {
            JsonElement entry = versions.get(i);
            if (!entry.isJsonObject()) {
                log.warning("Can't read version with id " + i);
                continue;
            }

            infos.add(read(entry.getAsJsonObject(), i));
        }

        this.infos = infos;
    }

    private VersionInfo read(JsonObject jo, int id) {
        val name = JsonUtil.getString(jo, "id");
        val type = JsonUtil.getString(jo, "type");
        val url = JsonUtil.getString(jo, "url");

        return new VersionInfo(id, name, type, url);
    }

}
