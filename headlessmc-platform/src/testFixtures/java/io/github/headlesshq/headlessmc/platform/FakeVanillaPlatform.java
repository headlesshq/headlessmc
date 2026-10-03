package io.github.headlesshq.headlessmc.platform;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.platform.client.ClientSupport;
import io.github.headlesshq.headlessmc.platform.mods.ModSupport;
import io.github.headlesshq.headlessmc.platform.server.ServerSupport;
import io.github.headlesshq.headlessmc.util.maven.MavenRepository;
import io.github.headlesshq.headlessmc.util.typemap.TypedMap;
import io.github.headlesshq.headlessmc.version.FakeVersion;
import io.github.headlesshq.headlessmc.version.Version;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A {@link VanillaPlatform} over a {@link FakeVanillaVersionService}.
 * The installer resolves {@link Version}s registered via
 * {@link #withVersion(Version)}, defaulting to an empty {@link FakeVersion}.
 */
public class FakeVanillaPlatform implements VanillaPlatform {
    private final FakeVanillaVersionService versionService;
    private final Map<String, Version> versionJsons = new LinkedHashMap<>();
    private final List<VersionID> installedClients = new ArrayList<>();
    private final List<VersionID> installedServers = new ArrayList<>();
    private Optional<ServerSupport> serverSupport = Optional.empty();
    private Optional<ClientSupport> clientSupport = Optional.empty();
    private Optional<ModSupport> modSupport = Optional.empty();
    private int serverJavaVersion = 21;

    public FakeVanillaPlatform(String... versionsLatestFirst) {
        this.versionService = new FakeVanillaVersionService(versionsLatestFirst);
    }

    public FakeVanillaPlatform withVersion(Version version) {
        versionJsons.put(version.getId(), version);
        return this;
    }

    public FakeVanillaPlatform withServerSupport(ServerSupport support) {
        this.serverSupport = Optional.of(support);
        return this;
    }

    public FakeVanillaPlatform withClientSupport(ClientSupport support) {
        this.clientSupport = Optional.of(support);
        return this;
    }

    public FakeVanillaPlatform withModSupport(ModSupport support) {
        this.modSupport = Optional.of(support);
        return this;
    }

    public VanillaVersion version(String name) {
        return versionService.getVersion(name).orElseThrow(
            () -> new IllegalArgumentException("Unknown version " + name));
    }

    public List<VersionID> installedClients() {
        return installedClients;
    }

    public List<VersionID> installedServers() {
        return installedServers;
    }

    @Override
    public String getName() {
        return Vanilla.PLATFORM_NAME;
    }

    @Override
    public String getCapitalizedName() {
        return Vanilla.CAPITALIZED_PLATFORM_NAME;
    }

    @Override
    public FakeVanillaVersionService getVersionService() {
        return versionService;
    }

    @Override
    public List<MavenRepository> getRepositories() {
        return List.of();
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

    @Override
    public VanillaInstaller getInstaller() {
        return new VanillaInstaller() {
            @Override
            public Version getVersion(VanillaVersion version) {
                return versionJsons.computeIfAbsent(version.getName(), FakeVersion::new);
            }

            @Override
            public Version installClient(VersionID id, Path mcDir, TypedMap args) throws HeadlessMcException {
                installedClients.add(id);
                return getVersion(id.getVersion());
            }

            @Override
            public Installation installServer(VersionID id, Path dir, TypedMap args) throws HeadlessMcException {
                installedServers.add(id);
                return new Installation(serverJavaVersion);
            }
        };
    }

}
