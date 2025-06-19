package io.github.headlesshq.headlessmc.modrinth;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
final class ModrinthFile {
    @SerializedName("hashes")
    private final Hashes hashes;
    @SerializedName("url")
    private final String url;
    @SerializedName("filename")
    private final String filename;
    @SerializedName("primary")
    private final boolean primary;
    @SerializedName("size")
    private final long size;

    @Data
    public static class Hashes {
        @SerializedName("sha1")
        private final String sha1;
        @SerializedName("sha512")
        private final String sha512;
    }

}
