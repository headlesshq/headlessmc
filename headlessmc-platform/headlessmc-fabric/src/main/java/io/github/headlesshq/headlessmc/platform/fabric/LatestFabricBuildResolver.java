package io.github.headlesshq.headlessmc.platform.fabric;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import io.github.headlesshq.headlessmc.commons.DownloadService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.Collections;
import java.util.List;

@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class LatestFabricBuildResolver {
    private final URI url = URI.create("https://meta.fabricmc.net/v2/versions/loader");
    private final DownloadService downloadService;

    public String getLatestBuild() throws IOException {
        String json = downloadService.download(url).getContentAsString();
        Gson gson = new Gson();
        Type listType = new TypeToken<List<BuildData>>() {}.getType();
        List<BuildData> builds = gson.fromJson(json, listType);
        Collections.sort(builds);
        if (builds.isEmpty()) {
            throw new IOException("No Fabric builds found!");
        }

        return builds.get(builds.size() - 1).getVersion();
    }

    @Getter
    private static class BuildData implements Comparable<BuildData> {
        @SerializedName("separator")
        String separator;
        @SerializedName("build")
        int build;
        @SerializedName("maven")
        String maven;
        @SerializedName("version")
        String version;
        @SerializedName("stable")
        boolean stable;

        @Override
        public int compareTo(BuildData other) {
            return Integer.compare(this.build, other.build);
        }
    }

}
