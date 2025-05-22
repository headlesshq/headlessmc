package io.github.headlesshq.headlessmc.launcher.version;

import io.github.headlesshq.headlessmc.api.traits.HasName;
import io.github.headlesshq.headlessmc.os.OS;
import org.jetbrains.annotations.Nullable;

public interface Library extends HasName {
    String getPath(OS os);

    Rule getRule();

    Extractor getExtractor();

    String getUrl(String path);

    @Nullable String getSha1();

    @Nullable Long getSize();

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
