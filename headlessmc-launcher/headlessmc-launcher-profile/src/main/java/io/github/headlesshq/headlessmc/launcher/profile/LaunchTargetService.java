package io.github.headlesshq.headlessmc.launcher.profile;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.launcher.process.LaunchException;
import io.github.headlesshq.headlessmc.launcher.process.ProcessLauncher;
import io.github.headlesshq.headlessmc.util.typemap.TypedMap;
import io.github.headlesshq.headlessmc.version.arg.VersionArg;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * Manages the {@link LaunchTarget}s: every CDI bean implementing {@link LaunchTarget} is available here
 * by its {@link LaunchTarget#getName() name}. This is the entry point for everything that needs to know
 * what a {@link Profile} launches, the methods taking a profile delegate to the target of that profile.
 *
 * <p>It also decides which target the arguments of a command are for, see {@link #parse(String, TargetArgs)}.</p>
 */
public interface LaunchTargetService {
    /**
     * @param name the name of a launch target, case is ignored.
     * @return the launch target with that name.
     */
    Optional<LaunchTarget> getTarget(String name);

    /**
     * @return all launch targets, sorted by their name.
     */
    List<LaunchTarget> getTargets();

    /**
     * @param name the name of a launch target, case is ignored.
     * @return the launch target with that name.
     * @throws IllegalArgumentException if there is no such launch target, listing the available ones.
     */
    LaunchTarget require(String name) throws IllegalArgumentException;

    /**
     * @param profile a profile.
     * @return the launch target of the profile.
     * @throws LaunchException if the launch target of the profile is not available,
     *                         e.g. because the plugin providing it has been removed.
     */
    LaunchTarget require(Profile profile) throws LaunchException;

    /**
     * Finds the launch target for the arguments of a command.
     * <ul>
     *     <li>If a target is named, it has to {@link LaunchTarget#parse(TargetArgs) parse} the arguments.</li>
     *     <li>Otherwise all {@link LaunchTarget#isDetectable() detectable} targets are asked to parse
     *     the arguments. Parsing verifies the arguments, so a target only succeeds for arguments
     *     it can actually launch. Exactly one target has to succeed. Targets that only succeed with a
     *     {@link ParsedTarget#fallback() fallback} are left out if another target succeeds.</li>
     * </ul>
     *
     * @param target the name of the launch target to use, {@code null} to detect it.
     * @param args   the arguments to parse.
     * @return the launch target and what it parsed.
     * @throws IllegalArgumentException if the named target does not exist or cannot parse the arguments,
     *                                  if no target can parse the arguments, with the reason of every target,
     *                                  or if more than one target can.
     */
    TargetMatch parse(@Nullable String target, TargetArgs args) throws IllegalArgumentException;

    /**
     * Completes the arguments of a command.
     *
     * @param target the name of the launch target to complete for, {@code null} to complete for all
     *               launch targets that allow one of the {@link TargetArgs#sides()}.
     * @param args   the arguments typed so far, the last one is being completed.
     * @return the candidates for the last argument, without duplicates.
     */
    List<ArgCandidate> complete(@Nullable String target, TargetArgs args);

    /**
     * @param profile a profile.
     * @return what the launch target knows about the profile,
     * {@link TargetInfo#EMPTY} if the target is not available or the profile is invalid.
     */
    TargetInfo describe(Profile profile);

    /**
     * The version a profile launches, with a concrete Minecraft version, for everything that depends on the
     * Minecraft version, like mods. A profile has two versions, the one that has been
     * {@link TargetInfo#specified() specified}, which can be vague, e.g. {@code fabric/latest}, and the one it
     * currently launches, {@link TargetInfo#current()}, e.g. {@code fabric/26.3}.
     * This returns the current version. For a profile that has not been resolved yet, e.g. a profile that has just
     * been created for the arguments of a command, it finds out what the specified version resolves to,
     * see {@link LaunchTarget#resolveVersion(Profile)}. Nothing is installed or saved.
     *
     * <p>Not every profile has a version: its launch target may not launch a Minecraft version at all,
     * or may not be available.</p>
     *
     * @param profile a profile.
     * @return the version of the profile, with a concrete Minecraft version, never {@code latest} or {@code stable}.
     * @throws IllegalArgumentException if the Minecraft version of the profile is not known, with the reason.
     */
    VersionArg requireVersion(Profile profile) throws IllegalArgumentException;

    /**
     * The platform of the version a profile launches, e.g. {@code fabric}. Unlike {@link #requireVersion(Profile)}
     * this does not need to know the Minecraft version, so nothing has to be resolved for it.
     *
     * @param profile a profile.
     * @return the name of the platform of the {@link TargetInfo#version() version} of the profile.
     * @throws IllegalArgumentException if the profile does not launch a version, with the reason.
     */
    String requirePlatform(Profile profile) throws IllegalArgumentException;

    /**
     * @param profile a profile.
     * @return a short text describing what the profile launches, the name of the target if it is not available.
     */
    String getLabel(Profile profile);

    /**
     * @param profile a profile.
     * @return the profile with everything it inherits, see {@link LaunchTarget#effective(Profile)}.
     * @throws HeadlessMcException if the launch target is not available or the profile is invalid.
     */
    Profile effective(Profile profile) throws HeadlessMcException;

    /**
     * Finds a stored profile that launches what has been parsed,
     * see {@link LaunchTarget#isSameTarget(Profile, ParsedTarget)}.
     *
     * @param match parsed arguments.
     * @return a stored profile with the same target and side for the arguments.
     */
    Optional<Profile> findProfile(TargetMatch match);

    /**
     * Creates a profile for parsed arguments, see {@link LaunchTarget#create(String, ParsedTarget, TypedMap)}.
     * The profile is not saved.
     *
     * @param name    the name of the profile, {@code null} to use the
     *                {@link LaunchTarget#defaultName(LaunchTargetHandle) default name} of the target.
     * @param match   the parsed arguments.
     * @param options the options for creating the profile.
     * @return a new profile.
     * @throws IllegalArgumentException if no name is given and the target has no default name.
     */
    Profile create(@Nullable String name, TargetMatch match, TypedMap options) throws IllegalArgumentException;

    /**
     * Creates a profile for launching parsed arguments once, without ever saving it.
     * Unlike {@link #create(String, TargetMatch, TypedMap)} this always finds a name.
     *
     * @param match   the parsed arguments.
     * @param options the options for creating the profile.
     * @return a new profile that is not meant to be saved.
     */
    Profile createTransient(TargetMatch match, TypedMap options);

    /**
     * Resolves a profile, see {@link LaunchTarget#resolve(Profile, TypedMap)}, without saving it.
     *
     * @param profile the profile to resolve.
     * @param options the options for resolving.
     * @return the resolved profile.
     * @throws HeadlessMcException if the launch target is not available or the profile cannot be resolved.
     */
    Profile resolve(Profile profile, TypedMap options) throws HeadlessMcException;

    /**
     * Resolves a profile and saves the result if it changed, see {@link #persist(Profile, Profile)}:
     * resolving a profile that has been created for a single launch does not create a file.
     *
     * <p>Call this with the profile as it is stored. Options of a single launch, like additional
     * jvm arguments, have to be applied to the returned profile, otherwise they would be saved.</p>
     *
     * @param profile the stored profile to resolve.
     * @param options the options for resolving.
     * @return the resolved profile.
     * @throws HeadlessMcException if the launch target is not available or the profile cannot be resolved.
     */
    Profile resolveAndPersist(Profile profile, TypedMap options) throws HeadlessMcException;

    /**
     * Saves a changed profile, if it is a stored profile.
     * A launch works with two profiles: the profile as it is stored, and the profile that is launched,
     * which additionally has the options of that launch. Everything that is found out during a launch and
     * should be remembered, like what the profile has been resolved to, is a change to the first one,
     * and is saved with this method. The second one is never saved.
     *
     * <p>Only a profile that is stored exactly as it is given is saved: a profile that has been created
     * for a single launch does not get a file, and a stored profile that has been changed for a single
     * launch is not written back.</p>
     *
     * @param profile the profile as it is stored.
     * @param changed the profile with the changes to remember.
     * @return the profile to continue with: the saved profile, as {@link ProfileService#save(Profile)} returns it,
     * or the changed profile if nothing has been saved.
     */
    Profile persist(Profile profile, Profile changed);

    /**
     * Builds the process for a resolved profile, see {@link LaunchTarget#launcher(Profile, TypedMap)}.
     *
     * @param profile the resolved profile.
     * @param options the options for launching.
     * @return a launcher for the process.
     * @throws HeadlessMcException if the launch target is not available or the process cannot be built.
     */
    ProcessLauncher launcher(Profile profile, TypedMap options) throws HeadlessMcException;

    /**
     * A launch target that parsed arguments.
     *
     * @param target the launch target.
     * @param parsed what the launch target parsed.
     */
    record TargetMatch(LaunchTarget target, ParsedTarget parsed) {

    }

}
