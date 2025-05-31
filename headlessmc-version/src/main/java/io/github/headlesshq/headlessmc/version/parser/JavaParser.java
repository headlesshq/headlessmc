package io.github.headlesshq.headlessmc.version.parser;

import com.google.gson.JsonElement;
import io.github.headlesshq.headlessmc.java.JavaVersionParser;
import org.jetbrains.annotations.Nullable;

/**
 * Parses the Java version entry.
 * <pre>
 "javaVersion": {
     "component": "java-runtime-delta",
     "majorVersion": 21
 }</pre>
 */
final class JavaParser {
    public @Nullable Integer parse(@Nullable JsonElement element) {
        if (element == null || !element.isJsonObject()) {
            return null;
        }

        JsonElement version = element.getAsJsonObject().get("majorVersion");
        return version != null
                ? Integer.parseInt(JavaVersionParser.getMajorVersion(version.getAsString()))
                : null;
    }

}
