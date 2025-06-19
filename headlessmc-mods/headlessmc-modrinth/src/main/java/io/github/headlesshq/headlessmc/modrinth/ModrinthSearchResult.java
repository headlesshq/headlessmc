package io.github.headlesshq.headlessmc.modrinth;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.util.List;

@Data
final class ModrinthSearchResult {
    @SerializedName("hits")
    private final List<ModrinthProject> hits;

}
