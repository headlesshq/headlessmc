package io.github.headlesshq.headlessmc.commands.java;

import io.github.headlesshq.headlessmc.reflection.ReflectionRegistered;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.util.List;

/**
 * The updates found by the {@link JavaUpdater}, as cached on disk.
 *
 * @param updates the installations for which a newer runtime is available.
 */
@RegisterForReflection
public record JavaUpdates(List<Entry> updates) implements ReflectionRegistered {
    /**
     * @param installation the name of the outdated installation inside the HeadlessMc java directory.
     * @param latest       the name of the newer runtime, which is also the directory it gets installed to.
     */
    @RegisterForReflection
    public record Entry(String installation, String latest) implements ReflectionRegistered {

    }

}
