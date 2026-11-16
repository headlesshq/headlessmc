package io.github.headlesshq.headlessmc.commands.util;

import io.github.headlesshq.headlessmc.console.Completions;
import io.github.headlesshq.headlessmc.launcher.profile.ArgCandidate;
import io.github.headlesshq.headlessmc.launcher.profile.LaunchTarget;
import io.github.headlesshq.headlessmc.launcher.profile.LaunchTargetService;
import io.github.headlesshq.headlessmc.launcher.profile.Profile;
import io.github.headlesshq.headlessmc.launcher.profile.ProfileService;
import io.github.headlesshq.headlessmc.launcher.profile.TargetArgs;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Default;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Completes the arguments of a {@link LaunchTargetCommand}.
 * The arguments are completed by the {@link LaunchTarget}s themselves, see
 * {@link LaunchTarget#complete(TargetArgs)}: by the launch target named with {@code --target},
 * or else by all launch targets that could be meant.
 * For commands that accept them, the names of stored profiles are offered for the first argument.
 */
@Slf4j
@Default
@Dependent
@RegisterForReflection
@Named("headlessmc:picocli:completions:launch-targets") // prevent Bean from getting removed by Quarkus during build
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class LaunchTargetCompletions implements Completions {
    private final LaunchTargetService launchTargetService;
    private final ProfileService profileService;

    @Override
    public Iterable<Candidate> candidates(@Nullable Object spec, Line line) {
        if (!(spec instanceof LaunchTargetCommand command)) {
            log.error("Spec for launch target completions {} was not a LaunchTargetCommand", spec);
            return List.of();
        }

        @Nullable String target = command.getTarget();
        @Nullable List<String> typed = command.getTargetArgs();
        TargetArgs args = new TargetArgs(typed == null ? List.of() : typed, command.getSides());
        List<Candidate> result = new ArrayList<>();
        Set<String> names = new HashSet<>();
        if (target == null && command.acceptsProfileNames() && args.args().size() <= 1) {
            for (Profile profile : profileService.getProfiles()) {
                if (args.allows(profile.side()) && names.add(profile.name())) {
                    result.add(new Candidate(profile.name(), null));
                }
            }
        }

        for (ArgCandidate candidate : launchTargetService.complete(target, args)) {
            if (names.add(candidate.value())) {
                result.add(new Candidate(candidate.value(), candidate.description()));
            }
        }

        return result;
    }

    @Override
    public Iterator<String> iterator() {
        return Collections.emptyIterator();
    }

}
