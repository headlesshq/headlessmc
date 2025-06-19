package io.github.headlesshq.headlessmc.files;

import lombok.experimental.UtilityClass;

import java.nio.file.Path;
import java.nio.file.Paths;

@UtilityClass
public class PathUtil {
    public static Path stripQuotes(String path) {
        return Paths.get(stripQuotesAtStartAndEnd(path));
    }

    public static String stripQuotesAtStartAndEnd(String path) {
        if (path.startsWith("\"") && path.endsWith("\"")) {
            return path.substring(1, path.length() - 1);
        }

        return path;
    }

}
