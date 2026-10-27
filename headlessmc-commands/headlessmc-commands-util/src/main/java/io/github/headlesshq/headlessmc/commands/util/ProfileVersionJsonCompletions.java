package io.github.headlesshq.headlessmc.commands.util;

import io.github.headlesshq.headlessmc.console.Completions;
import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.version.service.VersionJsonService;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Default;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Like {@link ProfileVersionCompletions}, but also completes the names of installed version.jsons
 * for the first argument, for commands that can launch a version.json directly.
 */
@Slf4j
@Default
@Dependent
@RegisterForReflection
@Named("headlessmc:picocli:completions:profile-version-jsons") // prevent Bean from getting removed by Quarkus during build
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class ProfileVersionJsonCompletions implements Completions {
    private final ProfileVersionCompletions profileVersionCompletions;
    private final VersionJsonService versionJsonService;

    @Override
    public Iterable<Candidate> candidates(@Nullable Object spec, Line line) {
        List<Candidate> result = new ArrayList<>();
        Set<String> names = new HashSet<>();
        for (Candidate candidate : profileVersionCompletions.candidates(spec, line)) {
            result.add(candidate);
            names.add(candidate.getName());
        }

        @Nullable List<String> args = spec instanceof VersionArgCommand command ? command.getVersionArg() : null;
        if (spec instanceof VersionArgCommand && (args == null || args.size() <= 1)) {
            try {
                versionJsonService.getInstalledFileNames().stream()
                    .sorted()
                    .filter(names::add)
                    .map(name -> new Candidate(name, null))
                    .forEach(result::add);
            } catch (HeadlessMcException e) {
                log.error("Failed to list installed versions for completions", e);
            }
        }

        return result;
    }

    @Override
    public Iterator<String> iterator() {
        return profileVersionCompletions.iterator();
    }

}
