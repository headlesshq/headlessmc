package io.github.headlesshq.headlessmc.launcher.profile;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.launcher.process.LaunchException;
import io.github.headlesshq.headlessmc.platform.PlatformService;
import io.github.headlesshq.headlessmc.platform.VersionID;
import io.github.headlesshq.headlessmc.util.typemap.TypedMap;
import io.github.headlesshq.headlessmc.version.arg.Side;
import io.github.headlesshq.headlessmc.version.arg.VersionArg;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Base class of the two launch targets that launch a version described by a {@link VersionArg}:
 * {@value VersionHandle#VERSION} for clients and {@value VersionHandle#SERVER} for servers.
 * It implements everything that only depends on the {@link VersionArg}, parsing and describing it.
 * Resolving and launching is different for clients and servers and left to the implementations.
 *
 * <p>Arguments are only accepted if they resolve to a {@link VersionID}: the platform, the Minecraft version
 * and, if one is given, the build have to exist, and the platform has to support the {@link #getSide() side}.
 * The name of an installed version.json or a path does not resolve to a {@link VersionID},
 * so these are never claimed by this target.</p>
 */
public abstract class AbstractVersionLaunchTarget extends AbstractLaunchTarget<VersionHandle> {
    /**
     * @return the service to resolve versions with.
     */
    protected abstract PlatformService platformService();

    /**
     * @return the side this target launches, it is the side of all profiles of this target.
     */
    protected abstract Side getSide();

    /**
     * @param name    the name of the new profile.
     * @param handle  the handle of the new profile.
     * @param options the options of {@link #create(String, ParsedTarget, TypedMap)}, without a game directory.
     * @return the default game directory for a new profile.
     */
    protected abstract Path defaultPath(String name, VersionHandle handle, TypedMap options);

    /**
     * @return the game arguments a new profile starts with.
     */
    protected List<String> defaultGameArgs() {
        return List.of();
    }

    /**
     * @param id a resolved version.
     * @return {@code true} if the platform of the version supports the {@link #getSide() side} of this target,
     * arguments for other versions are not accepted.
     */
    protected boolean isSupported(VersionID id) {
        return id.getPlatform().hasSupportFor(getSide());
    }

    @Override
    protected Class<VersionHandle> handleType() {
        return VersionHandle.class;
    }

    @Override
    public String getName() {
        return getSide().isServer() ? VersionHandle.SERVER : VersionHandle.VERSION;
    }

    @Override
    public String getUsage() {
        return (getSide().isServer() ? "[server]" : "[client]") + " [platform] <mc-version> [build]";
    }

    @Override
    public ParsedTarget parse(TargetArgs args) throws IllegalArgumentException {
        if (args.args().isEmpty()) {
            throw new IllegalArgumentException("no version specified");
        }

        if (!args.sides().contains(getSide())) {
            throw new IllegalArgumentException("only launches " + name(getSide()) + "s");
        }

        VersionArg arg;
        try {
            arg = VersionArg.parse(args.args());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("'" + args + "' is not a version", e);
        }

        Optional<Side> side = arg.side();
        if (side.isPresent() && side.get() != getSide()) {
            throw new IllegalArgumentException("'" + args + "' is a " + name(side.get()) + " version");
        }

        // a version without a side is a client, unless the command only launches servers
        if (side.isEmpty() && getSide().isServer() && !args.isServerOnly()) {
            throw new IllegalArgumentException("'" + args + "' is a client version, use 'server " + args + "'");
        }

        VersionID id;
        try {
            id = VersionID.resolve(platformService(), arg.withSide(getSide()));
        } catch (HeadlessMcException | IllegalArgumentException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }

        if (!isSupported(id)) {
            throw new IllegalArgumentException(
                "platform " + id.getPlatform().getName() + " does not support " + name(getSide()) + "s"
            );
        }

        // keep what has been specified, e.g. the build "latest", but with the proper names of platform and version
        boolean vagueVersion = VersionArg.LATEST.equalsIgnoreCase(arg.version())
            || VersionArg.STABLE.equalsIgnoreCase(arg.version());
        VersionArg specified = new VersionArg(
            Optional.of(getSide()),
            id.getPlatform().getName(),
            vagueVersion ? arg.version() : id.getVersion().getName(),
            arg.build()
        );

        return new ParsedTarget(handle(VersionHandle.of(specified)), Optional.of(getSide()));
    }

    @Override
    public List<ArgCandidate> complete(TargetArgs args) {
        if (!args.sides().contains(getSide())) {
            return List.of();
        }

        List<String> values = new ArrayList<>(args.args());
        VersionArgCandidates candidates = new VersionArgCandidates(platformService());
        Side first = values.isEmpty() ? null : VersionArgCandidates.getSide(values.getFirst());
        if (first != null && first != getSide()) {
            return List.of();
        }

        if (first == null) {
            if (getSide().isServer() && !args.isServerOnly()) {
                // for commands launching both sides a server has to start with "server"
                return values.size() <= 1 ? List.of(new ArgCandidate(name(Side.SERVER))) : List.of();
            }

            // complete as if the side had been typed, to only get platforms supporting this side
            values.addFirst(name(getSide()));
        }

        boolean sideTyped = first != null;
        String other = name(getSide().isServer() ? Side.CLIENT : Side.SERVER);
        return candidates.complete(values).stream()
            .filter(candidate -> !other.equals(candidate.value()))
            .filter(candidate -> sideTyped || !name(getSide()).equals(candidate.value()))
            .toList();
    }

    @Override
    public TargetInfo describe(Profile profile) {
        VersionHandle handle = handle(profile);
        return new TargetInfo(
            Optional.of(handle.version()),
            handle.currentVersion(),
            Optional.ofNullable(handle.currentTarget())
        );
    }

    @Override
    public boolean isSameTarget(Profile stored, ParsedTarget parsed) {
        return handle(stored).version().equals(handle(parsed.handle()).version());
    }

    @Override
    public Optional<String> defaultName(LaunchTargetHandle handle) {
        return Optional.of(handle(handle).version().toString("-"));
    }

    @Override
    public Profile create(String name, ParsedTarget target, TypedMap options) {
        VersionHandle handle = handle(target.handle());
        Path path = options.get(GAME_DIR);
        if (path == null) {
            path = defaultPath(name, handle, options);
        }

        return new Profile(name, Optional.of(getSide()), handle(handle), path).withGameArgs(defaultGameArgs());
    }

    /**
     * Checks the things that cannot be wrong in a profile created by this target,
     * but can be in a file that has been edited by hand.
     *
     * @param profile a profile with this target.
     * @return the handle of the profile.
     * @throws LaunchException if the side of the profile or its version is not the side of this target.
     */
    protected VersionHandle validate(Profile profile) throws LaunchException {
        VersionHandle handle = handle(profile);
        if (!Optional.of(getSide()).equals(profile.side())) {
            throw new LaunchException(
                "Profile " + profile.name() + " is invalid: it has launch target " + getName() + ", but is "
                    + profile.side().map(side -> "a " + name(side)).orElse("neither a client nor a server")
            );
        }

        if (handle.isServer() != getSide().isServer()) {
            throw new LaunchException(
                "Profile " + profile.name() + " is invalid: version " + handle.version()
                    + " does not belong to launch target " + getName()
            );
        }

        return handle;
    }

    private static String name(Side side) {
        return side.isServer() ? "server" : "client";
    }

}
