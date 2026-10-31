package io.github.headlesshq.headlessmc.quilt;

import io.github.headlesshq.headlessmc.platform.Platform;
import io.github.headlesshq.headlessmc.platform.VersionService;
import io.github.headlesshq.headlessmc.platform.client.ClientSupport;
import io.github.headlesshq.headlessmc.platform.mods.ModSupport;
import io.github.headlesshq.headlessmc.platform.server.ServerSupport;
import io.github.headlesshq.headlessmc.util.maven.MavenRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.util.List;
import java.util.Optional;

/**
 * The Quilt platform, every {@code Platform} bean is picked up by HeadlessMc,
 * so {@code launch quilt/1.21.1} and {@code server add quilt/1.21.1} work once this plugin is installed.
 */
@Quilt
@ApplicationScoped
@Named(Quilt.PLATFORM_NAME)
public class QuiltPlatform implements Platform {
    private final List<MavenRepository> repositories;
    private final VersionService versionService;
    private final ClientSupport clientSupport;
    private final ServerSupport serverSupport;
    private final ModSupport modSupport;

    @Inject
    public QuiltPlatform(
        @Quilt Instance<MavenRepository> repositories,
        @Quilt VersionService versionService,
        @Quilt ClientSupport clientSupport,
        @Quilt ServerSupport serverSupport,
        @Quilt ModSupport modSupport
    ) {
        this.repositories = repositories.stream().toList();
        this.versionService = versionService;
        this.clientSupport = clientSupport;
        this.serverSupport = serverSupport;
        this.modSupport = modSupport;
    }

    @Override
    public String getName() {
        return Quilt.PLATFORM_NAME;
    }

    @Override
    public String getCapitalizedName() {
        return Quilt.CAPITALIZED_PLATFORM_NAME;
    }

    @Override
    public VersionService getVersionService() {
        return versionService;
    }

    @Override
    public List<MavenRepository> getRepositories() {
        return repositories;
    }

    @Override
    public Optional<ServerSupport> getServerSupport() {
        return Optional.of(serverSupport);
    }

    @Override
    public Optional<ClientSupport> getClientSupport() {
        return Optional.of(clientSupport);
    }

    @Override
    public Optional<ModSupport> getModSupport() {
        return Optional.of(modSupport);
    }

}
