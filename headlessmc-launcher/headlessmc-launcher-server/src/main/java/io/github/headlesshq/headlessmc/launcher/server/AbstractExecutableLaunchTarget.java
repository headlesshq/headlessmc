package io.github.headlesshq.headlessmc.launcher.server;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.files.FileService;
import io.github.headlesshq.headlessmc.launcher.process.LaunchException;
import io.github.headlesshq.headlessmc.launcher.process.ProcessLauncher;
import io.github.headlesshq.headlessmc.launcher.profile.AbstractLaunchTarget;
import io.github.headlesshq.headlessmc.launcher.profile.ArgCandidate;
import io.github.headlesshq.headlessmc.launcher.profile.LaunchTargetHandle;
import io.github.headlesshq.headlessmc.launcher.profile.ParsedTarget;
import io.github.headlesshq.headlessmc.launcher.profile.PathCandidates;
import io.github.headlesshq.headlessmc.launcher.profile.Profile;
import io.github.headlesshq.headlessmc.launcher.profile.TargetArgs;
import io.github.headlesshq.headlessmc.launcher.profile.TargetInfo;
import io.github.headlesshq.headlessmc.launcher.profile.VersionArgCandidates;
import io.github.headlesshq.headlessmc.reflection.ReflectionRegistered;
import io.github.headlesshq.headlessmc.util.typemap.TypedMap;
import io.github.headlesshq.headlessmc.version.arg.Side;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Base class of the launch targets that launch a single executable file, {@link JarLaunchTarget} and
 * {@link ScriptLaunchTarget}.
 *
 * <p>Arguments: {@code [client|server] <file>}. What such a file is cannot be told from the file,
 * so the {@link Profile#side() side} of a profile is what the user says it is:
 * the side given in the arguments, a server for commands that only launch servers,
 * and otherwise no side at all. A profile without a side is launched plainly, without anything
 * that is specific to Minecraft clients or servers.</p>
 *
 * <p>The game directory of a new profile is the directory of the file.
 * The file is launched in the game directory with the Java version of the profile,
 * if the profile has none it gets one when it is resolved, see {@link #defaultJavaVersion(Path)}.</p>
 *
 * @param <H> the type of the data this target stores in its handles.
 */
abstract class AbstractExecutableLaunchTarget<H extends ReflectionRegistered> extends AbstractLaunchTarget<H> {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractExecutableLaunchTarget.class);

    /**
     * @return the service to resolve the paths in arguments with.
     */
    protected abstract FileService fileService();

    /**
     * @return the launcher to build the process with.
     */
    protected abstract ServerLauncher serverLauncher();

    /**
     * @param handle a handle of this target.
     * @return the file the handle launches.
     */
    protected abstract Path getFile(H handle);

    /**
     * @param file the file to launch.
     * @return the data of a handle launching the file.
     */
    protected abstract H newHandle(Path file);

    /**
     * Verifies that an existing file is of the kind this target launches.
     *
     * @param file an existing file.
     * @throws IllegalArgumentException if this target cannot launch the file, with the reason.
     */
    protected abstract void verify(Path file) throws IllegalArgumentException;

    /**
     * @return a quick check for the files this target launches, to complete arguments with.
     */
    protected abstract Predicate<Path> filter();

    /**
     * @param file the file to launch.
     * @return the Java version a profile without a Java version launches the file with.
     */
    protected abstract int defaultJavaVersion(Path file);

    @Override
    public String getUsage() {
        return "[client|server] <path/to/" + getName() + ">";
    }

    @Override
    public ParsedTarget parse(TargetArgs args) throws IllegalArgumentException {
        List<String> values = args.args();
        if (values.isEmpty() || values.size() > 2) {
            throw new IllegalArgumentException("expected the path of a " + getName());
        }

        Optional<Side> side = Optional.empty();
        if (values.size() == 2) {
            @Nullable Side specified = VersionArgCandidates.getSide(values.getFirst());
            if (specified == null) {
                throw new IllegalArgumentException("'" + values.getFirst() + "' is neither 'client' nor 'server'");
            }

            side = Optional.of(specified);
        }

        String arg = values.getLast();
        Path file;
        try {
            file = fileService().getUserPath(arg).toAbsolutePath().normalize();
        } catch (InvalidPathException e) {
            throw new IllegalArgumentException("'" + arg + "' is not a path", e);
        }

        if (!Files.isRegularFile(file)) {
            throw new IllegalArgumentException("file " + file + " does not exist");
        }

        verify(file);
        if (side.isEmpty()) {
            // the commands for servers add servers, everything else has no side unless the user names one
            if (args.isServerOnly()) {
                side = Optional.of(Side.SERVER);
            } else if (args.isClientOnly()) {
                side = Optional.of(Side.CLIENT);
            }
        }

        return new ParsedTarget(handle(newHandle(file)), side);
    }

    @Override
    public List<ArgCandidate> complete(TargetArgs args) {
        List<String> values = args.args();
        if (values.size() > 2) {
            return List.of();
        }

        List<ArgCandidate> result = new ArrayList<>();
        if (values.size() <= 1) {
            for (Side side : Side.values()) {
                if (args.sides().contains(side)) {
                    result.add(new ArgCandidate(side.name().toLowerCase(Locale.ENGLISH)));
                }
            }
        } else if (VersionArgCandidates.getSide(values.getFirst()) == null) {
            return List.of();
        }

        result.addAll(PathCandidates.complete(values.isEmpty() ? "" : values.getLast(), filter()));
        return result;
    }

    @Override
    public TargetInfo describe(Profile profile) {
        return new TargetInfo(Optional.empty(), Optional.empty(), Optional.of(getFile(handle(profile))));
    }

    @Override
    public String getLabel(Profile profile) {
        return getName() + ":" + Objects.requireNonNullElse(getFile(handle(profile)).getFileName(), "");
    }

    @Override
    public Profile create(String name, ParsedTarget target, TypedMap options) {
        H handle = handle(target.handle());
        Path path = options.get(GAME_DIR);
        if (path == null) {
            path = Objects.requireNonNull(getFile(handle).toAbsolutePath().getParent(), "File has no directory");
        }

        return new Profile(name, target.side(), handle(handle), path);
    }

    @Override
    public Profile resolve(Profile profile, TypedMap options) throws HeadlessMcException {
        Path file = getExistingFile(profile);
        if (profile.javaVersion() != null) {
            return profile;
        }

        int javaVersion = defaultJavaVersion(file);
        LOG.info("Profile {} has no Java version, using Java {} to launch {}", profile.name(), javaVersion, file);
        return profile.withJavaVersion(javaVersion);
    }

    @Override
    public ProcessLauncher launcher(Profile profile, TypedMap options) throws HeadlessMcException {
        return serverLauncher().launcher(profile, getExistingFile(profile), options);
    }

    @Override
    public LaunchTargetHandle relocate(Profile profile, Path newPath) {
        Path file = getFile(handle(profile));
        return handle(newHandle(Objects.requireNonNull(Relocation.relocate(file, profile.path(), newPath))));
    }

    private Path getExistingFile(Profile profile) throws LaunchException {
        Path file = getFile(handle(profile));
        if (!Files.isRegularFile(file)) {
            throw new LaunchException("The " + getName() + " " + file + " of profile " + profile.name() + " does not exist.");
        }

        return file;
    }

}
