package io.github.headlesshq.headlessmc.launcher.profile;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.files.McFiles;
import io.github.headlesshq.headlessmc.launcher.process.ProcessLauncher;
import io.github.headlesshq.headlessmc.util.typemap.Key;
import io.github.headlesshq.headlessmc.util.typemap.TypedMap;
import io.github.headlesshq.headlessmc.version.arg.VersionArg;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * A launch target is a kind of thing a {@link Profile} can launch: a Minecraft version, a server,
 * a version.json file, a jar, a script, a modpack, another profile, ...
 * A profile only stores a {@link LaunchTargetHandle}, the name of its target and some data,
 * the target with that name gives the data its meaning.
 *
 * <p>This is an extension point. Every CDI bean implementing this interface is a launch target,
 * so plugins can add their own, see {@link LaunchTargetService}.
 * {@link AbstractLaunchTarget} takes care of converting the data of the handle.</p>
 *
 * <p>The life of a target, in the order its methods are called:</p>
 * <ol>
 *     <li>{@link #parse(TargetArgs)} turns the arguments of a command into a handle.
 *     It has to <em>verify</em> the arguments, as this is also how the target for arguments is detected:
 *     if no target has been specified, the one target that can parse the arguments is used.</li>
 *     <li>{@link #create(String, ParsedTarget, TypedMap)} creates a profile for the handle.</li>
 *     <li>{@link #resolve(Profile, TypedMap)} finds out, and remembers in the profile, what exactly is launched,
 *     installing it if needed.</li>
 *     <li>{@link #launcher(Profile, TypedMap)} builds the process.</li>
 * </ol>
 *
 * <p>Only {@link #resolve(Profile, TypedMap)} and {@link #launcher(Profile, TypedMap)} may install or download
 * something. {@link #parse(TargetArgs)} may read files and cached metadata.
 * Everything else has to be cheap, it is called for listings and completions.</p>
 *
 * <p>Data that is only known after {@link #resolve(Profile, TypedMap)} has to be nullable in the handle,
 * a profile is saved before it is resolved for the first time, and profiles converted from older formats
 * are not resolved at all. The {@link Profile#side() side} of a profile is never such data:
 * it is decided by {@link #parse(TargetArgs)} and does not change.</p>
 */
public interface LaunchTarget {
    /**
     * Set for {@link #resolve(Profile, TypedMap)} when a profile is being added.
     * A target can use this to fill the new profile with defaults that are only known once it has been installed.
     */
    Key<Boolean> NEW_PROFILE = new Key<>(Boolean.class, "hmc.launch.target.new.profile");
    /**
     * Set for {@link #resolve(Profile, TypedMap)} to forget what the profile has been resolved to
     * and to resolve it again, e.g. to move a profile for {@code latest} to a newer build.
     */
    Key<Boolean> UPDATE = new Key<>(Boolean.class, "hmc.launch.target.update");
    /**
     * The {@code .minecraft} directory to install into and to place default game directories in,
     * if the user has selected one. Without it targets use the directory a profile already uses,
     * or the primary directory.
     */
    Key<McFiles> MC_FILES = new Key<>(McFiles.class, "hmc.launch.target.mc.files");
    /**
     * The game directory for {@link #create(String, ParsedTarget, TypedMap)}, if the user has specified one.
     */
    Key<Path> GAME_DIR = new Key<>(Path.class, "hmc.launch.target.game.dir");

    /**
     * @return the name of this target, e.g. {@code version}. It is stored in profiles
     * ({@link LaunchTargetHandle#name()}) and used to select this target on the command line.
     */
    String getName();

    /**
     * @return a single line describing the arguments {@link #parse(TargetArgs)} accepts,
     * e.g. {@code [platform] <mc-version> [build]}, for help and error messages.
     */
    String getUsage();

    /**
     * Makes sense of the arguments of a command.
     * This does not only parse the arguments, it also has to verify that they can be resolved by this target:
     * a version has to exist, a file has to exist and be of the right kind, and so on.
     * A target must not accept arguments just because they have the right shape, otherwise it
     * would claim arguments that are meant for another target.
     * Arguments this target can launch, but that are more likely meant for another target if that one can
     * launch them too, are returned as a {@link ParsedTarget#fallback() fallback}.
     *
     * @param args the arguments to parse.
     * @return an unresolved handle for the arguments, and the side of a profile for them.
     * @throws IllegalArgumentException if this target cannot launch the arguments, with the reason as message.
     */
    ParsedTarget parse(TargetArgs args) throws IllegalArgumentException;

    /**
     * Completes the arguments of a command. This must not be slow, do not access the network.
     *
     * @param args the arguments typed so far, the last one is being completed and may be empty.
     * @return candidates for the last argument.
     */
    List<ArgCandidate> complete(TargetArgs args);

    /**
     * Whether this target takes part in the detection of the target for arguments.
     * A target that is not detectable is only used if it has been named explicitly.
     *
     * @return {@code true} if this target may be detected by {@link #parse(TargetArgs)}.
     */
    default boolean isDetectable() {
        return true;
    }

    /**
     * Describes what the profile launches without resolving anything.
     *
     * @param profile a profile with this target.
     * @return what is known about what the profile launches.
     */
    TargetInfo describe(Profile profile);

    /**
     * Finds the version the profile launches, with a concrete Minecraft version, for everything that depends on
     * the Minecraft version, like mods.
     * {@link #describe(Profile)} cannot always tell: a profile knows the version that has been
     * {@link TargetInfo#specified() specified}, which can be vague, e.g. {@code fabric/latest}, and the version
     * it has been resolved to, {@link TargetInfo#current()}, e.g. {@code fabric/26.3}. A profile that has not been
     * resolved yet, like one that has just been created for the arguments of a command, only has the first.
     * This finds out what the specified version would be resolved to now.
     *
     * <p>Unlike {@link #resolve(Profile, TypedMap)} this installs nothing and does not change the profile.
     * Like {@link #parse(TargetArgs)} it may read files and cached metadata.</p>
     *
     * @param profile a profile with this target.
     * @return the current version of the profile, or else what its specified version resolves to.
     * Empty if the profile does not launch a Minecraft version.
     * @throws HeadlessMcException if the specified version cannot be resolved.
     */
    default Optional<VersionArg> resolveVersion(Profile profile) throws HeadlessMcException {
        return describe(profile).version();
    }

    /**
     * @param profile a profile with this target.
     * @return a short text describing what the profile launches for listings, e.g. {@code version} or {@code profile:main}.
     */
    default String getLabel(Profile profile) {
        return getName();
    }

    /**
     * Checks if a stored profile launches what has been parsed from arguments.
     * Launching by arguments, e.g. {@code launch fabric 1.21.1}, uses a stored profile for them if there is one.
     *
     * @param stored a stored profile with this target.
     * @param parsed the parsed arguments.
     * @return {@code true} if the stored profile is a profile for the parsed arguments.
     */
    boolean isSameTarget(Profile stored, ParsedTarget parsed);

    /**
     * A name for a profile that is created for the given handle without the user naming it.
     * Only targets whose handle makes a good name, like a version, have one.
     * For all other targets the user has to name a profile that is added.
     *
     * @param handle a handle of this target.
     * @return a default name for a profile with the handle.
     */
    default Optional<String> defaultName(LaunchTargetHandle handle) {
        return Optional.empty();
    }

    /**
     * Creates a new profile for parsed arguments, with the defaults of this target:
     * its {@link Profile#side() side}, game directory and arguments.
     * The profile is neither saved nor resolved, and nothing is installed.
     *
     * @param name    the name of the profile.
     * @param target  the parsed arguments.
     * @param options {@link #GAME_DIR} and {@link #MC_FILES}, if the user specified them.
     * @return a new profile.
     */
    Profile create(String name, ParsedTarget target, TypedMap options);

    /**
     * The profile that is actually launched for a profile.
     * This is the profile itself, unless a target combines the profile with something else,
     * like a profile that launches another profile.
     *
     * @param profile a profile with this target.
     * @return the profile with everything it inherits.
     * @throws HeadlessMcException if the profile cannot be combined, e.g. because what it inherits from is missing.
     */
    default Profile effective(Profile profile) throws HeadlessMcException {
        return profile;
    }

    /**
     * Called when the game directory of a profile is moved.
     * A target that remembers paths inside the game directory has to return a handle with the moved paths.
     *
     * @param profile a profile with this target and its old {@link Profile#path() game directory}.
     * @param newPath the new game directory.
     * @return the handle for the new game directory.
     */
    default LaunchTargetHandle relocate(Profile profile, Path newPath) {
        return profile.target();
    }

    /**
     * Finds out what exactly the profile launches and brings the profile up to date with it:
     * installs what is missing, pins what has been found in the handle, and fills fields of the profile
     * that can only be derived now, e.g. the Java version of a server.
     *
     * <p>This is called with the profile as it is stored, before options of a single launch are applied to it.
     * The returned profile is saved if it has changed, so that the next launch resolves to the same thing.
     * Return the given profile itself if nothing has changed.
     * A resolve must never change the {@link Profile#side() side} or {@link Profile#path() game directory}.</p>
     *
     * @param profile a profile with this target.
     * @param options {@link #NEW_PROFILE}, {@link #UPDATE}, {@link #MC_FILES} and options of specific targets.
     * @return the resolved profile, the given profile if nothing changed.
     * @throws HeadlessMcException if the profile cannot be resolved.
     */
    Profile resolve(Profile profile, TypedMap options) throws HeadlessMcException;

    /**
     * Builds the process for a profile that has been {@link #resolve(Profile, TypedMap) resolved}.
     *
     * @param profile the resolved profile, with the options of this launch applied.
     * @param options options of specific targets, e.g. the account to launch a client with.
     * @return a launcher for the process.
     * @throws HeadlessMcException if the process cannot be built.
     */
    ProcessLauncher launcher(Profile profile, TypedMap options) throws HeadlessMcException;

}
