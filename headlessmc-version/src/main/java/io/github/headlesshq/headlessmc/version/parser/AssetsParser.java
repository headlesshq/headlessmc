package io.github.headlesshq.headlessmc.version.parser;

import com.google.gson.JsonObject;
import io.github.headlesshq.headlessmc.version.Assets;
import org.jetbrains.annotations.Nullable;

import java.net.URI;

final class AssetsParser {
    public @Nullable Assets parse(JsonObject json) {
        String name = JsonUtil.getString(json, "assets");
        String id = JsonUtil.getString(json, "assetIndex", "id");
        if (name == null || id == null) {
            return null;
        }

        String url = JsonUtil.getString(json, "assetIndex", "url");
        String sha1 = JsonUtil.getString(json, "assetIndex", "sha1");
        Long size = JsonUtil.getLong(json, "assetIndex", "size");
        URI uri = url == null ? null : URI.create(url);
        return new AssetsImpl(new DownloadImpl(id, sha1, size, uri), name);
    }

}
