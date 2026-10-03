package io.github.headlesshq.headlessmc.platform;

import io.github.headlesshq.headlessmc.platform.client.ClientSupport;
import io.github.headlesshq.headlessmc.platform.mods.ModSupport;
import io.github.headlesshq.headlessmc.platform.server.ServerSupport;
import io.github.headlesshq.headlessmc.util.maven.MavenRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.SequencedSet;

/**
 * A configurable {@link Platform} for tests.
 */
public class FakePlatform implements Platform {
    private final String name;
    private final VersionService versionService;
    private final List<MavenRepository> repositories = new ArrayList<>();
    private Optional<ServerSupport> serverSupport = Optional.empty();
    private Optional<ClientSupport> clientSupport = Optional.empty();
    private Optional<ModSupport> modSupport = Optional.empty();

    public FakePlatform(String name, VersionService versionService) {
        this.name = name;
        this.versionService = versionService;
    }

    /**
     * @param name          the platform name.
     * @param buildsPerMcVersion pairs of [mcVersion, build...] arrays, builds latest-first.
     * @return a FakePlatform with a {@link FakeVersionService}.
     */
    public static FakePlatform create(String name, String[]... buildsPerMcVersion) {
        FakeVersionService service = new FakeVersionService(name);
        for (String[] entry : buildsPerMcVersion) {
            String[] builds = new String[entry.length - 1];
            System.arraycopy(entry, 1, builds, 0, builds.length);
            service.withBuilds(entry[0], builds);
        }

        return new FakePlatform(name, service);
    }

    public FakePlatform withServerSupport(ServerSupport support) {
        this.serverSupport = Optional.of(support);
        return this;
    }

    public FakePlatform withClientSupport(ClientSupport support) {
        this.clientSupport = Optional.of(support);
        return this;
    }

    public FakePlatform withModSupport(ModSupport support) {
        this.modSupport = Optional.of(support);
        return this;
    }

    public FakePlatform withRepository(MavenRepository repository) {
        this.repositories.add(repository);
        return this;
    }

    public BoundPlatformVersion version(String vanillaVersion, String build) {
        return new BoundPlatformVersion(name, vanillaVersion, build);
    }

    public SequencedSet<? extends PlatformVersion> versions() {
        return versionService.getVersions();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getCapitalizedName() {
        return name.substring(0, 1).toUpperCase(Locale.ENGLISH) + name.substring(1);
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
        return serverSupport;
    }

    @Override
    public Optional<ClientSupport> getClientSupport() {
        return clientSupport;
    }

    @Override
    public Optional<ModSupport> getModSupport() {
        return modSupport;
    }

}
