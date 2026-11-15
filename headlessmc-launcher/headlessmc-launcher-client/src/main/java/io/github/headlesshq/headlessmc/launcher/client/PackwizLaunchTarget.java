package io.github.headlesshq.headlessmc.launcher.client;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.files.FileService;
import io.github.headlesshq.headlessmc.launcher.process.LaunchException;
import io.github.headlesshq.headlessmc.launcher.process.ProcessLauncher;
import io.github.headlesshq.headlessmc.launcher.profile.AbstractLaunchTarget;
import io.github.headlesshq.headlessmc.launcher.profile.ArgCandidate;
import io.github.headlesshq.headlessmc.launcher.profile.LaunchTarget;
import io.github.headlesshq.headlessmc.launcher.profile.LaunchTargetHandle;
import io.github.headlesshq.headlessmc.launcher.profile.LaunchTargetService;
import io.github.headlesshq.headlessmc.launcher.profile.ParsedTarget;
import io.github.headlesshq.headlessmc.launcher.profile.PathCandidates;
import io.github.headlesshq.headlessmc.launcher.profile.Profile;
import io.github.headlesshq.headlessmc.launcher.profile.TargetArgs;
import io.github.headlesshq.headlessmc.launcher.profile.TargetInfo;
import io.github.headlesshq.headlessmc.launcher.profile.VersionArgCandidates;
import io.github.headlesshq.headlessmc.launcher.profile.VersionHandle;
import io.github.headlesshq.headlessmc.mods.packwiz.PackwizService;
import io.github.headlesshq.headlessmc.platform.Platform;
import io.github.headlesshq.headlessmc.platform.VersionID;
import io.github.headlesshq.headlessmc.reflection.ReflectionRegistered;
import io.github.headlesshq.headlessmc.util.json.JsonService;
import io.github.headlesshq.headlessmc.util.typemap.TypedMap;
import io.github.headlesshq.headlessmc.util.typemap.TypedMapImpl;
import io.github.headlesshq.headlessmc.version.arg.Side;
import io.github.headlesshq.headlessmc.version.arg.VersionArg;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;
import lombok.With;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The {@value #NAME} launch target: launches a packwiz modpack from its {@value #PACK_TOML}.
 * The pack decides the Minecraft version and the mod loader, this target launches that version
 * in the directory of the pack.
 *
 * <p>Arguments: {@code <pack.toml | directory> [client|server] [loader]}, e.g. {@code ~/packs/my-pack client}.
 * The first argument is a {@value #PACK_TOML}, or a directory containing one. It has to exist and to be parsed.
 * A pack can be launched as client and as server, so the side has to be given, except for commands that only
 * launch one side. If a pack supports multiple loaders, the loader has to be given as well.</p>
 *
 * <p>The pack is not launched by this target itself. What the pack says is turned into the arguments of the
 * {@value io.github.headlesshq.headlessmc.launcher.profile.VersionHandle#VERSION} or
 * {@value io.github.headlesshq.headlessmc.launcher.profile.VersionHandle#SERVER} launch target,
 * and resolving and launching is delegated to it. Its handle is kept in {@link Handle#resolved()},
 * so the version of a pack is pinned like any other version.
 * Every resolve reads the pack again, if the pack asks for another version, that version is resolved.</p>
 */
@Slf4j
@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class PackwizLaunchTarget extends AbstractLaunchTarget<PackwizLaunchTarget.Handle> {
    /**
     * The name of this launch target.
     */
    public static final String NAME = "packwiz";
    /**
     * The name of the file describing a packwiz modpack.
     */
    public static final String PACK_TOML = "pack.toml";

    private final LaunchTargetService launchTargetService;
    private final PackwizService packwizService;
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
        return "<pack.toml | directory> [client|server] [loader]";
    }

    @Override
    public ParsedTarget parse(TargetArgs args) throws IllegalArgumentException {
        List<String> values = args.args();
        if (values.isEmpty() || values.size() > 3) {
            throw new IllegalArgumentException("expected a " + PACK_TOML + " or a directory containing one");
        }

        Path file = getPackFile(values.getFirst());
        @Nullable Side side = null;
        @Nullable String platform = null;
        for (String value : values.subList(1, values.size())) {
            @Nullable Side valueSide = VersionArgCandidates.getSide(value);
            if (valueSide != null && side == null) {
                side = valueSide;
            } else if (platform == null) {
                platform = value;
            } else {
                throw new IllegalArgumentException("unexpected argument '" + value + "'");
            }
        }

        if (side == null) {
            if (args.isServerOnly()) {
                side = Side.SERVER;
            } else if (args.isClientOnly()) {
                side = Side.CLIENT;
            } else {
                throw new IllegalArgumentException(
                    "a pack can be launched as client or server, please specify which: " + getUsage()
                );
            }
        }

        VersionID id = getVersion(file, platform);
        LaunchTargetService.TargetMatch inner = parseInner(id, side);
        Handle handle = new Handle(file, id.getPlatform().getName(), inner.parsed().handle());
        return new ParsedTarget(handle(handle), Optional.of(side));
    }

    @Override
    public List<ArgCandidate> complete(TargetArgs args) {
        List<String> values = args.args();
        if (values.size() <= 1) {
            return PathCandidates.complete(
                values.isEmpty() ? "" : values.getFirst(),
                file -> PACK_TOML.equalsIgnoreCase(String.valueOf(file.getFileName()))
            );
        }

        if (values.size() == 2) {
            List<ArgCandidate> result = new ArrayList<>();
            for (Side side : Side.values()) {
                if (args.sides().contains(side)) {
                    result.add(new ArgCandidate(side.name().toLowerCase(Locale.ENGLISH)));
                }
            }

            return result;
        }

        return List.of();
    }

    @Override
    public TargetInfo describe(Profile profile) {
        Handle handle = handle(profile);
        if (handle.resolved() == null) {
            return TargetInfo.EMPTY;
        }

        return launchTargetService.describe(profile.withTarget(handle.resolved()));
    }

    @Override
    public Optional<VersionArg> resolveVersion(Profile profile) throws HeadlessMcException {
        Handle handle = handle(profile);
        if (handle.resolved() == null) {
            return Optional.empty();
        }

        Profile inner = profile.withTarget(handle.resolved());
        return launchTargetService.require(inner).resolveVersion(inner);
    }

    @Override
    public String getLabel(Profile profile) {
        Path dir = handle(profile).file().toAbsolutePath().getParent();
        return NAME + ":" + (dir == null || dir.getFileName() == null ? "" : dir.getFileName());
    }

    @Override
    public boolean isSameTarget(Profile stored, ParsedTarget parsed) {
        Handle storedHandle = handle(stored);
        Handle parsedHandle = handle(parsed.handle());
        return storedHandle.file().equals(parsedHandle.file())
            && Objects.equals(storedHandle.platform(), parsedHandle.platform());
    }

    @Override
    public Profile create(String name, ParsedTarget target, TypedMap options) {
        Handle handle = handle(target.handle());
        Side side = target.side().orElseThrow(() -> new IllegalArgumentException("A pack needs a side"));
        LaunchTargetHandle inner = Objects.requireNonNull(handle.resolved(), "Pack has not been parsed");
        // a pack is played in its own directory, everything else is what the launched version would get
        TypedMap innerOptions = copy(options);
        if (innerOptions.get(GAME_DIR) == null) {
            innerOptions.put(GAME_DIR, Objects.requireNonNull(handle.file().getParent(), "Pack has no directory"));
        }

        return launchTargetService.require(inner.name())
            .create(name, new ParsedTarget(inner, Optional.of(side)), innerOptions)
            .withTarget(handle(handle));
    }

    @Override
    public Profile resolve(Profile profile, TypedMap options) throws HeadlessMcException {
        Handle handle = handle(profile);
        Side side = getSide(profile);
        if (!Files.isRegularFile(handle.file())) {
            throw new LaunchException("The pack " + handle.file() + " of profile " + profile.name() + " does not exist.");
        }

        // the pack may have changed since the last launch
        VersionID id;
        try {
            id = getVersion(handle.file(), handle.platform());
        } catch (IllegalArgumentException e) {
            throw new LaunchException("Failed to read the pack of profile " + profile.name() + ": " + e.getMessage(), e);
        }

        LaunchTargetService.TargetMatch expected = parseInner(id, side);
        @Nullable LaunchTargetHandle inner = handle.resolved();
        if (inner == null
            || !inner.name().equalsIgnoreCase(expected.target().getName())
            || !expected.target().isSameTarget(profile.withTarget(inner), expected.parsed())) {
            if (inner != null) {
                log.info("The pack of profile {} now asks for {}", profile.name(), id);
            }

            inner = expected.parsed().handle();
        }

        Profile innerProfile = profile.withTarget(inner);
        Profile resolved = launchTargetService.resolve(innerProfile, options);
        //noinspection ObjectEquality
        if (resolved == innerProfile && inner == handle.resolved()) {
            return profile;
        }

        // the inner target may also have changed the profile, e.g. set a java version
        return resolved.withTarget(handle(handle.withResolved(resolved.target())));
    }

    @Override
    public ProcessLauncher launcher(Profile profile, TypedMap options) throws HeadlessMcException {
        Handle handle = handle(profile);
        getSide(profile);
        if (handle.resolved() == null) {
            throw new LaunchException("Profile " + profile.name() + " has not been resolved.");
        }

        return launchTargetService.launcher(profile.withTarget(handle.resolved()), options);
    }

    @Override
    public LaunchTargetHandle relocate(Profile profile, Path newPath) {
        Handle handle = handle(profile);
        Path file = handle.file().toAbsolutePath().normalize();
        Path oldPath = profile.path().toAbsolutePath().normalize();
        if (file.startsWith(oldPath)) {
            handle = handle.withFile(newPath.toAbsolutePath().normalize().resolve(oldPath.relativize(file).toString()));
        }

        @Nullable LaunchTargetHandle inner = handle.resolved();
        if (inner != null) {
            LaunchTarget innerTarget = launchTargetService.require(inner.name());
            handle = handle.withResolved(innerTarget.relocate(profile.withTarget(inner), newPath));
        }

        return handle(handle);
    }

    private Path getPackFile(String arg) throws IllegalArgumentException {
        Path file;
        try {
            file = fileService.getUserPath(arg).toAbsolutePath().normalize();
        } catch (InvalidPathException e) {
            throw new IllegalArgumentException("'" + arg + "' is not a path", e);
        }

        if (Files.isDirectory(file)) {
            file = file.resolve(PACK_TOML);
            if (!Files.isRegularFile(file)) {
                throw new IllegalArgumentException("directory " + file.getParent() + " contains no " + PACK_TOML);
            }
        } else if (!Files.isRegularFile(file)) {
            throw new IllegalArgumentException("file " + file + " does not exist");
        } else if (!PathCandidates.hasExtension(file, ".toml")) {
            throw new IllegalArgumentException(file + " is not a " + PACK_TOML);
        }

        return file;
    }

    /**
     * @return the one version of the pack to launch, for the given loader if the pack supports multiple.
     */
    private VersionID getVersion(Path file, @Nullable String platform) throws IllegalArgumentException {
        List<VersionID> ids;
        try {
            ids = packwizService.parsePackwizTomlFile(file);
        } catch (HeadlessMcException e) {
            throw new IllegalArgumentException(file + " is not a packwiz pack: " + e.getMessage(), e);
        }

        String available = ids.stream().map(VersionID::getPlatform).map(Platform::getName).collect(Collectors.joining(", "));
        if (platform != null) {
            List<VersionID> filtered = ids.stream()
                .filter(id -> id.getPlatform().getName().equalsIgnoreCase(platform))
                .toList();
            if (filtered.size() != 1) {
                throw new IllegalArgumentException(
                    "the pack has " + (filtered.isEmpty() ? "no" : "multiple versions for") + " loader " + platform
                        + ", available: " + available
                );
            }

            return filtered.getFirst();
        }

        if (ids.size() != 1) {
            throw new IllegalArgumentException(
                "the pack supports multiple loaders, please specify one of " + available
            );
        }

        return ids.getFirst();
    }

    /**
     * Lets the launch target for versions of the given side parse the version of the pack.
     */
    private LaunchTargetService.TargetMatch parseInner(VersionID id, Side side) throws IllegalArgumentException {
        List<String> args = new ArrayList<>();
        args.add(side.name().toLowerCase(Locale.ENGLISH));
        args.add(id.getPlatform().getName());
        args.add(id.getVersion().getName());
        id.getBuild().ifPresent(build -> args.add(build.getName()));
        String target = side.isServer() ? VersionHandle.SERVER : VersionHandle.VERSION;
        return launchTargetService.parse(target, new TargetArgs(args, Set.of(side)));
    }

    private Side getSide(Profile profile) throws LaunchException {
        return profile.side().orElseThrow(() -> new LaunchException(
            "Profile " + profile.name() + " is invalid: it launches a pack, but is neither a client nor a server"
        ));
    }

    private static TypedMap copy(TypedMap options) {
        TypedMap result = new TypedMapImpl();
        for (TypedMap.Entry<?> entry : options) {
            put(entry, result);
        }

        return result;
    }

    private static <V> void put(TypedMap.Entry<V> entry, TypedMap destination) {
        destination.put(entry.key(), entry.value());
    }

    /**
     * The data of the {@value #NAME} launch target.
     *
     * @param file     the {@value #PACK_TOML} of the pack.
     * @param platform the name of the loader to launch the pack with.
     *                 {@code null} if the pack has a single loader and none has been chosen.
     * @param resolved the handle of the launch target the version of the pack is launched with.
     *                 {@code null} until the pack has been read for the first time.
     */
    @With
    @RegisterForReflection
    public record Handle(
        Path file,
        @Nullable String platform,
        @Nullable LaunchTargetHandle resolved
    ) implements ReflectionRegistered {
        @SuppressWarnings("ConstantValue")
        public Handle {
            Objects.requireNonNull(file, "Launch target data is missing 'file'");
        }
    }

}
