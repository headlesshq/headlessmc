package io.github.headlesshq.headlessmc.platform.forge;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.exceptions.HeadlessMcIOException;
import io.github.headlesshq.headlessmc.platform.AbstractJarEntryModReader;
import io.github.headlesshq.headlessmc.platform.mods.Mod;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.annotation.Nullable;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.toml.TomlMapper;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Parses forge.mods.toml files. E.g.:
 * <pre>
 * {@code
 * modLoader = "javafml"
 * loaderVersion = "[1,)"
 * issueTrackerURL = "https://github.com/headlesshq/headlessmc"
 * license = "MIT"
 *
 * [[mods]]
 * modId = "headlessmc"
 * version = "1.0.0"
 * displayName = "HeadlessMc"
 * authors = "3arthqu4ke"
 * description = '''
 *     A Minecraft Launcher
 * '''
 * }
 * </pre>
 */
@Forge
@Priority(100)
@ApplicationScoped
public class ForgeModsTomlReader extends AbstractJarEntryModReader {
    private final ObjectMapper mapper;

    public ForgeModsTomlReader(ObjectMapper mapper, String... entries) {
        super(entries);
        this.mapper = mapper;
    }

    @Inject
    public ForgeModsTomlReader() {
        this(
            TomlMapper.builder().enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY).build(),
            "META-INF/forge.mods.toml"
        );
    }

    @Override
    public Optional<List<Mod>> readEntry(InputStream inputStream) throws HeadlessMcException {
        try {
            ForgeModsToml modsToml = mapper.readValue(inputStream, ForgeModsToml.class);
            return Optional.of(modsToml.mods.stream()
                .map(entry -> new Mod(
                    entry.modId,
                    entry.displayName == null ? entry.modId : entry.displayName,
                    Optional.ofNullable(entry.description),
                    entry.authors,
                    Map.of() // TODO: this
                )).toList());
        } catch (JacksonException e) {
            throw new HeadlessMcIOException(e);
        }
    }

    @RegisterForReflection
    private record ForgeModsToml(List<ModEntry> mods) {}

    @RegisterForReflection
    private record ModEntry(
        String modId,
        @Nullable String displayName,
        @Nullable String description,
        List<String> authors
    ) {}

}
