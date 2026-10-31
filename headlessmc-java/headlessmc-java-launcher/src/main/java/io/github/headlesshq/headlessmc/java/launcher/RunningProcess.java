package io.github.headlesshq.headlessmc.java.launcher;

import io.github.headlesshq.headlessmc.reflection.ReflectionRegistered;
import io.quarkus.runtime.annotations.RegisterForReflection;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.time.Instant;

/**
 * A process registered with the {@link JavaProcessManager}.
 *
 * @param id        identifies the process, e.g. the id of the launched version.
 * @param pid       the process id.
 * @param started   when the process was started, used to detect reused process ids,
 *                  or {@code null} if the operating system does not provide it.
 * @param directory the directory the process is running in.
 */
@RegisterForReflection
public record RunningProcess(
    String id,
    long pid,
    @Nullable Instant started,
    Path directory
) implements ReflectionRegistered {

}
