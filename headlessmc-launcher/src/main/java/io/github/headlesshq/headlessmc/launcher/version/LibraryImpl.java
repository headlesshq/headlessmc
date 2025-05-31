package io.github.headlesshq.headlessmc.launcher.version;

import io.github.headlesshq.headlessmc.os.OS;
import lombok.CustomLog;
import lombok.Data;
import lombok.val;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Map;

@Data
@CustomLog
class LibraryImpl implements Library {
    private static final String URL = "https://libraries.minecraft.net/";

    private final Map<String, String> natives;
    private final Extractor extractor;
    private final String name;
    private final Rule rule;
    private final String baseUrl;
    private final @Nullable String sha1;
    private final @Nullable Long size;
    private final String url;
    private final String path;
    private final boolean nativeLibrary;

    @Override
    public String getPath(OS os) {
        String result = getPathWithDefaultPathSeparator(os);
        if (File.separatorChar != '/') {
            result = result.replace("/", File.separator);
        }

        return result;
    }

    private String getPathWithDefaultPathSeparator(OS os) {
        if (path != null) {
            return path.replace("${arch}", os.is64bit() ? "64" : "32");
        }

        val split = name.split(":");
        val sb = new StringBuilder()
            .append(split[0].replace(".", File.separator))
            .append(File.separator)
            .append(split[1])
            .append(File.separator)
            .append(split[2])
            .append(File.separator)
            .append(split[1])
            .append("-")
            .append(split[2]);

        String n = natives.get(os.getType().getName());
        if (n != null && nativeLibrary) {
            sb.append("-")
              .append(n.replace("${arch}", os.is64bit() ? "64" : "32"));
        }

        return sb.append(".jar").toString();
    }

    @Override
    public String getUrl(String path) {
        if (url != null) {
            return url;
        }

        if (baseUrl == null) {
            log.debug("Assuming " + getName() + " has base url " + URL);
            return URL + path.replace(File.separator, "/");
        }

        return baseUrl + path.replace(File.separator, "/");
    }

}
