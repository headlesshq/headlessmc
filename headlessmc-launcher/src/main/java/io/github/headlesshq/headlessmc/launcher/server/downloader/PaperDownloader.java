package io.github.headlesshq.headlessmc.launcher.server.downloader;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import io.github.headlesshq.headlessmc.launcher.Launcher;
import io.github.headlesshq.headlessmc.launcher.download.DownloadService;
import io.github.headlesshq.headlessmc.launcher.server.ServerTypeDownloader;
import io.github.headlesshq.headlessmc.launcher.util.JsonUtil;
import io.github.headlesshq.headlessmc.launcher.util.URLs;
import net.lenni0451.commons.httpclient.HttpResponse;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URL;

@CustomLog
@RequiredArgsConstructor
public class PaperDownloader implements ServerTypeDownloader {
    // PaperMC's v2 API (api.papermc.io/v2) was sunset and now returns HTTP 410.
    // The current API is v3 ("fill"), which returns builds newest-first and
    // provides a ready-made download url per build.
    private static final URL URL = URLs.url("https://fill.papermc.io/v3/projects/paper/versions/");

    @Override
    public DownloadHandler download(Launcher launcher, String version, @Nullable String typeVersionIn, String... args) throws IOException {
        JsonObject build = getBuild(launcher.getDownloadService(), version, typeVersionIn);
        String buildId = String.valueOf(JsonUtil.getLong(build, "id"));
        String url = JsonUtil.getString(build, "downloads", "server:default", "url");
        if (url == null) {
            throw new IOException("No server download for paper " + version + " build " + buildId);
        }

        log.debug("Downloading paper from " + url);
        return new UrlJarDownloadHandler(launcher.getDownloadService(), url, buildId);
    }

    private JsonObject getBuild(DownloadService downloadService, String version, @Nullable String typeVersionIn) throws IOException {
        HttpResponse response = downloadService.download(new URL(URL + version + "/builds"));
        String string = response.getContentAsString();
        JsonElement element = JsonParser.parseString(string);
        if (!element.isJsonArray()) {
            throw new IOException("Expected a builds array in " + string);
        }

        JsonArray builds = element.getAsJsonArray();
        if (builds.isEmpty()) {
            throw new IOException("No builds found in " + string);
        }

        // v3 returns builds newest-first, so the latest build is the first entry.
        if (typeVersionIn == null) {
            return builds.get(0).getAsJsonObject();
        }

        for (JsonElement candidate : builds) {
            JsonObject build = candidate.getAsJsonObject();
            if (typeVersionIn.equals(String.valueOf(JsonUtil.getLong(build, "id")))) {
                return build;
            }
        }

        throw new IOException("No paper build " + typeVersionIn + " found for " + version);
    }

}
