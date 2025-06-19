package io.github.headlesshq.headlessmc.platform.purpur;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import io.github.headlesshq.headlessmc.commons.DownloadService;
import io.github.headlesshq.headlessmc.platform.PlatformDownloadOptions;
import io.github.headlesshq.headlessmc.platform.PlatformDownloader;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;
import net.lenni0451.commons.httpclient.HttpResponse;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;

@Purpur
@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class PurpurDownloader implements PlatformDownloader {
    private static final URI URL = URI.create("https://api.purpurmc.org/v2/purpur/");

    private final DownloadService downloadService;

    @Override
    public Path download(PlatformDownloadOptions options) throws IOException {
        return null;
    }

    public DownloadHandler download(Launcher launcher, String version, @Nullable String typeVersionIn, String... args) throws IOException {
        String build = getBuild(launcher.getDownloadService(), version, typeVersionIn);
        URI url = URI.create(String.format("%s%s/%s/download", URL, version, build));
        log.debug("Downloading purpur from " + url);
        return new UrlJarDownloadHandler(launcher.getDownloadService(), url.toString(), build);
    }

    private String getBuild(String version, @Nullable String typeVersionIn) throws IOException {
        String build = typeVersionIn;
        if (build == null) {
            // TODO: catch json exceptions
            HttpResponse response = downloadService.download(URI.create(URL + version + "/"));
            String string = response.getContentAsString();
            JsonElement element = JsonParser.parseString(string);
            build = element.getAsJsonObject().getAsJsonObject("builds").getAsJsonPrimitive("latest").getAsString();
            if (build == null) {
                throw new IOException("No builds found in " + string);
            }
        }

        return build;
    }

}
