package io.github.headlesshq.headlessmc.launcher.version;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.headlesshq.headlessmc.launcher.util.JsonUtil;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;

import java.net.URI;

@Data
@RequiredArgsConstructor
public class Logging {
    private final String argument;
    private final String type;
    private final File file;

    @Deprecated
    @SuppressWarnings("unused") // used by graalvm
    private Logging() {
        this(null, null, null);
    }

    @Data
    @RequiredArgsConstructor
    public static class File {
        private final String id;
        private final String sha1;
        private final Long size;
        private final URI url;

        @Deprecated
        @SuppressWarnings("unused") // used by graalvm
        private File() {
            this(null, null, 0L, null);
        }
    }

    public static Logging fromJson(JsonElement element) {
        return new Gson().fromJson(element, Logging.class);
    }

    public static @Nullable Logging getFromVersion(JsonObject versionJson) {
        JsonElement element = JsonUtil.getElement(versionJson, "logging", "client");
        if (element != null) {
            return fromJson(element);
        }

        return null;
    }

    /*
    "logging": {
        "client": {
          "argument": "-Dlog4j.configurationFile\u003d${path}",
          "file": {
            "id": "client-1.12.xml",
            "sha1": "bd65e7d2e3c237be76cfbef4c2405033d7f91521",
            "size": 888,
            "url": "https://piston-data.mojang.com/v1/objects/bd65e7d2e3c237be76cfbef4c2405033d7f91521/client-1.12.xml"
          },
          "type": "log4j2-xml"
        }
      }
     */
}
