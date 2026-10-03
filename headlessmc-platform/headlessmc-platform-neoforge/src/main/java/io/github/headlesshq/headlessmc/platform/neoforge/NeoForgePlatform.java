package io.github.headlesshq.headlessmc.platform.neoforge;

import io.github.headlesshq.headlessmc.platform.Platform;
import io.github.headlesshq.headlessmc.platform.VersionService;
import io.github.headlesshq.headlessmc.platform.client.ClientSupport;
import io.github.headlesshq.headlessmc.platform.mods.ModSupport;
import io.github.headlesshq.headlessmc.platform.server.ServerSupport;
import io.github.headlesshq.headlessmc.util.maven.MavenRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;

@Getter
@NeoForge
@ApplicationScoped
@RequiredArgsConstructor
public class NeoForgePlatform implements Platform {
    private final String name;
    private final String capitalizedName;
    private final List<MavenRepository> repositories;
    private final VersionService versionService;
    private final ClientSupport clientSupport;
    private final ServerSupport serverSupport;
    private final ModSupport modSupport;

    @Inject
    public NeoForgePlatform(
        @NeoForge Instance<MavenRepository> repositories,
        @NeoForge VersionService versionService,
        @NeoForge ClientSupport clientSupport,
        @NeoForge ServerSupport serverSupport,
        @NeoForge ModSupport modSupport
    ) {
        this(
            NeoForge.PLATFORM_NAME,
            NeoForge.CAPITALIZED_PLATFORM_NAME,
            repositories.stream().toList(),
            versionService,
            clientSupport,
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
        return Optional.of(clientSupport);
    }

    @Override
    public Optional<ModSupport> getModSupport() {
        return Optional.of(modSupport);
    }

}
