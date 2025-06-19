package io.github.headlesshq.headlessmc.platform.paper;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import io.github.headlesshq.headlessmc.commons.DownloadService;
import io.github.headlesshq.headlessmc.platform.PlatformDownloadOptions;
import io.github.headlesshq.headlessmc.platform.PlatformDownloader;
import io.github.headlesshq.headlessmc.version.id.VersionID;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;
import net.lenni0451.commons.httpclient.HttpResponse;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;

@Paper
@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class PaperDownloader implements PlatformDownloader {
    private static final URI URL = URI.create("https://api.papermc.io/v2/projects/paper/versions/");

    private final DownloadService downloadService;

    @Override
    public Path download(PlatformDownloadOptions options) throws IOException {
        if (!options.getVersionID().isServer()) {
            throw new IOException("Paper is only supported on servers, not on client version " + options.getVersionID());
        }

        VersionID versionID = resolveBuild(options.getVersionID());
        String build = versionID.getBuild();
        String version = versionID.getVersion();
        URI url = URI.create(String.format("%s%s/builds/%s/downloads/paper-%s-%s.jar", URL, version, build, version, build));

        // log.debug("Downloading paper from " + url);
        Path paperJar = options.getPathProvider().getPath(versionID);
        downloadService.download(url.toString(), paperJar);
        return paperJar;
    }

    private VersionID resolveBuild(VersionID version) throws IOException {
        String build = version.getBuild();
        if (build == null) {
            HttpResponse response = downloadService.download(URI.create(URL.toString() + version + "/"));
            String string = response.getContentAsString();
            JsonElement element = JsonParser.parseString(string);
            JsonArray array = element.getAsJsonObject().getAsJsonArray("builds");
            if (array != null) {
                build = String.valueOf(array.get(array.size() - 1).getAsInt());
            }

            if (build == null) {
                throw new IOException("No builds found in " + string);
            }

            return version.withBuild(build);
        }

        return version;
    }

}
