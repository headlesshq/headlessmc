package io.github.headlesshq.headlessmc.version.parser;

import com.google.gson.annotations.SerializedName;
import io.github.headlesshq.headlessmc.version.Download;
import lombok.Data;
import org.jetbrains.annotations.Nullable;

import java.net.URI;

@Data
final class DownloadImpl implements Download {
    @SerializedName(value = "id", alternate = "path")
    private final String name;
    private final @Nullable String sha1;
    private final @Nullable Long size;
    private final @Nullable URI url;

}
