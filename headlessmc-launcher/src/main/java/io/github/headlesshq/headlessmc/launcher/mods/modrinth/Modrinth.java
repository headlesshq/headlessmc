package io.github.headlesshq.headlessmc.launcher.mods.modrinth;

import com.google.gson.JsonParseException;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import io.github.headlesshq.headlessmc.launcher.api.VersionId;
import io.github.headlesshq.headlessmc.launcher.download.DownloadService;
import io.github.headlesshq.headlessmc.launcher.mods.Mod;
import io.github.headlesshq.headlessmc.launcher.mods.ModDistributionPlatform;
import io.github.headlesshq.headlessmc.launcher.mods.ModdableGame;
import io.github.headlesshq.headlessmc.launcher.util.JsonUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.CustomLog;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import net.lenni0451.commons.httpclient.HttpResponse;
import org.jetbrains.annotations.VisibleForTesting;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@CustomLog
@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class Modrinth implements ModDistributionPlatform {
    private static final URL DEFAULT_MODRINTH_API = URI.create("https://api.modrinth.com/v2/");

    private final DownloadService downloadService;
    private final URL api;

    public Modrinth(DownloadService downloadService) {
        this(downloadService, DEFAULT_MODRINTH_API);
    }

    @Override
    public List<Mod> search(String name) throws IOException {
        String url = String.format("%ssearch?query=%s&amp;facets=[[%%22project_type:mod%%22]]", api, name);
        return searchUrl(url);
    }

    @Override
    public List<Mod> search(String name, VersionId versionId) throws IOException {
        String url = String.format("%ssearch?query=%s&amp;facets=[[%%22project_type:mod%%22],[%%22categories:%s%%22],[%%22versions:%s%%22]]",
                api, name, versionId.getPlatform().getName(), versionId.getName());
        return searchUrl(url);
    }

    private List<Mod> searchUrl(String url) throws IOException {
        HttpResponse response = downloadService.download(new URL(url));
        try {
            String content = response.getContentAsString();
            SearchResult searchResult = JsonUtil.GSON.fromJson(content, SearchResult.class);
            List<Mod> mods = new ArrayList<>(searchResult.getHits().size());
            int id = 0;
            for (ModrinthProject project : searchResult.getHits()) {
                mods.add(new Mod(project.getSlug(), id++, project.getDescription(), Collections.singletonList(project.getAuthor())));
            }

            return mods;
        } catch (JsonParseException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void download(ModdableGame game, String modName) throws IOException {
        List<ModrinthProjectVersion> versions = getVersions(game.getVersionId(), modName);
        if (versions.isEmpty()) {
            throw new IOException("No versions of " + modName + " found for " + game.getVersionId());
        }

        ModrinthProjectVersion version = versions.get(0);
        if (version.getFiles().isEmpty()) {
            throw new IOException("No files found for version " + game.getVersionId() + " of " + modName);
        }

        ModrinthFile file = version.getFiles().get(0);
        for (ModrinthFile primaryFile : version.getFiles()) {
            if (primaryFile.isPrimary()) {
                file = primaryFile;
                break;
            }
        }

        log.debug("Downloading " + file + " to " + game.getModsDirectory().resolve(file.getFilename()));
        downloadService.download(
                file.getUrl(),
                game.getModsDirectory().resolve(file.getFilename()),
                file.getSize(),
                file.getHashes().getSha1()
        );
    }

    @Override
    public String getName() {
        return "Modrinth";
    }

    @VisibleForTesting
    List<ModrinthProjectVersion> getVersions(VersionId version, String name) throws IOException {
        String url = String.format("%sproject/%s/version?game_versions=[%%22%s%%22]&loaders=[%%22%s%%22]", api, name, version.getName(), version.getPlatform().getName());
        HttpResponse response = downloadService.download(new URL(url));
        try {
            String content = response.getContentAsString();
            TypeToken<List<ModrinthProjectVersion>> type = new TypeToken<List<ModrinthProjectVersion>>() {};
            return JsonUtil.GSON.fromJson(content, type);
        } catch (JsonParseException e) {
            throw new IOException(e);
        }
    }

    @Data
    private static class SearchResult {
        @SerializedName("hits")
        private final List<ModrinthProject> hits;
    }

}
