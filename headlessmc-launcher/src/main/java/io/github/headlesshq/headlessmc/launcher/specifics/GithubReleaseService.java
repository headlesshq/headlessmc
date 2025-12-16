package io.github.headlesshq.headlessmc.launcher.specifics;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.SerializedName;
import io.github.headlesshq.headlessmc.launcher.download.DownloadService;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import net.lenni0451.commons.httpclient.HttpResponse;

import java.io.IOException;
import java.net.URL;

@RequiredArgsConstructor
class GithubReleaseService {
    private final DownloadService downloadService;

    public GithubRelease getGithubRelease(String owner, String repo) throws IOException {
        URL url = new URL(String.format("https://api.github.com/repos/%s/%s/releases/latest", owner, repo));
        // Would be cool if we could check for the env variable GITHUB_TOKEN and use it?
        HttpResponse response = downloadService.download(url);
        try {
            return new Gson().fromJson(response.getContentAsString(), GithubRelease.class);
        } catch (JsonParseException e) {
            throw new IOException(e);
        }
    }

    @ToString
    static class GithubRelease {
        @SerializedName("tag_name")
        String tag_name;
    }

}
