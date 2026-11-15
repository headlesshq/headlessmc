package io.github.headlesshq.headlessmc.launcher.client;

import io.github.headlesshq.headlessmc.auth.Account;
import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.files.FileService;
import io.github.headlesshq.headlessmc.files.McFiles;
import io.github.headlesshq.headlessmc.files.McFilesService;
import io.github.headlesshq.headlessmc.launcher.process.LaunchException;
import io.github.headlesshq.headlessmc.launcher.process.ProcessLauncher;
import io.github.headlesshq.headlessmc.launcher.profile.AbstractLaunchTarget;
import io.github.headlesshq.headlessmc.launcher.profile.ArgCandidate;
import io.github.headlesshq.headlessmc.launcher.profile.GameDirService;
import io.github.headlesshq.headlessmc.launcher.profile.ParsedTarget;
import io.github.headlesshq.headlessmc.launcher.profile.PathCandidates;
import io.github.headlesshq.headlessmc.launcher.profile.Profile;
import io.github.headlesshq.headlessmc.launcher.profile.TargetArgs;
import io.github.headlesshq.headlessmc.launcher.profile.TargetInfo;
import io.github.headlesshq.headlessmc.platform.VersionID;
import io.github.headlesshq.headlessmc.reflection.ReflectionRegistered;
import io.github.headlesshq.headlessmc.util.json.JsonService;
import io.github.headlesshq.headlessmc.util.typemap.TypedMap;
import io.github.headlesshq.headlessmc.version.Version;
import io.github.headlesshq.headlessmc.version.arg.Side;
import io.github.headlesshq.headlessmc.version.arg.VersionArg;
import io.github.headlesshq.headlessmc.version.service.VersionJsonService;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * The {@value #NAME} launch target: launches a client directly from a version.json file,
 * e.g. a version another launcher installed under a name that is not a version, or a file outside of every
 * {@code .minecraft} directory.
 *
 * <p>Arguments: {@code <path/to/version.json | id>}, e.g. {@code ~/.minecraft/versions/my-pack/my-pack.json}
 * or {@code my-pack}. The argument is the path of an existing version.json, or the id of a version that is
 * installed in one of the {@code .minecraft} directories, the first directory that has it is used.
 * Either way the profile remembers the file.</p>
 *
 * <p>An id is only a {@link ParsedTarget#fallback() fallback}: {@code 1.21.1} is first of all a Minecraft
 * version, launched by the {@value io.github.headlesshq.headlessmc.launcher.profile.VersionHandle#VERSION}
 * target, also if a version.json with that id is installed. An id no platform knows, like
 * {@code my-pack}, or one that is not a version by itself, like {@code fabric-loader-0.16.9-1.21.1},
 * is launched by this target. With {@code --target version-file} every installed id is.</p>
 *
 * <p>Libraries and assets are installed into the {@code .minecraft} directory the file is in,
 * see {@link McFilesService#require(Path)}. If a platform recognizes the version, it is used,
 * see {@link ClientLauncher#matchVersionId(Version)}.</p>
 */
@Slf4j
@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class VersionFileLaunchTarget extends AbstractLaunchTarget<VersionFileLaunchTarget.Handle> {
    /**
     * The name of this launch target.
     */
    public static final String NAME = "version-file";
    private static final String JSON = ".json";

    private final VersionJsonService versionJsonService;
    private final McFilesService mcFilesService;
    private final GameDirService gameDirService;
    private final ClientLauncher clientLauncher;
    private final DefaultJvmArgsService defaultJvmArgsService;
    private final FileService fileService;
    private final JsonService jsonService;

    @Override
    protected JsonService jsonService() {
        return jsonService;
    }

    @Override
    protected Class<Handle> handleType() {
        return Handle.class;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getUsage() {
        return "<path/to/version.json | id of an installed version>";
    }

    @Override
    public ParsedTarget parse(TargetArgs args) throws IllegalArgumentException {
        if (args.args().size() != 1) {
            throw new IllegalArgumentException("expected the path of one version.json file, or the id of an installed version");
        }

        if (!args.sides().contains(Side.CLIENT)) {
            throw new IllegalArgumentException("a version.json launches a client");
        }

        String arg = args.args().getFirst();
        Path file;
        try {
            file = fileService.getUserPath(arg).toAbsolutePath().normalize();
        } catch (InvalidPathException e) {
            throw new IllegalArgumentException("'" + arg + "' is not a path", e);
        }

        boolean json = PathCandidates.hasExtension(file, JSON);
        if (!json || !Files.isRegularFile(file)) {
            // not a file: the id of an installed version, like other launchers and older versions of HeadlessMc take
            Optional<Path> installed = findInstalled(arg);
            if (installed.isPresent()) {
                return new ParsedTarget(handle(new Handle(installed.get())), Optional.of(Side.CLIENT), true);
            }

            throw new IllegalArgumentException(
                json
                    ? "file " + file + " does not exist"
                    : "'" + arg + "' is neither a .json file nor the id of an installed version"
            );
        }

        parse(file);
        return new ParsedTarget(handle(new Handle(file)), Optional.of(Side.CLIENT));
    }

    /**
     * Finds the version.json of an installed version by its id.
     * A version is installed as {@code versions/<id>/<id>.json}, so the file with that name is looked at first.
     * A version.json that has been copied or renamed by hand can have another id than its file says,
     * these are found by reading the installed versions.
     * The {@code .minecraft} directories are searched in their order, the primary directory first.
     */
    private Optional<Path> findInstalled(String id) {
        if (id.isBlank() || id.contains("/") || id.contains("\\")) {
            return Optional.empty();
        }

        List<McFiles> available = mcFilesService.all().stream().filter(McFiles::isAvailable).toList();
        for (McFiles mcFiles : available) {
            try {
                Optional<Path> file = versionJsonService.tryGetVersion(mcFiles, id)
                    .filter(version -> id.equals(version.getId()))
                    .flatMap(Version::asFile);
                if (file.isPresent()) {
                    return file;
                }
            } catch (HeadlessMcException | InvalidPathException e) {
                log.debug("Failed to look for version {} in {}", id, mcFiles.getMcDir(), e);
            }
        }

        for (McFiles mcFiles : available) {
            try {
                Optional<Path> file = versionJsonService.getInstalledVersions(mcFiles).stream()
                    .filter(version -> id.equals(version.getId()))
                    .findFirst()
                    .flatMap(Version::asFile);
                if (file.isPresent()) {
                    return file;
                }
            } catch (HeadlessMcException e) {
                log.debug("Failed to list versions in {}", mcFiles.getMcDir(), e);
            }
        }

        return Optional.empty();
    }

    @Override
    public List<ArgCandidate> complete(TargetArgs args) {
        if (args.args().size() > 1 || !args.sides().contains(Side.CLIENT)) {
            return List.of();
        }

        String typed = args.args().isEmpty() ? "" : args.args().getFirst();
        List<ArgCandidate> result = new ArrayList<>(PathCandidates.complete(typed, PathCandidates.extension(JSON)));
        // what this is mostly used for: versions that are installed, but not under the name of a version.
        // This only lists directories, the versions are not read.
        Set<String> ids = new LinkedHashSet<>();
        for (McFiles mcFiles : mcFilesService.all()) {
            try {
                ids.addAll(versionJsonService.getInstalledFileNames(mcFiles));
            } catch (HeadlessMcException e) {
                log.debug("Failed to list versions in {}", mcFiles.getMcDir(), e);
            }
        }

        ids.forEach(id -> result.add(new ArgCandidate(id, "installed version")));
        return result;
    }

    @Override
    public TargetInfo describe(Profile profile) {
        Handle handle = handle(profile);
        Optional<VersionArg> version = Optional.empty();
        if (Files.isRegularFile(handle.file())) {
            try {
                // matches are cached, this does not parse the hierarchy every time
                version = clientLauncher.matchVersionId(versionJsonService.getParser().parse(handle.file()))
                    .map(VersionID::asArg);
            } catch (HeadlessMcException e) {
                log.debug("Failed to read {}", handle.file(), e);
            }
        }

        return new TargetInfo(Optional.empty(), version, Optional.of(handle.file()));
    }

    @Override
    public String getLabel(Profile profile) {
        return NAME + ":" + handle(profile).file().getFileName();
    }

    @Override
    public Profile create(String name, ParsedTarget target, TypedMap options) {
        Handle handle = handle(target.handle());
        Path path = options.get(GAME_DIR);
        if (path == null) {
            // with the layout of other launchers the game directory of a version.json is the directory it is in
            path = gameDirService.getGameDir(handle.file(), isModded(handle.file())).orElse(null);
        }

        if (path == null) {
            // like a version of that name: a game directory in the .minecraft directory of the file
            String id = handle.file().getFileName().toString();
            id = id.substring(0, id.length() - JSON.length());
            path = gameDirService.getGameDir(
                VersionArg.builder().version(id).build(),
                options.get(MC_FILES, mcFilesService.require(handle.file()))
            );
        }

        return new Profile(name, Optional.of(Side.CLIENT), handle(handle), path);
    }

    @Override
    public Profile resolve(Profile profile, TypedMap options) throws HeadlessMcException {
        Handle handle = validate(profile);
        if (!Files.isRegularFile(handle.file())) {
            McFiles mcFiles = mcFilesService.require(handle.file());
            throw new LaunchException(
                "The version.json " + handle.file() + " of profile " + profile.name() + " does not exist"
                    + (mcFiles.isAvailable() ? "." : ", is the disk of " + mcFiles.getMcDir() + " mounted?")
            );
        }

        if (options.get(NEW_PROFILE, false)) {
            return defaultJvmArgsService.withDefaultJvmArgs(profile, parse(handle.file()));
        }

        return profile;
    }

    @Override
    public ProcessLauncher launcher(Profile profile, TypedMap options) throws HeadlessMcException {
        Handle handle = validate(profile);
        Account account = options.get(ClientLauncher.ACCOUNT);
        if (account == null) {
            throw new LaunchException("No account to launch profile " + profile.name() + " with");
        }

        Version version = versionJsonService.getParser().parse(handle.file());
        return clientLauncher.launcher(profile, account, version, clientLauncher.matchVersionId(version).orElse(null));
    }

    /**
     * A version.json is only known to be vanilla if it is recognized as a vanilla version.
     * Anything else, also what cannot be read or recognized, is treated as modded:
     * it gets a game directory of its own instead of sharing one.
     */
    private boolean isModded(Path file) {
        try {
            return clientLauncher.matchVersionId(versionJsonService.getParser().parse(file))
                .map(id -> !id.isVanilla())
                .orElse(true);
        } catch (HeadlessMcException e) {
            log.debug("Failed to read {}", file, e);
            return true;
        }
    }

    private Handle validate(Profile profile) throws LaunchException {
        if (!profile.isClient()) {
            throw new LaunchException(
                "Profile " + profile.name() + " is invalid: it has launch target " + NAME + ", but is not a client"
            );
        }

        return handle(profile);
    }

    /**
     * @throws IllegalArgumentException if the file is not a version.json.
     */
    private Version parse(Path file) throws IllegalArgumentException {
        Version version;
        try {
            version = versionJsonService.getParser().parse(file);
        } catch (HeadlessMcException e) {
            throw new IllegalArgumentException(file + " is not a version.json: " + e.getMessage(), e);
        }

        //noinspection ConstantValue
        if (version.getId() == null) {
            throw new IllegalArgumentException(file + " is not a version.json, it has no id");
        }

        return version;
    }

    /**
     * The data of the {@value #NAME} launch target.
     *
     * @param file the version.json file that is launched.
     */
    @RegisterForReflection
    public record Handle(Path file) implements ReflectionRegistered {
        @SuppressWarnings("ConstantValue")
        public Handle {
            Objects.requireNonNull(file, "Launch target data is missing 'file'");
        }
    }

}
