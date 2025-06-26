package io.github.headlesshq.headlessmc.platform.forge;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import io.github.headlesshq.headlessmc.commons.DownloadService;
import io.github.headlesshq.headlessmc.platform.AbstractHttpCache;
import lombok.Data;
import net.lenni0451.commons.httpclient.HttpResponse;

import java.io.IOException;
import java.net.URI;
import java.util.List;

public class ForgeIndexCache extends AbstractHttpCache<List<ForgeVersion>> {
    public ForgeIndexCache(DownloadService downloadService, URI url) {
        super(downloadService, url);
    }

    @Override
    protected List<ForgeVersion> parse(HttpResponse response) throws IOException {
        Gson gson = new Gson();
        ForgeVersionList list = gson.fromJson(response.getContentAsString(), ForgeVersionList.class);
        return list.getVersions();
    }

    @Data
    public static class ForgeVersionList {
        @SerializedName("versions")
        private List<ForgeVersion> versions;
    }

}
