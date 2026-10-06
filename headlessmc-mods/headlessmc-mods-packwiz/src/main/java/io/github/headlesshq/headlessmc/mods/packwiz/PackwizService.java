package io.github.headlesshq.headlessmc.mods.packwiz;

import io.github.headlesshq.headlessmc.exceptions.BadArgumentException;
import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.exceptions.HeadlessMcIOException;
import io.github.headlesshq.headlessmc.platform.PlatformService;
import io.github.headlesshq.headlessmc.platform.VersionID;
import io.github.headlesshq.headlessmc.version.arg.VersionArg;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.toml.TomlMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Reads the {@code pack.toml} of a <a href="https://packwiz.infra.link">packwiz</a> modpack,
 * to find the version the modpack is for.
 *
 * @see PackwizToml
 */
@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class PackwizService {
    private final PlatformService platformService;
    private final ObjectMapper mapper;

    @Inject
    public PackwizService(PlatformService platformService) {
        this(platformService, TomlMapper.builder().build());
    }

    /**
     * Parses a pack.toml file and resolves the versions it targets.
     *
     * @param toml the file.
     * @return a VersionID for every loader in the {@code [versions]} table,
     * in the order they are defined, or a single vanilla VersionID if no loader is defined.
     * @throws HeadlessMcException if the toml could not be parsed, or a version could not be resolved.
     */
    public List<VersionID> parsePackwizTomlFile(Path toml) throws HeadlessMcException {
        try {
            return parsePackwizToml(Files.readString(toml));
        } catch (IOException e) {
            throw new HeadlessMcIOException("Failed to read " + toml, e);
        }
    }

    /**
     * Parses a pack.toml and resolves the versions it targets.
     *
     * @param toml the content of the pack.toml.
     * @return a VersionID for every loader in the {@code [versions]} table,
     * in the order they are defined, or a single vanilla VersionID if no loader is defined.
     * @throws HeadlessMcException if the toml could not be parsed, or a version could not be resolved.
     */
    public List<VersionID> parsePackwizToml(String toml) throws HeadlessMcException {
        return toVersionArgs(readPackwizToml(toml))
            .stream()
            .map(arg -> VersionID.resolve(platformService, arg))
            .toList();
    }

    private PackwizToml readPackwizToml(String toml) throws HeadlessMcException {
        try {
            return mapper.readValue(toml, PackwizToml.class);
        } catch (JacksonException e) {
            throw new HeadlessMcIOException("Failed to parse packwiz toml", e);
        }
    }

    /**
     * Converts the {@link PackwizToml#versions()} into {@link VersionArg}s.
     * The packwiz format stores versions as a free {@code component -> version} map,
     * so multiple loaders can be defined, e.g. {@code quilt} and {@code fabric}.
     * Loaders that are not supported by the {@link PlatformService} are skipped.
     *
     * @param packwizToml the parsed pack.toml.
     * @return a VersionArg for each supported loader of the modpack, or a vanilla VersionArg if there are none.
     * @throws BadArgumentException if no minecraft version is specified,
     *                              or loaders were specified, but none of them are supported.
     */
    private List<VersionArg> toVersionArgs(PackwizToml packwizToml) throws BadArgumentException {
        Map<String, String> versions = packwizToml.versions() == null ? Map.of() : packwizToml.versions();
        String mcVersion = versions.get(PackwizToml.MINECRAFT);
        if (mcVersion == null) {
            throw new BadArgumentException("No minecraft version specified in packwiz toml");
        }

        List<Map.Entry<String, String>> loaders = versions.entrySet()
            .stream()
            .filter(entry -> !PackwizToml.MINECRAFT.equals(entry.getKey()))
            .toList();

        if (loaders.isEmpty()) {
            return List.of(new VersionArg(Optional.empty(), VersionArg.PLATFORM_VANILLA, mcVersion, Optional.empty()));
        }

        List<VersionArg> result = new ArrayList<>(loaders.size());
        for (Map.Entry<String, String> loader : loaders) {
            if (platformService.getPlatform(loader.getKey()).isEmpty()) {
                log.warn("Skipping unknown loader {} {} in packwiz toml", loader.getKey(), loader.getValue());
                continue;
            }

            result.add(new VersionArg(Optional.empty(), loader.getKey(), mcVersion, Optional.of(loader.getValue())));
        }

        if (result.isEmpty()) {
            throw new BadArgumentException("None of the loaders in packwiz toml are supported: " + loaders);
        }

        return result;
    }

}
