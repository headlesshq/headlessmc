package io.github.headlesshq.headlessmc.commands.version;

import io.github.headlesshq.headlessmc.console.cache.CachedConsole;
import io.github.headlesshq.headlessmc.console.Console;
import io.github.headlesshq.headlessmc.console.format.TableBuilder;
import io.github.headlesshq.headlessmc.console.format.TableProvider;
import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.platform.*;
import io.github.headlesshq.headlessmc.platform.client.VersionMatcherService;
import io.github.headlesshq.headlessmc.version.Version;
import io.github.headlesshq.headlessmc.version.arg.Side;
import io.github.headlesshq.headlessmc.version.service.VersionJsonService;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Default;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import picocli.CommandLine;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Getter
@Setter
@Default
@Dependent
@Named("command:version:list")
@CommandLine.Command(
    name = "list",
    aliases = "ls",
    mixinStandardHelpOptions = true,
    description = "Lists installed or remote versions."
)
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class ListCommand implements Runnable, CachedConsole.Enabled {
    private final VersionMatcherService versionMatcherService;
    private final VersionJsonService versionService;
    private final TableProvider tableProvider;
    private final PlatformService platforms;
    private final Console console;

    @CommandLine.Option(
        names = {"-t", "--type"},
        description = "Filters by a certain type of version (e.g. release, snapshot, ...)"
    )
    private @Nullable String type;

    @CommandLine.Option(
        names = {"-r", "--remote"},
        description = "Lists remote versions that can be installed",
        defaultValue = "false"
    )
    private boolean remote;

    @CommandLine.Parameters(
        description = "Search parameters (e.g. platform, version, build, ...)"
    )
    private List<String> parameters = new ArrayList<>();

    @Override
    public void run() {
        if (remote) {
            listRemoteVersions();
            return;
        }

        List<Version> versions = filterByParameters();
        if (type != null) {
            Set<String> availableTypes = versions.stream()
                .map(Version::getType)
                .filter(Objects::nonNull)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
            if (!availableTypes.contains(type.toLowerCase(Locale.ENGLISH))) {
                throw new IllegalArgumentException("Failed to find versions of type %s, available types: %s".formatted(
                    type, availableTypes
                ));
            }

            versions = versions.stream().filter(version -> type.equalsIgnoreCase(version.getType())).toList();
        }

        tableProvider.<Version>get()
            .withColumn("name", Version::getId)
            .withColumn("parent", Version::getInheritsFrom)
            .withColumn("type", Version::getType)
            .addAll(versions)
            .log(console::write);
    }

    private void listRemoteVersions() {
        VersionSearchParameter param = VersionParameterParser.parse(platforms, parameters);
        for (Platform platform : param.platforms()) {
            TableBuilder<String[]> table = tableProvider.<String[]>get()
                .withColumn("platform", s -> s[0])
                .withColumn("version", s -> s[1])
                .withColumn("build", s -> s[2]);
            VersionService versionService = platform.getVersionService();
            for (VanillaVersion version : param.versions()) {
                versionService.getBuilds(version).stream()
                    .filter(v -> param.other().isEmpty()
                        || param.other().stream().anyMatch(o -> v.toString().toLowerCase(Locale.ENGLISH).contains(o)))
                    .map(v -> new String[]{platform.getName(), version.getName(), v.getName()})
                    .forEach(table::add);
            }

            console.write(table.toString());
        }
    }

    private List<Version> filterByParameters() {
        List<String> parameters = this.parameters.stream().map(param -> param.toLowerCase(Locale.ENGLISH)).toList();
        List<Version> versions = new ArrayList<>(versionService.getInstalledVersions());
        if (parameters.isEmpty()) {
            return versions;
        }

        List<Version> result = new ArrayList<>(versions.size());
        for (Version version : versions) {
            List<@Nullable String> searchables = new ArrayList<>();
            searchables.add(version.getId());
            searchables.add(version.getType());
            try {
                Set<VersionID> ids = versionMatcherService.match(version, versionService);
                ids.forEach(id -> {
                    searchables.add(id.getPlatform().getName());
                    searchables.add(id.getVersion().getName());
                    id.getBuild().map(PlatformVersion::getName).ifPresent(searchables::add);
                    id.getSide().map(Side::name).ifPresent(searchables::add);
                });
            } catch (HeadlessMcException e) {
                log.error("Failed to match VersionIDs for {}", version.getId());
            }

            List<String> searchableStrings = searchables.stream()
                .filter(Objects::nonNull)
                .map(string -> string.toLowerCase(Locale.ENGLISH))
                .toList();

            if (parameters.stream()
                .allMatch(param -> searchableStrings.stream().anyMatch(string -> string.contains(param)))) {
                result.add(version);
            }
        }

        return result;
    }

}
