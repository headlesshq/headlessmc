package io.github.headlesshq.headlessmc.version.parser;

import com.google.gson.JsonElement;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Map;

/**
 * Parses the natives entry on some libraries:
 * <pre>
    "natives": {
        "linux": "natives-linux",
        "osx": "natives-osx",
        "windows": "natives-windows"
    }</pre>
 */
final class NativesParser {
    public Map<String, String> parse(@Nullable JsonElement element) {
        if (element == null || !element.isJsonObject()) {
            return Collections.emptyMap();
        }

        return JsonUtil.toStringMap(element.getAsJsonObject());
    }

}
