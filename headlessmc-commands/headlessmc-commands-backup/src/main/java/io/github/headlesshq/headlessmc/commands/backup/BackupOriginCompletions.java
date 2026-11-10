package io.github.headlesshq.headlessmc.commands.backup;

import io.github.headlesshq.headlessmc.launcher.backup.BackupOrigin;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Default;
import jakarta.inject.Named;

import java.util.Arrays;
import java.util.Iterator;

@Default
@Dependent
@RegisterForReflection
@Named("headlessmc:picocli:completions:backup-origins") // prevent Bean from getting removed by Quarkus during build
public class BackupOriginCompletions implements Iterable<String> {
    @Override
    public Iterator<String> iterator() {
        return Arrays.stream(BackupOrigin.values())
            .map(BackupOrigin::getName)
            .iterator();
    }

}
