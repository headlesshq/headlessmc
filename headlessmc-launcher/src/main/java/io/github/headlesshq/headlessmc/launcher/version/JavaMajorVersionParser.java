package io.github.headlesshq.headlessmc.launcher.version;

import com.google.gson.JsonElement;
import io.github.headlesshq.headlessmc.java.JavaVersionParser;
import lombok.val;

class JavaMajorVersionParser {
    public Integer parse(JsonElement element) {
        if (element == null || !element.isJsonObject()) {
            return null;
        }

        val version = element.getAsJsonObject().get("majorVersion");
        return version != null
            ? Integer.parseInt(JavaVersionParser.getMajorVersion(
                version.getAsString()))
            : null;
    }

}
