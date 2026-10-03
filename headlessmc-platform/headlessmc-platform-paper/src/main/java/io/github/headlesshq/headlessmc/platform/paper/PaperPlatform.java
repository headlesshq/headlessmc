package io.github.headlesshq.headlessmc.platform.paper;

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
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;

/**
 * Implementation of the {@link Paper} platform.
 */
@Paper
@Getter
@ApplicationScoped
@RequiredArgsConstructor
@Named(Paper.PLATFORM_NAME)
public class PaperPlatform implements Platform {
    private final String name;
    private final String capitalizedName;
    private final List<MavenRepository> repositories;
    private final VersionService versionService;
    private final ServerSupport serverSupport;
    private final ModSupport modSupport;

    @Inject
    public PaperPlatform(
        @Paper Instance<MavenRepository> repositories,
        @Paper VersionService versionService,
        @Paper ServerSupport serverSupport,
        @Paper ModSupport modSupport
    ) {
        this(
            Paper.PLATFORM_NAME,
            Paper.CAPITALIZED_PLATFORM_NAME,
            repositories.stream().toList(),
            versionService,
            serverSupport,
            modSupport
        );
    }

    @Override
    public Optional<ServerSupport> getServerSupport() {
        return Optional.of(serverSupport);
    }

    @Override
    public Optional<ClientSupport> getClientSupport() {
        return Optional.empty();
    }

    @Override
    public Optional<ModSupport> getModSupport() {
        return Optional.of(modSupport);
    }

}
