package io.github.headlesshq.headlessmc.launcher.backup.profile;

import io.github.headlesshq.headlessmc.java.launcher.JavaProcessManager;
import io.github.headlesshq.headlessmc.java.launcher.RunningProcess;
import io.github.headlesshq.headlessmc.launcher.backup.BackupArchiveService;
import io.github.headlesshq.headlessmc.launcher.backup.BackupScope;
import io.github.headlesshq.headlessmc.launcher.profile.Profile;
import io.github.headlesshq.headlessmc.launcher.profile.WorldService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Detects if the files of a profile are in use by a running game.
 *
 * <p>A profile is in use if HeadlessMc launched a process in its game directory that is still running,
 * see {@link JavaProcessManager}, or if one of its worlds is open. Since 1.16 Minecraft locks the
 * {@code session.lock} file of a world while it is open, which also detects games launched by other launchers.
 */
@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class ProfileUsageService {
    private final JavaProcessManager processManager;
    private final WorldService worldService;

    /**
     * @param scope   the files to check.
     * @param profile the profile the files belong to, if any.
     * @return why the files are in use, or an empty {@link Optional} if they are not.
     */
    public Optional<String> getUsage(BackupScope scope, @Nullable Profile profile) {
        String name = profile == null ? scope.source().toString() : profile.name();
        List<RunningProcess> running = processManager.getRunningProcesses(scope.source());
        if (!running.isEmpty()) {
            return Optional.of(name + " is running (pid " + running.getFirst().pid() + ")");
        }

        for (Path world : getWorlds(scope, profile)) {
            if (isLocked(world.resolve(BackupArchiveService.SESSION_LOCK))) {
                return Optional.of("World " + world.getFileName() + " of " + name + " is open in a running game");
            }
        }

        return Optional.empty();
    }

    /**
     * @param sessionLock the {@code session.lock} file of a world.
     * @return {@code true} if another process holds a lock on the file.
     */
    public boolean isLocked(Path sessionLock) {
        if (!Files.isRegularFile(sessionLock)) {
            return false;
        }

        try (FileChannel channel = FileChannel.open(sessionLock, StandardOpenOption.WRITE)) {
            FileLock lock = channel.tryLock();
            if (lock == null) {
                return true;
            }

            lock.release();
            return false;
        } catch (OverlappingFileLockException e) {
            return true;
        } catch (IOException e) {
            // e.g. on Windows a locked file cannot be opened
            return true;
        }
    }

    private Set<Path> getWorlds(BackupScope scope, @Nullable Profile profile) {
        Path source = scope.source();
        Set<Path> worlds = new LinkedHashSet<>();
        for (String path : scope.paths()) {
            if (!path.isEmpty()) {
                worlds.add(source.resolve(path));
            }
        }

        if (profile != null) {
            for (Path world : worldService.listWorlds(profile.withPath(source))) {
                if (scope.paths().stream().anyMatch(path -> path.isEmpty() || world.startsWith(source.resolve(path)))) {
                    worlds.add(world);
                }
            }
        }

        return worlds;
    }

}
