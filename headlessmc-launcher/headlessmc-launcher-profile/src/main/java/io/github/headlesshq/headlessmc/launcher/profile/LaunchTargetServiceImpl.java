package io.github.headlesshq.headlessmc.launcher.profile;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.launcher.process.LaunchException;
import io.github.headlesshq.headlessmc.launcher.process.ProcessLauncher;
import io.github.headlesshq.headlessmc.util.typemap.TypedMap;
import io.github.headlesshq.headlessmc.version.arg.VersionArg;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Default implementation of {@link LaunchTargetService}, discovers all {@link LaunchTarget} beans.
 */
@Slf4j
@ApplicationScoped
public class LaunchTargetServiceImpl implements LaunchTargetService {
    private final Iterable<LaunchTarget> targets;
    private final ProfileService profileService;

    @Inject
    public LaunchTargetServiceImpl(@Any Instance<LaunchTarget> targets, ProfileService profileService) {
        this.targets = targets;
        this.profileService = profileService;
    }

    /**
     * @param targets        the launch targets, read every time they are needed, so the collection may change.
     * @param profileService the service to store profiles with.
     */
    public LaunchTargetServiceImpl(Iterable<LaunchTarget> targets, ProfileService profileService) {
        this.targets = targets;
        this.profileService = profileService;
    }

    @Override
    public Optional<LaunchTarget> getTarget(String name) {
        List<LaunchTarget> result = getTargets().stream()
            .filter(target -> target.getName().equalsIgnoreCase(name))
            .toList();

        if (result.size() > 1) {
            throw new IllegalStateException("There are multiple launch targets with the name " + name + ": " + result);
        }

        return result.stream().findFirst();
    }

    @Override
    public List<LaunchTarget> getTargets() {
        return StreamSupport.stream(targets.spliterator(), false)
            .sorted(Comparator.comparing(LaunchTarget::getName))
            .toList();
    }

    @Override
    public LaunchTarget require(String name) throws IllegalArgumentException {
        return getTarget(name).orElseThrow(() -> new IllegalArgumentException(
            "Failed to find launch target '" + name + "', available: " + names(getTargets()) + "."
        ));
    }

    @Override
    public LaunchTarget require(Profile profile) throws LaunchException {
        return getTarget(profile.target().name()).orElseThrow(() -> new LaunchException(
            "Launch target '" + profile.target().name() + "' of profile " + profile.name()
                + " is not available, has a plugin been removed? Available: " + names(getTargets()) + "."
        ));
    }

    @Override
    public TargetMatch parse(@Nullable String targetName, TargetArgs args) throws IllegalArgumentException {
        if (targetName != null) {
            LaunchTarget target = require(targetName);
            ParsedTarget parsed;
            try {
                parsed = target.parse(args);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                    "Launch target " + target.getName() + " cannot launch '" + args + "': " + e.getMessage()
                        + ". Usage: " + target.getUsage(), e
                );
            }

            if (!args.allows(parsed.side())) {
                throw new IllegalArgumentException(
                    "'" + args + "' is " + describe(parsed.side()) + ", which this command does not launch."
                );
            }

            return new TargetMatch(target, parsed);
        }

        List<TargetMatch> matches = new ArrayList<>();
        Map<LaunchTarget, String> reasons = new LinkedHashMap<>();
        for (LaunchTarget target : getTargets()) {
            if (!target.isDetectable()) {
                continue;
            }

            try {
                ParsedTarget parsed = target.parse(args);
                if (args.allows(parsed.side())) {
                    matches.add(new TargetMatch(target, parsed));
                } else {
                    reasons.put(target, "is " + describe(parsed.side()) + ", which this command does not launch");
                }
            } catch (IllegalArgumentException e) {
                log.debug("Launch target {} cannot launch '{}'", target.getName(), args, e);
                reasons.put(target, String.valueOf(e.getMessage()));
            }
        }

        if (matches.size() > 1 && matches.stream().anyMatch(match -> !match.parsed().fallback())) {
            // a fallback is only used if nothing else can launch the arguments
            matches.removeIf(match -> match.parsed().fallback());
        }

        if (matches.size() == 1) {
            return matches.getFirst();
        }

        if (matches.isEmpty()) {
            StringBuilder message = new StringBuilder("No launch target can launch '").append(args).append("':");
            reasons.forEach((target, reason) -> message
                .append(System.lineSeparator())
                .append(" - ").append(target.getName()).append(": ").append(reason)
                .append(" (").append(target.getUsage()).append(")")
            );

            throw new IllegalArgumentException(message.toString());
        }

        throw new IllegalArgumentException(
            "'" + args + "' is ambiguous, it can be launched by "
                + names(matches.stream().map(TargetMatch::target).toList())
                + ". Specify which one to use with --target <name>."
        );
    }

    @Override
    public List<ArgCandidate> complete(@Nullable String targetName, TargetArgs args) {
        if (targetName != null) {
            return getTarget(targetName).map(target -> complete(target, args)).orElse(List.of());
        }

        Set<String> values = new LinkedHashSet<>();
        List<ArgCandidate> result = new ArrayList<>();
        for (LaunchTarget target : getTargets()) {
            if (!target.isDetectable()) {
                continue;
            }

            for (ArgCandidate candidate : complete(target, args)) {
                if (values.add(candidate.value())) {
                    result.add(candidate);
                }
            }
        }

        return result;
    }

    private List<ArgCandidate> complete(LaunchTarget target, TargetArgs args) {
        try {
            return target.complete(args);
        } catch (RuntimeException e) {
            log.debug("Launch target {} failed to complete '{}'", target.getName(), args, e);
            return List.of();
        }
    }

    @Override
    public TargetInfo describe(Profile profile) {
        Optional<LaunchTarget> target = getTarget(profile.target().name());
        if (target.isEmpty()) {
            return TargetInfo.EMPTY;
        }

        try {
            return target.get().describe(profile);
        } catch (HeadlessMcException e) {
            log.debug("Failed to describe profile {}", profile.name(), e);
            return TargetInfo.EMPTY;
        }
    }

    @Override
    public VersionArg requireVersion(Profile profile) throws IllegalArgumentException {
        if (getTarget(profile.target().name()).isEmpty()) {
            throw new IllegalArgumentException(
                "The version of profile " + profile.name() + " is not known, its launch target '"
                    + profile.target().name() + "' is not available."
            );
        }

        TargetInfo info = describe(profile);
        VersionArg version = info.version().orElseThrow(() -> new IllegalArgumentException(
            "Profile " + profile.name() + " (" + getLabel(profile) + ") does not launch a known Minecraft version."
        ));

        if (!info.isVersionKnown()) {
            throw new IllegalArgumentException(
                "The Minecraft version of profile " + profile.name() + " (" + version + ") is not known yet, "
                    + "launch it once or run 'profile update " + profile.name() + "'."
            );
        }

        return version;
    }

    @Override
    public String getLabel(Profile profile) {
        Optional<LaunchTarget> target = getTarget(profile.target().name());
        if (target.isEmpty()) {
            return profile.target().name();
        }

        try {
            return target.get().getLabel(profile);
        } catch (HeadlessMcException e) {
            log.debug("Failed to get label of profile {}", profile.name(), e);
            return profile.target().name();
        }
    }

    @Override
    public Profile effective(Profile profile) throws HeadlessMcException {
        return require(profile).effective(profile);
    }

    @Override
    public Optional<Profile> findProfile(TargetMatch match) {
        LaunchTarget target = match.target();
        return profileService.getProfiles().stream()
            .filter(profile -> target.getName().equalsIgnoreCase(profile.target().name()))
            .filter(profile -> profile.side().equals(match.parsed().side()))
            .filter(profile -> {
                try {
                    return target.isSameTarget(profile, match.parsed());
                } catch (HeadlessMcException e) {
                    log.debug("Failed to compare profile {}", profile.name(), e);
                    return false;
                }
            })
            .findFirst();
    }

    @Override
    public Profile create(@Nullable String name, TargetMatch match, TypedMap options) throws IllegalArgumentException {
        String profileName = name;
        if (profileName == null) {
            profileName = match.target().defaultName(match.parsed().handle()).orElseThrow(
                () -> new IllegalArgumentException("Please specify a name for the profile with --name.")
            );
        }

        return match.target().create(profileName, match.parsed(), options);
    }

    @Override
    public Profile createTransient(TargetMatch match, TypedMap options) {
        LaunchTarget target = match.target();
        String name = target.defaultName(match.parsed().handle()).orElse(target.getName());
        Profile profile = target.create(name, match.parsed(), options);
        if (target.defaultName(match.parsed().handle()).isEmpty()) {
            // e.g. "jar-paper.jar", something more telling than the name of the target
            String label = target.getLabel(profile).replaceAll("[^A-Za-z0-9._-]+", "-");
            profile = profile.withName(label.toLowerCase(Locale.ENGLISH));
        }

        return profile;
    }

    @Override
    public Profile resolve(Profile profile, TypedMap options) throws HeadlessMcException {
        Profile resolved = require(profile).resolve(profile, options);
        if (!resolved.side().equals(profile.side())) {
            throw new LaunchException(
                "Launch target " + profile.target().name() + " changed the side of profile " + profile.name()
            );
        }

        return resolved;
    }

    @Override
    public Profile resolveAndPersist(Profile profile, TypedMap options) throws HeadlessMcException {
        Profile resolved = resolve(profile, options);
        //noinspection ObjectEquality
        if (resolved != profile && !resolved.equals(profile) && isStored(profile)) {
            log.debug("Saving resolved profile {}", resolved.name());
            return profileService.save(resolved);
        }

        return resolved;
    }

    /**
     * A profile with the name of a stored profile is not necessarily that profile:
     * it can be a profile created for a single launch, or a stored profile with changes for a single launch.
     * Neither must replace the stored profile.
     */
    private boolean isStored(Profile profile) {
        return profileService.getProfile(profile.name()).filter(profile::equals).isPresent();
    }

    @Override
    public ProcessLauncher launcher(Profile profile, TypedMap options) throws HeadlessMcException {
        return require(profile).launcher(profile, options);
    }

    private static String names(List<LaunchTarget> targets) {
        return targets.stream().map(LaunchTarget::getName).sorted().collect(Collectors.joining(", "));
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private static String describe(Optional<io.github.headlesshq.headlessmc.version.arg.Side> side) {
        return side.map(s -> s.isServer() ? "a server" : "a client").orElse("neither a client nor a server");
    }

}
