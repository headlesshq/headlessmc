package io.github.headlesshq.headlessmc.commands.mod;

import io.github.headlesshq.headlessmc.console.Completions;
import io.github.headlesshq.headlessmc.platform.Platform;
import io.github.headlesshq.headlessmc.platform.PlatformService;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Default;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;

import java.util.Iterator;

@Default
@Dependent
@RegisterForReflection
@Named("headlessmc:picocli:completions:mod:platforms") // prevent Bean from getting removed by Quarkus during build
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class PlatformCompletions implements Completions {
    private final PlatformService platformService;

    @Override
    public Iterable<Candidate> candidates(@Nullable Object spec, Line line) {
        return platformService.getPlatforms().stream()
            .map(platform -> new Candidate(platform.getName(), null))
            .toList();
    }

    @Override
    public Iterator<String> iterator() {
        return platformService.getPlatforms().stream()
            .map(Platform::getName)
            .iterator();
    }

}
