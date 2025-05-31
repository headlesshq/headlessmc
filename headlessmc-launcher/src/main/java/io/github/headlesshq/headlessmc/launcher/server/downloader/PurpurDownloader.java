package io.github.headlesshq.headlessmc.launcher.server.downloader;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import io.github.headlesshq.headlessmc.launcher.Launcher;
import io.github.headlesshq.headlessmc.launcher.download.DownloadService;
import io.github.headlesshq.headlessmc.launcher.server.ServerTypeDownloader;
import io.github.headlesshq.headlessmc.launcher.util.JsonUtil;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import net.lenni0451.commons.httpclient.HttpResponse;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URI;

@CustomLog
@RequiredArgsConstructor
public class PurpurDownloader implements ServerTypeDownloader {
    private static final URI URL = URI.create("https://api.purpurmc.org/v2/purpur/");

    @Override
    public DownloadHandler download(Launcher launcher, String version, @Nullable String typeVersionIn, String... args) throws IOException {
        String build = getBuild(launcher.getDownloadService(), version, typeVersionIn);
        URI url = URI.create(String.format("%s%s/%s/download", URL, version, build));
        log.debug("Downloading purpur from " + url);
        return new UrlJarDownloadHandler(launcher.getDownloadService(), url.toString(), build);
    }

    private String getBuild(DownloadService downloadService, String version, @Nullable String typeVersionIn) throws IOException {
        String build = typeVersionIn;
        if (build == null) {
            HttpResponse response = downloadService.download(URI.create(URL + version + "/"));
            String string = response.getContentAsString();
            JsonElement element = JsonParser.parseString(string);
            JsonArray array = JsonUtil.getArray(element, "builds", "all");
            if (array != null) {
                build = array.get(array.size() - 1).getAsString();
            }

            if (build == null) {
                throw new IOException("No builds found in " + string);
            }
        }

        return build;
    }

}
