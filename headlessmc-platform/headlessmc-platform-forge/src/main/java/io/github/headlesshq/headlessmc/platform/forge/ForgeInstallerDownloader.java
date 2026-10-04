package io.github.headlesshq.headlessmc.platform.forge;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.exceptions.NotFoundException;
import io.github.headlesshq.headlessmc.exceptions.RequestException;
import io.github.headlesshq.headlessmc.exceptions.UncheckedInterruptedException;
import io.github.headlesshq.headlessmc.files.cache.Cache;
import io.github.headlesshq.headlessmc.platform.PlatformVersion;
import io.github.headlesshq.headlessmc.platform.VersionID;
import io.github.headlesshq.headlessmc.platform.util.CommonInstallerServices;
import io.github.headlesshq.headlessmc.util.maven.Artifact;
import io.github.headlesshq.headlessmc.util.maven.MavenRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Forge
@ApplicationScoped
@RequiredArgsConstructor
public class ForgeInstallerDownloader {
    private final ForgeArtifactResolver artifactResolver;
    private final CommonInstallerServices services;
    private final MavenRepository repository;
    private final Cache<PrismIndex> cache;
    private final String platformName;

    @Inject
    public ForgeInstallerDownloader(
        @Forge ForgeArtifactResolver artifactResolver,
        CommonInstallerServices services,
        @Forge MavenRepository repository,
        @Forge Cache<PrismIndex> cache
    ) {
        this(artifactResolver, services, repository, cache, Forge.PLATFORM_NAME);
    }

    public Path downloadInstaller(VersionID id) throws HeadlessMcException {
        PrismIndex index = cache.get().orElseThrow(() -> new RequestException("Failed to get versions"));
        PrismIndex.Meta meta = getMeta(index, id);
        Artifact artifact = artifactResolver.resolve(platformName, index, meta, "installer");
        Path path = artifact.jar(services.getFileService().getPath(services.getAppFiles().getCacheDir(), "forge"));
        if (Files.exists(path)) {
            return path;
        }

        URI url = repository.getJar(artifact);
        // TODO: can we obtain hash?
        // TODO: PrismIndex meta has hash!!!
        try {
            services.getDownloadService().download(url).toFile(path);
        } catch (UncheckedInterruptedException e) {
            throw e;
        } catch (HeadlessMcException e) {
            // TODO: json with list of exceptions!
            artifact = new Artifact(artifact.group(), artifact.name(), artifact.version() + "-" + meta.getMcVersion(), artifact.classifier());
            path = artifact.jar(services.getFileService().getPath(services.getAppFiles().getCacheDir(), "forge"));
            if (Files.exists(path)) {
                return path;
            }

            url = repository.getJar(artifact);
            services.getDownloadService().download(url).toFile(path);
        }

        return path;
    }

    private PrismIndex.Meta getMeta(PrismIndex index, VersionID id) throws HeadlessMcException {
        List<PrismIndex.Meta> candidates = new ArrayList<>(1);
        for (PrismIndex.Meta meta : index.versions()) {
            if (id.getVersion().getName().equals(meta.getMcVersion())
                && meta.version().contains(id.getBuild().map(PlatformVersion::getName).orElse(""))) {
                candidates.add(meta);
            }
        }

        if (candidates.isEmpty()) {
            throw new NotFoundException("Failed to find version " + id + " in index " + index);
        }

        if (candidates.size() == 1 || id.getBuild().isEmpty()) {
            return candidates.getFirst();
        }

        // we checked if the meta.version contains the platform version, sort by longest match
        candidates.sort(Comparator.comparingInt(meta -> -meta.version().length()));
        if (candidates.get(0).version().length() == candidates.get(1).version().length()) {
            throw new NotFoundException("Ambiguous versions for " + id + " in " + candidates);
        }

        return candidates.getFirst();
    }

}
