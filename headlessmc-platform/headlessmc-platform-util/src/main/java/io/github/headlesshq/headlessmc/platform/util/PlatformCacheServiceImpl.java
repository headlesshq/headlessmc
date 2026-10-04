package io.github.headlesshq.headlessmc.platform.util;

import io.github.headlesshq.headlessmc.files.AppFiles;
import io.github.headlesshq.headlessmc.files.FileService;
import io.github.headlesshq.headlessmc.files.cache.*;
import io.github.headlesshq.headlessmc.net.DownloadService;
import io.github.headlesshq.headlessmc.reflection.ReflectionRegistered;
import io.github.headlesshq.headlessmc.util.json.JsonService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.util.TypeLiteral;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;

import java.net.URI;
import java.util.List;

@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class PlatformCacheServiceImpl implements PlatformCacheService {
    private final DownloadService downloadService;
    private final FileService fileService;
    private final AppFiles files;
    private final JsonService jsonService;

    //@Produces
    //@ApplicationScoped
    //public <J> Cache<J> createCache(InjectionPoint point) {
    // if we implemented PlatformID we could actually do this and e.g.
    // inject @Vanilla Cache<VanillaManifest>
    // by getting @Vanilla from the point and looking up a list of PlatformIDs
    // for the one that has the Vanilla.Literal
    //}

    @Override
    public <J extends ReflectionRegistered> Cache<J> versionCache(TypeLiteral<J> type, String platformName, URI url) {
        return versionCache(type, platformName, url, 0);
    }

    @Override
    public <J extends ReflectionRegistered> Cache<J> versionCache(
        TypeLiteral<J> type,
        String platformName,
        URI url,
        long version
    ) {
        return versionCacheInternal(type, platformName, url, version);
    }

    @Override
    public <J extends ReflectionRegistered> Cache<List<J>> versionCacheList(
        TypeLiteral<List<J>> type,
        String platformName,
        URI url,
        long version
    ) {
        return versionCacheInternal(type, platformName, url, version);
    }

    private <J> Cache<J> versionCacheInternal(
        TypeLiteral<J> type,
        String platformName,
        URI url,
        long version
    ) {
        return CacheBuilder.<J>create()
            .withVersion(version)
            .withSource(
                new JsonResource<>(
                    CacheExceptionHandler.logging(),
                    jsonService,
                    type,
                    JsonResource.getDefaultClassLoader(),
                    platformName + "/" + platformName + "-versions.json"
                )
            ).withSourceStore(
                new JsonCacheFile<>(
                    CacheExceptionHandler.logging(),
                    jsonService,
                    type,
                    0,
                    fileService.getPath(files.getCacheDir(), platformName + "-versions.json")
                )
            ).withSource(
                new HttpResource<>(
                    CacheExceptionHandler.throwing(),
                    downloadService,
                    downloadBuilder -> downloadBuilder.jsonRegisteredForReflection(type),
                    url
                )
            ).build();
    }

}
