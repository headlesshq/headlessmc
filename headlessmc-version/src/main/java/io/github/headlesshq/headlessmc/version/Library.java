package io.github.headlesshq.headlessmc.version;

import io.github.headlesshq.headlessmc.os.OS;
import org.jetbrains.annotations.Nullable;

import java.net.URI;

public interface Library extends Download {
    String getPath(OS os);

    Rule getRule();

    @Nullable ExtractionRules getExtractionRules();

    URI getUrl(String path);

    boolean isNativeLibrary();

    default boolean isOrContainsNatives(OS os) {
        return isNativeLibrary() || getPath(os).contains("natives");
    }

    default String getPackage() {
        return getName().split(":")[0];
    }

    default String getNameAfterPackage() {
        return getName().split(":")[1];
    }

    default String getVersionNumber() {
        return getName().split(":")[2];
    }

}
