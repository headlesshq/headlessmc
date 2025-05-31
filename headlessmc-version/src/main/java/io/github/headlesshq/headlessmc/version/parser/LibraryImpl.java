package io.github.headlesshq.headlessmc.version.parser;

import io.github.headlesshq.headlessmc.os.OS;
import io.github.headlesshq.headlessmc.version.ExtractionRules;
import io.github.headlesshq.headlessmc.version.Library;
import io.github.headlesshq.headlessmc.version.Rule;
import lombok.CustomLog;
import lombok.Data;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.net.URI;
import java.util.Map;

@Data
@CustomLog
final class LibraryImpl implements Library {
    private static final URI DEFAULT_BASE_URL = URI.create("https://libraries.minecraft.net/");

    private final Map<String, String> natives;
    private final @Nullable ExtractionRules extractionRules;
    private final String name;
    private final Rule rule;
    private final @Nullable String baseUrl;
    private final @Nullable String sha1;
    private final @Nullable Long size;
    private final @Nullable URI url;
    private final @Nullable String path;
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

        String[] split = name.split(":");
        StringBuilder sb = new StringBuilder()
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
    public URI getUrl(String path) {
        if (url != null) {
            return url;
        }

        if (baseUrl == null) {
            log.debug("Assuming " + getName() + " has base url " + DEFAULT_BASE_URL);
            return URI.create(DEFAULT_BASE_URL + path.replace(File.separator, "/"));
        }

        return URI.create(baseUrl + path.replace(File.separator, "/"));
    }

}
