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
public class PaperDownloader implements ServerTypeDownloader {
    private static final URI URL = URI.create("https://api.papermc.io/v2/projects/paper/versions/");

    @Override
    public DownloadHandler download(Launcher launcher, String version, @Nullable String typeVersionIn, String... args) throws IOException {
        String build = getBuild(launcher.getDownloadService(), version, typeVersionIn);
        URI url = URI.create(String.format("%s%s/builds/%s/downloads/paper-%s-%s.jar", URL, version, build, version, build));
        log.debug("Downloading paper from " + url);
        return new UrlJarDownloadHandler(launcher.getDownloadService(), url.toString(), build);
    }

    private String getBuild(DownloadService downloadService, String version, @Nullable String typeVersionIn) throws IOException {
        String build = typeVersionIn;
        if (build == null) {
            HttpResponse response = downloadService.download(new URL(URL + version + "/"));
            String string = response.getContentAsString();
            JsonElement element = JsonParser.parseString(string);
            JsonArray array = JsonUtil.getArray(element, "builds");
            if (array != null) {
                build = String.valueOf(array.get(array.size() - 1).getAsInt());
            }

            if (build == null) {
                throw new IOException("No builds found in " + string);
            }
        }

        return build;
    }

}
