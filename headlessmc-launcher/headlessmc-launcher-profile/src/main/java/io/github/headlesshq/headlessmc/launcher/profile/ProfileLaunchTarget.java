package io.github.headlesshq.headlessmc.launcher.profile;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.launcher.process.LaunchException;
import io.github.headlesshq.headlessmc.launcher.process.ProcessLauncher;
import io.github.headlesshq.headlessmc.reflection.ReflectionRegistered;
import io.github.headlesshq.headlessmc.util.json.JsonService;
import io.github.headlesshq.headlessmc.util.typemap.TypedMap;
import io.github.headlesshq.headlessmc.util.typemap.TypedMapImpl;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.SequencedMap;
import java.util.Set;

/**
 * The {@value #NAME} launch target: a profile that launches another profile.
 * This is profile inheritance. The profile with this target, the child, launches whatever its parent launches,
 * with its own settings added on top of the settings of the parent. It is the same idea as launching a profile
 * with additional options on the command line, only that the options are saved under a name.
 *
 * <p>Arguments: {@code <profile-name>}, e.g. {@code profile add --target profile main --name main-debug}.
 * This target is never detected, a profile is only inherited from if this target has been named.</p>
 *
 * <p>The profile that is launched is built by {@link #effective(Profile)}:</p>
 * <table>
 *     <caption>How a child is combined with its parent</caption>
 *     <tr><th>Field</th><th>Value</th></tr>
 *     <tr><td>name, game directory, eula status</td><td>of the child</td></tr>
 *     <tr><td>launch target</td><td>of the parent</td></tr>
 *     <tr><td>jvm arguments, game arguments</td><td>of the parent, followed by those of the child</td></tr>
 *     <tr><td>system properties</td><td>of the parent, properties of the child replace them</td></tr>
 *     <tr><td>patchers</td><td>of the parent and of the child</td></tr>
 *     <tr><td>launch options</td><td>of the child, options it does not set are taken from the parent</td></tr>
 *     <tr><td>java version</td><td>of the child, if it has none, of the parent</td></tr>
 * </table>
 *
 * <p>A parent can be a child itself. The {@link Profile#side() side} of a child is the side of its parent
 * at the time the child is created. Sides never change, so it stays correct.
 * Resolving a child resolves its parent, what is pinned is saved in the profile at the root, never in the child.</p>
 */
@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class ProfileLaunchTarget extends AbstractLaunchTarget<ProfileLaunchTarget.Handle> {
    /**
     * The name of this launch target.
     */
    public static final String NAME = "profile";

    private final LaunchTargetService launchTargetService;
    private final ProfileService profileService;
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
        return "<profile-name>";
    }

    @Override
    public ParsedTarget parse(TargetArgs args) throws IllegalArgumentException {
        if (args.args().size() != 1) {
            throw new IllegalArgumentException("expected the name of one profile");
        }

        String name = args.args().getFirst();
        Profile parent = profileService.getProfile(name)
            .orElseThrow(() -> new IllegalArgumentException("there is no profile named '" + name + "'"));

        return new ParsedTarget(handle(new Handle(parent.name())), parent.side());
    }

    @Override
    public List<ArgCandidate> complete(TargetArgs args) {
        if (args.args().size() > 1) {
            return List.of();
        }

        return profileService.getProfiles().stream()
            .filter(profile -> args.allows(profile.side()))
            .map(profile -> new ArgCandidate(profile.name()))
            .toList();
    }

    /**
     * @return {@code false}, a profile only inherits from another profile if that has been asked for.
     */
    @Override
    public boolean isDetectable() {
        return false;
    }

    @Override
    public TargetInfo describe(Profile profile) {
        try {
            return launchTargetService.describe(root(profile));
        } catch (HeadlessMcException e) {
            return TargetInfo.EMPTY;
        }
    }

    @Override
    public String getLabel(Profile profile) {
        return NAME + ":" + handle(profile).profile();
    }

    @Override
    public Profile create(String name, ParsedTarget target, TypedMap options) {
        Handle handle = handle(target.handle());
        Profile parent = profileService.getProfile(handle.profile())
            .orElseThrow(() -> new IllegalArgumentException("there is no profile named '" + handle.profile() + "'"));

        // by default a child plays in the game directory of its parent, it is the same game with other settings
        Path path = options.get(GAME_DIR);
        return new Profile(name, target.side(), handle(handle), path == null ? parent.path() : path);
    }

    @Override
    public Profile effective(Profile profile) throws HeadlessMcException {
        return effective(profile, new LinkedHashSet<>(), profile);
    }

    @Override
    public Profile resolve(Profile profile, TypedMap options) throws HeadlessMcException {
        Profile parent = parent(profile);
        checkSide(profile, parent);
        // detects cycles before anything is resolved
        effective(profile);

        // defaults for a new profile are for the child, the parent already exists
        TypedMap parentOptions = new TypedMapImpl();
        for (TypedMap.Entry<?> entry : options) {
            copy(entry, parentOptions);
        }

        parentOptions.remove(NEW_PROFILE);
        // what has been resolved is saved in the parent, a child has nothing to remember
        launchTargetService.resolveAndPersist(parent, parentOptions);
        return profile;
    }

    @Override
    public ProcessLauncher launcher(Profile profile, TypedMap options) throws HeadlessMcException {
        return launchTargetService.launcher(effective(profile), options);
    }

    /**
     * Makes a child inherit from a parent with another name, used when the parent is renamed.
     *
     * @param child  a profile with this target.
     * @param parent the new name of the parent.
     * @return the child with a handle for the new name.
     */
    public Profile withParent(Profile child, String parent) {
        return child.withTarget(handle(new Handle(parent)));
    }

    /**
     * @param profile any profile.
     * @return the name of the profile the given profile inherits from, if it has this target.
     */
    public Optional<String> getParentName(Profile profile) {
        if (!NAME.equalsIgnoreCase(profile.target().name())) {
            return Optional.empty();
        }

        try {
            return Optional.of(handle(profile).profile());
        } catch (HeadlessMcException e) {
            return Optional.empty();
        }
    }

    /**
     * @param parent the name of a profile.
     * @return the stored profiles that inherit directly from the profile with that name.
     */
    public List<Profile> getChildren(String parent) {
        return profileService.getProfiles().stream()
            .filter(profile -> getParentName(profile).filter(parent::equalsIgnoreCase).isPresent())
            .toList();
    }

    /**
     * @param origin the profile this started with. It is used instead of the stored profile of its name:
     *               it may not be stored yet, or be stored with another target, e.g. while it is edited.
     *               Without this a profile could be made to inherit from itself through another profile.
     */
    private Profile effective(Profile child, Set<String> visited, Profile origin) throws HeadlessMcException {
        if (!visited.add(child.name().toLowerCase(Locale.ENGLISH))) {
            throw new LaunchException(
                "Profile " + child.name() + " inherits from itself: " + String.join(" -> ", visited)
                    + " -> " + child.name().toLowerCase(Locale.ENGLISH)
            );
        }

        Profile parent = parent(child, origin);
        checkSide(child, parent);
        Profile base = NAME.equalsIgnoreCase(parent.target().name())
            ? effective(parent, visited, origin)
            : launchTargetService.effective(parent);

        return merge(base, child);
    }

    /**
     * @return the profile at the end of the chain of parents, the first one that does not have this target.
     */
    private Profile root(Profile profile) throws HeadlessMcException {
        Set<String> visited = new LinkedHashSet<>();
        Profile current = profile;
        while (NAME.equalsIgnoreCase(current.target().name())) {
            if (!visited.add(current.name().toLowerCase(Locale.ENGLISH))) {
                throw new LaunchException("Profile " + profile.name() + " inherits from itself");
            }

            current = parent(current, profile);
        }

        return current;
    }

    private Profile parent(Profile child) throws LaunchException {
        return parent(child, child);
    }

    /**
     * @param origin the profile that is being looked at, see {@link #effective(Profile, Set, Profile)}.
     */
    private Profile parent(Profile child, Profile origin) throws LaunchException {
        String parent = handle(child).profile();
        if (parent.equalsIgnoreCase(origin.name())) {
            return origin;
        }

        return profileService.getProfile(parent).orElseThrow(() -> new LaunchException(
            "Profile " + child.name() + " launches profile " + parent + ", which does not exist"
        ));
    }

    private static void checkSide(Profile child, Profile parent) throws LaunchException {
        if (!child.side().equals(parent.side())) {
            throw new LaunchException(
                "Profile " + child.name() + " is invalid: it is " + sideName(child) + ", but its parent "
                    + parent.name() + " is " + sideName(parent)
            );
        }
    }

    private static String sideName(Profile profile) {
        return profile.side().map(side -> side.isServer() ? "a server" : "a client")
            .orElse("neither a client nor a server");
    }

    private static Profile merge(Profile parent, Profile child) {
        List<String> vmArgs = new ArrayList<>(parent.vmArgs());
        vmArgs.addAll(child.vmArgs());

        @Nullable List<String> gameArgs = null;
        if (parent.gameArgs() != null || child.gameArgs() != null) {
            gameArgs = new ArrayList<>(parent.gameArgs() == null ? List.of() : parent.gameArgs());
            gameArgs.addAll(child.gameArgs() == null ? List.of() : child.gameArgs());
        }

        SequencedMap<String, @Nullable String> systemProperties = new LinkedHashMap<>(parent.systemProperties());
        systemProperties.putAll(child.systemProperties());

        Set<String> patchers = new LinkedHashSet<>(parent.patchers());
        patchers.addAll(child.patchers());

        return new Profile(
            child.name(),
            child.side(),
            parent.target(),
            child.path(),
            child.options().mergeWithDefaults(parent.options()),
            List.copyOf(patchers),
            systemProperties,
            vmArgs,
            gameArgs,
            child.javaVersion() == null ? parent.javaVersion() : child.javaVersion(),
            child.eulaStatus(),
            // whether the default arguments are part of the arguments is a property of the inherited arguments
            parent.hasDefaultClientJvmArgs(),
            child.hmcVersion()
        );
    }

    @SuppressWarnings("unchecked")
    private static <V> void copy(TypedMap.Entry<V> entry, TypedMap destination) {
        destination.put(entry.key(), entry.value());
    }

    /**
     * The data of the {@value #NAME} launch target.
     *
     * @param profile the name of the profile that is launched, the parent.
     */
    @RegisterForReflection
    public record Handle(String profile) implements ReflectionRegistered {
        @SuppressWarnings("ConstantValue")
        public Handle {
            Objects.requireNonNull(profile, "Launch target data is missing 'profile'");
        }
    }

}
