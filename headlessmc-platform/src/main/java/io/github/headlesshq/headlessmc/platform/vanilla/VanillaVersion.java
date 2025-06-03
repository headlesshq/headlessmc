package io.github.headlesshq.headlessmc.platform.vanilla;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.util.List;

@Data
public class VanillaVersion {
    @SerializedName("id")
    private final String name;
    @SerializedName("type")
    private final String type;
    @SerializedName("url")
    private final String url;

    @Data
    public static class Container {
        @SerializedName("versions")
        private final List<VanillaVersion> versions;
    }

}
