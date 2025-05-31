package io.github.headlesshq.headlessmc.version.parser;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.headlesshq.headlessmc.version.Logging;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;

/**
 * Represents the Logging entry in a version.
 * <pre>
 "logging": {
     "client": {
         "argument": "-Dlog4j.configurationFile=${path}",
         "file": {
             "id": "client-1.21.2.xml",
             "sha1": "39384bd14c0606d812afec88d8aff595b2587dd9",
             "size": 1073,
             "url": "..."
         },
     "type": "log4j2-xml"
 }</pre>
 */
@Data
@RequiredArgsConstructor
final class LoggingImpl implements Logging {
    private final String argument;
    private final String type;
    private final DownloadImpl file;

    @Deprecated
    @SuppressWarnings({"unused", "DataFlowIssue"}) // used by graalvm
    private LoggingImpl() {
        this(null, null, null);
    }

    public static LoggingImpl fromJson(JsonElement element) {
        return new Gson().fromJson(element, LoggingImpl.class);
    }

    public static @Nullable LoggingImpl getFromVersion(JsonObject versionJson) {
        JsonElement element = JsonUtil.getElement(versionJson, "logging", "client");
        if (element != null) {
            return fromJson(element);
        }

        return null;
    }

}
