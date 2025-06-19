package io.github.headlesshq.headlessmc.modrinth;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import io.github.headlesshq.headlessmc.commons.DownloadService;
import io.github.headlesshq.headlessmc.mods.ModDistributionPlatform;
import io.github.headlesshq.headlessmc.mods.ModDownload;
import io.github.headlesshq.headlessmc.version.id.VersionID;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;
import net.lenni0451.commons.httpclient.HttpResponse;
import org.jetbrains.annotations.VisibleForTesting;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The Modrinth {@link ModDistributionPlatform}.
 * @see <a href="https://modrinth.com/">https://modrinth.com/</a>
 */
@ApplicationScoped
@RequiredArgsConstructor
public class Modrinth implements ModDistributionPlatform {
    private static final URI DEFAULT_MODRINTH_API = URI.create("https://api.modrinth.com/v2/");
    private static final Gson GSON = new Gson();

    private final DownloadService downloadService;
    private final URI api;

    @Inject
    public Modrinth(DownloadService downloadService) {
        this(downloadService, DEFAULT_MODRINTH_API);
    }

    @Override
    public String getName() {
        return "modrinth";
    }

    @Override
    public List<ModDownload> search(String name) throws IOException {
        String url = String.format("%ssearch?query=%s&amp;facets=[[%%22project_type:mod%%22]]", api, name);
        return searchUrl(url);
    }

    @Override
    public List<ModDownload> search(String name, VersionID versionId) throws IOException {
        String url = String.format(
            "%ssearch?query=%s&amp;facets=[[%%22project_type:mod%%22],[%%22categories:%s%%22],[%%22versions:%s%%22]]",
            api, name, versionId.getPlatform(), versionId.getVersion()
        );

        return searchUrl(url);
    }

    @Override
    public void download(VersionID versionID, Path path, String modName) throws IOException {
        List<ModrinthProjectVersion> versions = getVersions(versionID, modName);
        if (versions.isEmpty()) {
            throw new IOException("No versions of " + modName + " found for " + versionID);
        }

        ModrinthProjectVersion version = versions.get(0);
        if (version.getFiles().isEmpty()) {
            throw new IOException("No files found for version " + versionID + " of " + modName);
        }

        ModrinthFile file = version.getFiles().get(0);
        for (ModrinthFile primaryFile : version.getFiles()) {
            if (primaryFile.isPrimary()) {
                file = primaryFile;
                break;
            }
        }

        //log.debug("Downloading " + file + " to " + game.getModsDirectory().resolve(file.getFilename()));
        downloadService.download(
                file.getUrl(),
                path.resolve(file.getFilename()),
                file.getSize(),
                file.getHashes().getSha1()
        );
    }

    private List<ModDownload> searchUrl(String url) throws IOException {
        HttpResponse response = downloadService.download(URI.create(url));
        try {
            String content = response.getContentAsString();
            ModrinthSearchResult searchResult = GSON.fromJson(content, ModrinthSearchResult.class);
            List<ModDownload> mods = new ArrayList<>(searchResult.getHits().size());
            for (ModrinthProject project : searchResult.getHits()) {
                mods.add(new ModDownload(
                        project.getSlug(),
                        project.getDescription(),
                        Collections.singletonList(project.getAuthor()),
                        project.getIconUrl()
                ));
            }

            return mods;
        } catch (JsonParseException e) {
            throw new IOException(e);
        }
    }

    @VisibleForTesting
    List<ModrinthProjectVersion> getVersions(VersionID version, String name) throws IOException {
        String url = String.format(
            "%sproject/%s/version?game_versions=[%%22%s%%22]&loaders=[%%22%s%%22]",
            api, name, version.getVersion(), version.getPlatform()
        );

        HttpResponse response = downloadService.download(URI.create(url));

        try {
            String content = response.getContentAsString();
            TypeToken<List<ModrinthProjectVersion>> type = new TypeToken<List<ModrinthProjectVersion>>() {};
            return GSON.fromJson(content, type);
        } catch (JsonParseException e) {
            throw new IOException(e);
        }
    }

}
