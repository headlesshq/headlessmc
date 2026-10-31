package io.github.headlesshq.headlessmc.launcher.backup;

import java.nio.file.Path;
import java.util.List;

/**
 * The files of a backup.
 *
 * @param source  the directory to back up.
 * @param paths   the directories, relative to {@code source}, to back up.
 *                An empty string stands for the entire {@code source} directory.
 * @param ignored glob patterns of paths, relative to {@code source}, to leave out, see {@link BackupConfig#ignore()}.
 */
public record BackupScope(Path source, List<String> paths, List<String> ignored) {

}
