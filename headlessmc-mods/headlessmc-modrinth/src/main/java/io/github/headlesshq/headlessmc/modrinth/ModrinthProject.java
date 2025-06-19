package io.github.headlesshq.headlessmc.modrinth;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
final class ModrinthProject {
    @SerializedName("slug")
    private final String slug;
    @SerializedName("author")
    private final String author;
    @SerializedName("description")
    private final String description;
    @SerializedName("icon_url")
    private final String iconUrl;

}
