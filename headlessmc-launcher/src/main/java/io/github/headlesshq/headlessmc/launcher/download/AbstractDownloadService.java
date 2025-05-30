package io.github.headlesshq.headlessmc.launcher.download;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import io.github.headlesshq.headlessmc.launcher.Launcher;
import io.github.headlesshq.headlessmc.launcher.Service;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import net.lenni0451.commons.httpclient.HttpResponse;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@CustomLog
@RequiredArgsConstructor
public abstract class AbstractDownloadService<T> extends Service<T> {
    private final Launcher launcher;
    private final URL url;

    protected abstract List<T> read(JsonElement element)
        throws IOException, JsonParseException;

    @Override
    protected Collection<T> update() {
        List<T> result;
        log.debug("Downloading from " + url);
        String str = null;
        try {
            HttpResponse response = launcher.getDownloadService().get(url);
            if (response.getStatusCode() > 299 || response.getStatusCode() < 200) {
                throw new IOException("Response code " + response.getStatusCode());
            }

            str = response.getContentAsString();
            JsonElement je = JsonParser.parseString(str);
            result = read(je);
        } catch (JsonParseException | IOException e) {
            log.error("Couldn't download from " + url + " : " + e.getMessage());
            if (str != null) {
                log.error(str);
            }

            result = new ArrayList<>(0);
        }

        return result;
    }

}
