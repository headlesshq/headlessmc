package io.github.headlesshq.headlessmc.version.parser;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.headlesshq.headlessmc.version.Download;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Parses the downloads of a version.
 * <pre>
{
    "downloads": {
        "client": {
            "sha1": "b88808bbb3da8d9f453694b5d8f74a3396f1a533",
            "size": 28984409,
            "url": "..."
        },
        "client_mappings": {
            "sha1": "57669731d542f98646772e91a0d68628f9827a5c",
            "size": 10670987,
            "url": "..."
        },
        "server": {
            "sha1": "e6ec2f64e6080b9b5d9b471b291c33cc7f509733",
            "size": 57269758,
            "url": "..."
        },
        "server_mappings": {
            "sha1": "f4812c1d66d0098a94616b19c21829e591d0af3a",
            "size": 8027211,
            "url": "..."
        }
    }
}</pre>
*/
final class DownloadsParser {
    public Download parseDownload(String name, JsonElement download) {
        String sha1 = JsonUtil.getString(download, "sha1");
        String url = JsonUtil.getString(download, "url");
        Long size = JsonUtil.getLong(download, "size");
        URI uri = url == null ? null : URI.create(url);
        return new DownloadImpl(name, sha1, size, uri);
    }

    public Map<String, Download> parseDownloads(JsonObject version) {
        Map<String, Download> result = new HashMap<>();
        JsonElement downloads = version.get("downloads");
        if (downloads != null) {
            if (!downloads.isJsonObject()) {
                throw new VersionParseException("Downloads were not a JSON object: " + downloads);
            }

            for (Map.Entry<String, JsonElement> entry : downloads.getAsJsonObject().entrySet()) {
                if (!entry.getValue().isJsonObject()) {
                    throw new VersionParseException("Download was not a JSON object: " + entry.getKey());
                }

                result.put(entry.getKey(), parseDownload(entry.getKey(), entry.getValue()));
            }
        }

        return result;
    }

}
