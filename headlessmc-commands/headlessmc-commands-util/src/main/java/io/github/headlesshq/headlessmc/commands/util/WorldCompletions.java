package io.github.headlesshq.headlessmc.commands.util;

import io.github.headlesshq.headlessmc.console.Completions;
import io.github.headlesshq.headlessmc.launcher.profile.ProfileService;
import io.github.headlesshq.headlessmc.launcher.profile.WorldService;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Default;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

// This does some IO, could be slow!!!
@Default
@Dependent
@RegisterForReflection
@Named("headlessmc:picocli:completions:worlds") // prevent Bean from getting removed by Quarkus during build
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class WorldCompletions implements Completions {
    private final ProfileService profileService;
    private final WorldService worldService;

    @Override
    public Iterable<Candidate> candidates(@Nullable Object spec, Line line) {
        return allWorlds()
            .map(world -> new Candidate(world, null))
            .toList();
    }

    @Override
    public Iterator<String> iterator() {
        return allWorlds().iterator();
    }

    private Stream<String> allWorlds() {
        return profileService.getProfiles()
            .stream()
            .map(worldService::listWorlds)
            .flatMap(List::stream)
            .map(Path::getFileName)
            .map(Path::toString)
            .distinct()
            .sorted(String.CASE_INSENSITIVE_ORDER);
    }

}
