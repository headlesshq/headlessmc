package io.github.headlesshq.headlessmc.commands.mod;

import io.github.headlesshq.headlessmc.console.Completions;
import io.github.headlesshq.headlessmc.mods.distribution.ModDistributionPlatform;
import io.github.headlesshq.headlessmc.mods.distribution.ModDistributionPlatformService;
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
@Named("headlessmc:picocli:completions:mod:distributions") // prevent Bean from getting removed by Quarkus during build
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class ModDistributionPlatformCompletions implements Completions {
    private final ModDistributionPlatformService modDistributionPlatformService;

    @Override
    public Iterable<Candidate> candidates(@Nullable Object spec, Line line) {
        return modDistributionPlatformService.platforms()
            .map(platform -> new Candidate(platform.getName(), null))
            .toList();
    }

    @Override
    public Iterator<String> iterator() {
        return modDistributionPlatformService.platforms()
            .map(ModDistributionPlatform::getName)
            .iterator();
    }

}
