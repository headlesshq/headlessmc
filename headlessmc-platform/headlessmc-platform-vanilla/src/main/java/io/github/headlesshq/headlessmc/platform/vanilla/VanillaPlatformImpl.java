package io.github.headlesshq.headlessmc.platform.vanilla;

import io.github.headlesshq.headlessmc.platform.*;
import io.github.headlesshq.headlessmc.platform.client.ClientSupport;
import io.github.headlesshq.headlessmc.platform.mods.ModSupport;
import io.github.headlesshq.headlessmc.platform.server.ServerFinder;
import io.github.headlesshq.headlessmc.platform.server.ServerSupport;
import io.github.headlesshq.headlessmc.util.maven.MavenRepository;
import io.github.headlesshq.headlessmc.version.arg.Side;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Default implementation of the {@link Vanilla} platform.
 */
@Getter
@Default
@Vanilla
@ApplicationScoped
@RequiredArgsConstructor
@Named(Vanilla.PLATFORM_NAME)
public class VanillaPlatformImpl implements VanillaPlatform, Platform {
    private final String name;
    private final String capitalizedName;
    private final List<MavenRepository> repositories;
    private final Set<Side> supportedSides;
    private final VanillaVersionService versionService;
    private final VanillaInstaller installer;
    private final ClientSupport clientSupport;
    private final ServerSupport serverSupport;

    @Inject
    public VanillaPlatformImpl(
        @Vanilla ServerFinder serverFinder,
        @Vanilla VanillaVersionService versionService,
        @Vanilla Instance<MavenRepository> repositories,
        VanillaInstaller installer,
        @Vanilla ClientSupport clientSupport,
        @Vanilla ServerSupport serverSupport
    ) {
        this(
            Vanilla.PLATFORM_NAME,
            Vanilla.CAPITALIZED_PLATFORM_NAME,
            repositories.stream().toList(),
            Side.BOTH,
            versionService,
            installer,
            clientSupport,
            serverSupport
        );
    }

    @Override
    public VanillaInstaller getInstaller() {
        return installer;
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
        return Optional.empty();
    }

}
