package io.github.headlesshq.headlessmc.commands.util;

import io.github.headlesshq.headlessmc.console.Completions;
import io.github.headlesshq.headlessmc.launcher.profile.LaunchTarget;
import io.github.headlesshq.headlessmc.launcher.profile.LaunchTargetService;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Default;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;

import java.util.Iterator;
import java.util.List;

/**
 * Completes the value of the {@code --target} option with the names of the available {@link LaunchTarget}s.
 */
@Default
@Dependent
@RegisterForReflection
@Named("headlessmc:picocli:completions:launch-target-names") // prevent Bean from getting removed by Quarkus during build
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class LaunchTargetNameCompletions implements Completions {
    private final LaunchTargetService launchTargetService;

    @Override
    public Iterable<Candidate> candidates(@Nullable Object spec, Line line) {
        return launchTargetService.getTargets().stream()
            .map(target -> new Candidate(target.getName(), target.getUsage()))
            .toList();
    }

    @Override
    public Iterator<String> iterator() {
        return names().iterator();
    }

    private List<String> names() {
        return launchTargetService.getTargets().stream().map(LaunchTarget::getName).toList();
    }

}
