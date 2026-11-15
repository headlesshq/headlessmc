package io.github.headlesshq.headlessmc.launcher.profile;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.launcher.process.LaunchException;
import io.github.headlesshq.headlessmc.launcher.process.ProcessLauncher;
import io.github.headlesshq.headlessmc.reflection.ReflectionRegistered;
import io.github.headlesshq.headlessmc.util.typemap.TypedMap;
import io.github.headlesshq.headlessmc.version.arg.Side;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.UnaryOperator;

/**
 * A configurable {@link LaunchTarget} for tests. Its handle is a single string, the first argument it parsed.
 * By default it parses every non-empty argument list, resolves nothing and cannot launch.
 */
public class FakeLaunchTarget implements LaunchTarget {
    private final String name;
    private final List<TargetArgs> parsed = new ArrayList<>();
    private final List<Profile> resolved = new ArrayList<>();
    private final List<Profile> launched = new ArrayList<>();
    private Function<TargetArgs, Optional<Side>> side = args -> Optional.empty();
    private @Nullable Function<TargetArgs, String> failure;
    private UnaryOperator<Profile> resolver = UnaryOperator.identity();
    private @Nullable ProcessLauncher launcher;
    private @Nullable String defaultName;
    private boolean detectable = true;
    private boolean fallback;
    private List<ArgCandidate> candidates = List.of();

    public FakeLaunchTarget(String name) {
        this.name = name;
    }

    /**
     * @param side the side of everything this target parses.
     * @return this target.
     */
    public FakeLaunchTarget withSide(@Nullable Side side) {
        this.side = args -> Optional.ofNullable(side);
        return this;
    }

    /**
     * @param failure returns the reason this target cannot parse arguments, or {@code null} if it can.
     * @return this target.
     */
    public FakeLaunchTarget failingIf(Function<TargetArgs, @Nullable String> failure) {
        this.failure = failure;
        return this;
    }

    /**
     * @param prefix this target only parses arguments whose first argument starts with this prefix.
     * @return this target.
     */
    public FakeLaunchTarget parsingOnly(String prefix) {
        return failingIf(args -> args.args().getFirst().startsWith(prefix) ? null : "does not start with " + prefix);
    }

    /**
     * @param resolver what {@link #resolve(Profile, TypedMap)} returns for a profile.
     * @return this target.
     */
    public FakeLaunchTarget resolvingWith(UnaryOperator<Profile> resolver) {
        this.resolver = resolver;
        return this;
    }

    public FakeLaunchTarget withLauncher(@Nullable ProcessLauncher launcher) {
        this.launcher = launcher;
        return this;
    }

    public FakeLaunchTarget withDefaultName(@Nullable String defaultName) {
        this.defaultName = defaultName;
        return this;
    }

    public FakeLaunchTarget detectable(boolean detectable) {
        this.detectable = detectable;
        return this;
    }

    /**
     * @param fallback whether this target parses its arguments as a {@link ParsedTarget#fallback() fallback}.
     * @return this target.
     */
    public FakeLaunchTarget fallback(boolean fallback) {
        this.fallback = fallback;
        return this;
    }

    public FakeLaunchTarget withCandidates(String... candidates) {
        this.candidates = List.of(candidates).stream().map(ArgCandidate::new).toList();
        return this;
    }

    /**
     * @param value the data of the handle.
     * @return a handle of this target.
     */
    public LaunchTargetHandle handle(String value) {
        return new LaunchTargetHandle(name, new Handle(value));
    }

    /**
     * @return the arguments {@link #parse(TargetArgs)} has been called with.
     */
    public List<TargetArgs> parsed() {
        return parsed;
    }

    /**
     * @return the profiles {@link #resolve(Profile, TypedMap)} has been called with.
     */
    public List<Profile> resolved() {
        return resolved;
    }

    /**
     * @return the profiles {@link #launcher(Profile, TypedMap)} has been called with.
     */
    public List<Profile> launched() {
        return launched;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getUsage() {
        return "<" + name + ">";
    }

    @Override
    public ParsedTarget parse(TargetArgs args) throws IllegalArgumentException {
        parsed.add(args);
        if (args.args().isEmpty()) {
            throw new IllegalArgumentException("no arguments");
        }

        @Nullable String reason = failure == null ? null : failure.apply(args);
        if (reason != null) {
            throw new IllegalArgumentException(reason);
        }

        return new ParsedTarget(handle(args.args().getFirst()), side.apply(args), fallback);
    }

    @Override
    public List<ArgCandidate> complete(TargetArgs args) {
        return candidates;
    }

    @Override
    public boolean isDetectable() {
        return detectable;
    }

    @Override
    public TargetInfo describe(Profile profile) {
        return TargetInfo.EMPTY;
    }

    @Override
    public String getLabel(Profile profile) {
        return name + ":" + value(profile.target());
    }

    @Override
    public boolean isSameTarget(Profile stored, ParsedTarget parsed) {
        return value(stored.target()).equals(value(parsed.handle()));
    }

    @Override
    public Optional<String> defaultName(LaunchTargetHandle handle) {
        return Optional.ofNullable(defaultName);
    }

    @Override
    public Profile create(String name, ParsedTarget target, TypedMap options) {
        Path path = options.get(GAME_DIR, Path.of(name));
        return new Profile(name, target.side(), target.handle(), path);
    }

    @Override
    public Profile resolve(Profile profile, TypedMap options) throws HeadlessMcException {
        resolved.add(profile);
        return resolver.apply(profile);
    }

    @Override
    public ProcessLauncher launcher(Profile profile, TypedMap options) throws HeadlessMcException {
        launched.add(profile);
        if (launcher == null) {
            throw new LaunchException("FakeLaunchTarget " + name + " has no launcher");
        }

        return launcher;
    }

    /**
     * @param handle a handle of this target, typed or as it has been read from json.
     * @return the value of the handle.
     */
    public static String value(LaunchTargetHandle handle) {
        Object data = handle.data();
        if (data instanceof Handle typed) {
            return typed.value();
        }

        if (data instanceof java.util.Map<?, ?> map) {
            return String.valueOf(map.get("value"));
        }

        return String.valueOf(data);
    }

    /**
     * The data of a {@link FakeLaunchTarget}.
     *
     * @param value the first argument that has been parsed.
     */
    public record Handle(String value) implements ReflectionRegistered {

    }

}
