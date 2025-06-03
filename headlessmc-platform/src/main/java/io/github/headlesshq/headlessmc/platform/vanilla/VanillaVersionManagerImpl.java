package io.github.headlesshq.headlessmc.platform.vanilla;

import com.google.gson.Gson;
import io.github.headlesshq.headlessmc.commons.DownloadService;
import io.github.headlesshq.headlessmc.platform.AbstractHttpCache;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import net.lenni0451.commons.httpclient.HttpResponse;

import java.net.URI;
import java.util.List;

@ApplicationScoped
public class VanillaVersionManagerImpl extends AbstractHttpCache<VanillaVersion.Container> implements VanillaVersionManager {
    public static final URI URL = URI.create("https://launchermeta.mojang.com/mc/game/version_manifest.json");

    @Inject
    public VanillaVersionManagerImpl(DownloadService downloadService) {
        this(downloadService, URL);
        System.out.println(downloadService);
    }

    public VanillaVersionManagerImpl(DownloadService downloadService, URI url) {
        super(downloadService, url);
    }

    @Override
    public List<VanillaVersion> getVersions() {
        return get().getVersions();
    }

    @Override
    public boolean isOlderThan(String version, String versionToCheck) {
        List<VanillaVersion> versions = getVersions();
        int newIndex = -1;
        int oldIndex = -1;
        for (int i = 0; i < versions.size(); i++) {
            VanillaVersion vanillaVersion = versions.get(i);
            if (vanillaVersion.getName().equalsIgnoreCase(version)) {
                newIndex = i;
            } else if (vanillaVersion.getName().equalsIgnoreCase(versionToCheck)) {
                oldIndex = i;
            }
        }

        if (newIndex == -1) {
            throw new IllegalArgumentException("Version " + version + " not found");
        } else if (oldIndex == -1) {
            throw new IllegalArgumentException("Version " + versionToCheck + " not found");
        }

        return newIndex > oldIndex;
    }

    @Override
    protected VanillaVersion.Container parse(HttpResponse response) {
        Gson gson = new Gson();
        return gson.fromJson(response.getContentAsString(), VanillaVersion.Container.class);
    }

}
