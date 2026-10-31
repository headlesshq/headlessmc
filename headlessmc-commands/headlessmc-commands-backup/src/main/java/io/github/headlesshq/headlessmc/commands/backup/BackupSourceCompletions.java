package io.github.headlesshq.headlessmc.commands.backup;

import io.github.headlesshq.headlessmc.launcher.backup.BackupProviderService;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Default;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.RequiredArgsConstructor;

import java.util.Iterator;

/**
 * Completes everything the {@link io.github.headlesshq.headlessmc.launcher.backup.BackupProvider}s can back up.
 */
@Default
@Dependent
@RegisterForReflection
@Named("headlessmc:picocli:completions:backup-sources") // prevent Bean from getting removed by Quarkus during build
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class BackupSourceCompletions implements Iterable<String> {
    private final BackupProviderService providerService;

    @Override
    public Iterator<String> iterator() {
        return providerService.getProviders().stream()
            .flatMap(provider -> provider.getTargets().stream())
            .distinct()
            .iterator();
    }

}
