package io.github.headlesshq.headlessmc.launcher.command.download;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import io.github.headlesshq.headlessmc.launcher.download.DownloadService;
import io.github.headlesshq.headlessmc.launcher.version.DefaultVersionFactory;
import io.github.headlesshq.headlessmc.launcher.version.Version;
import io.github.headlesshq.headlessmc.launcher.version.VersionParseException;
import io.github.headlesshq.headlessmc.launcher.version.VersionService;
import net.lenni0451.commons.httpclient.HttpResponse;

import java.io.IOException;
import java.nio.file.Paths;

public class VersionInfoUtil {
    public static Version toVersion(VersionInfo versionInfo,
                                    VersionService versionService,
                                    DownloadService downloadService) throws IOException {
        Version version = versionService.getVersionByName(versionInfo.getName());
        if (version != null) {
            return version;
        }

        // TODO: this more elegantly, version.json cache that does not fill the VersionService
        //  but that cache can be used before downloading version.jsons
        HttpResponse response = downloadService.download(new URL(versionInfo.getUrl()));
        String string = response.getContentAsString();
        DefaultVersionFactory defaultVersionFactory = new DefaultVersionFactory();
        JsonElement element = JsonParser.parseString(string);
        if (!element.isJsonObject()) {
            throw new IOException(string + " is not a JSON object");
        }

        try {
            return defaultVersionFactory.parse(
                    element.getAsJsonObject(),
                    Paths.get("HeadlessMC").resolve("versions").toFile(),
                    () -> 0);
        } catch (VersionParseException e) {
            throw new IOException(e);
        }
    }

}
