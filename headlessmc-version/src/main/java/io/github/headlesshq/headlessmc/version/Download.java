package io.github.headlesshq.headlessmc.version;

import org.jetbrains.annotations.Nullable;

import java.net.URL;

public interface Download {
    String getName();

    @Nullable String getSha1();

    @Nullable Long getSize();

    @Nullable URL getUrl();

}
