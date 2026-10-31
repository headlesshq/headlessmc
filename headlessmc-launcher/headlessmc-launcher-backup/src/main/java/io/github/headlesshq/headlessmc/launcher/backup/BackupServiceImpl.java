package io.github.headlesshq.headlessmc.launcher.backup;

import io.github.headlesshq.headlessmc.config.Holder;
import io.github.headlesshq.headlessmc.exceptions.HeadlessMcIOException;
import io.github.headlesshq.headlessmc.files.AppFiles;
import io.github.headlesshq.headlessmc.files.FileService;
import io.github.headlesshq.headlessmc.reflection.ReflectionRegistered;
import io.github.headlesshq.headlessmc.util.json.JsonParseException;
import io.github.headlesshq.headlessmc.util.json.JsonService;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Stores each {@link Backup} as a zip archive and a json file with its metadata
 * in a directory per provider and target inside the {@link #getBackupDir() backup directory}.
 */
@Slf4j
@ApplicationScoped
public class BackupServiceImpl implements BackupService {
    private static final DateTimeFormatter ID_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    private static final String JSON = ".json";
    private static final String ZIP = ".zip";

    private final Holder<BackupConfig> config;
    private final BackupProviderService providerService;
    private final BackupArchiveService archiveService;
    private final JsonService jsonService;
    private final FileService fileService;
    private final AppFiles appFiles;
    private final Clock clock;

    @Inject
    public BackupServiceImpl(
        Holder<BackupConfig> config,
        BackupProviderService providerService,
        BackupArchiveService archiveService,
        JsonService jsonService,
        FileService fileService,
        AppFiles appFiles
    ) {
        this(config, providerService, archiveService, jsonService, fileService, appFiles, Clock.systemDefaultZone());
    }

    public BackupServiceImpl(
        Holder<BackupConfig> config,
        BackupProviderService providerService,
        BackupArchiveService archiveService,
        JsonService jsonService,
        FileService fileService,
        AppFiles appFiles,
        Clock clock
    ) {
        this.config = config;
        this.providerService = providerService;
        this.archiveService = archiveService;
        this.jsonService = jsonService;
        this.fileService = fileService;
        this.appFiles = appFiles;
        this.clock = clock;
    }

    @Override
    public Backup create(BackupRequest request) {
        BackupProvider provider = providerService.getProvider(request.provider());
        BackupPlan plan = provider.plan(request);
        List<String> ignored = new ArrayList<>();
        if (request.defaultIgnore()) {
            ignored.addAll(config.get().ignore());
            ignored.addAll(plan.defaultIgnored());
        }

        ignored.addAll(request.ignore());
        BackupScope scope = new BackupScope(plan.source(), plan.paths(), normalize(ignored));
        if (!request.force()) {
            provider.getUsage(scope, plan.profile()).ifPresent(reason -> {
                throw new BackupInUseException(reason + ", stop it first or force the backup.");
            });
        }

        Backup backup = write(provider, plan, scope, request.origin());
        if (request.origin().isAutomatic()) {
            pruneAutomatic(backup);
        }

        return backup;
    }

    @Override
    public List<Backup> list() {
        Path dir = getBackupDir();
        if (!Files.isDirectory(dir)) {
            return List.of();
        }

        try (Stream<Path> paths = Files.walk(dir, 3)) {
            return paths.filter(path -> path.getFileName().toString().endsWith(JSON))
                .filter(Files::isRegularFile)
                .map(this::readBackup)
                .flatMap(Optional::stream)
                .sorted(Comparator.comparing(Backup::created).thenComparing(Backup::id))
                .toList();
        } catch (IOException e) {
            log.error("Failed to list backups in {}", dir, e);
            return List.of();
        }
    }

    @Override
    public List<Backup> list(@Nullable String provider, @Nullable String target) {
        return list().stream()
            .filter(backup -> provider == null || backup.provider().equalsIgnoreCase(provider))
            .filter(backup -> target == null || backup.target().equalsIgnoreCase(target))
            .toList();
    }

    @Override
    public Optional<Backup> get(String id) {
        return list().stream()
            .filter(backup -> backup.id().equals(id))
            .findFirst();
    }

    @Override
    public BackupDiff diff(Backup backup, boolean checksum) {
        List<String> paths = providerService.findProvider(backup.provider())
            .map(provider -> provider.getDiffPaths(backup))
            .orElse(backup.paths());
        return archiveService.diff(getArchive(backup), backup.scope(), paths, checksum, getBackupDir());
    }

    @Override
    public RestoreResult restore(Backup backup, RestoreOptions options) {
        BackupProvider provider = providerService.getProvider(backup.provider());
        BackupScope scope = backup.scope();
        if (!options.force()) {
            provider.getUsage(scope, backup.profile()).ifPresent(reason -> {
                throw new BackupInUseException(reason + ", stop it first or force the restore.");
            });
        }

        Path archive = getArchive(backup);
        BackupDiff changes = archiveService.diff(archive, scope, backup.paths(), true, getBackupDir());
        if (options.keepNew()) {
            changes = new BackupDiff(List.of(), changes.removed(), changes.modified());
        }

        Backup safetyBackup = null;
        if (!changes.isEmpty()) {
            if (options.backupFirst() && archiveService.hasFiles(scope, getBackupDir())) {
                BackupPlan plan = provider.replan(backup);
                safetyBackup = write(provider, plan, new BackupScope(plan.source(), plan.paths(), scope.ignored()), BackupOrigin.RESTORE);
            }

            archiveService.restore(archive, scope, changes);
        }

        provider.afterRestore(backup);
        if (safetyBackup != null) {
            pruneAutomatic(safetyBackup);
        }

        return new RestoreResult(changes, safetyBackup);
    }

    @Override
    public void remove(Backup backup) {
        Path dir = getTargetDir(backup.provider(), backup.target());
        fileService.delete(getArchive(backup));
        fileService.delete(fileService.getPath(dir, backup.id() + JSON));
        deleteIfEmpty(dir);
        deleteIfEmpty(dir.getParent());
    }

    @Override
    public List<Backup> prune(PruneRequest request) {
        Instant cutoff = request.olderThan() == null ? null : clock.instant().minus(request.olderThan());
        Map<String, List<Backup>> groups = list(request.provider(), request.target()).stream()
            .filter(backup -> request.origins().isEmpty() || request.origins().contains(backup.origin()))
            .collect(Collectors.groupingBy(
                backup -> backup.provider() + "/" + backup.target(),
                LinkedHashMap::new,
                Collectors.toList()
            ));

        List<Backup> removed = new ArrayList<>();
        for (List<Backup> group : groups.values()) {
            List<Backup> newestFirst = new ArrayList<>(group);
            newestFirst.sort(Comparator.comparing(Backup::created).thenComparing(Backup::id).reversed());
            for (int i = 0; i < newestFirst.size(); i++) {
                Backup backup = newestFirst.get(i);
                boolean beyondKeep = request.keep() == null || i >= request.keep();
                boolean old = cutoff == null || backup.created().isBefore(cutoff);
                if (beyondKeep && old) {
                    remove(backup);
                    removed.add(backup);
                }
            }
        }

        return removed;
    }

    @Override
    public Path getArchive(Backup backup) {
        return fileService.getPath(getTargetDir(backup.provider(), backup.target()), backup.archive());
    }

    @Override
    public Path getBackupDir() {
        return config.get().dir()
            .map(fileService::getUserPath)
            .orElseGet(appFiles::getBackupDir);
    }

    private Backup write(BackupProvider provider, BackupPlan plan, BackupScope scope, BackupOrigin origin) {
        Path source = scope.source();
        if (!Files.isDirectory(source)) {
            throw new IllegalArgumentException("Directory " + source + " of " + plan.target() + " does not exist.");
        }

        Instant created = clock.instant();
        Path dir = getTargetDir(provider.getName(), plan.target());
        String id = createId(dir, plan.target(), created);
        Path archive = dir.resolve(id + ZIP);
        Path temp = dir.resolve(id + ZIP + ".tmp");
        try {
            Files.createDirectories(dir);
            BackupArchiveService.ArchiveResult result = archiveService.write(scope, temp, getBackupDir());
            fileService.saveMove(temp, archive);
            Backup backup = new Backup(
                id, provider.getName(), plan.target(), plan.type(), origin, created, plan.profile(),
                source, scope.paths(), scope.ignored(), archive.getFileName().toString(),
                result.files(), result.size(), Backup.BACKUP_VERSION
            );

            jsonService.write(dir.resolve(id + JSON), backup, true);
            return backup;
        } catch (IOException e) {
            deleteQuietly(temp, e);
            deleteQuietly(archive, e);
            throw new HeadlessMcIOException("Failed to create backup " + id + " of " + source, e);
        } catch (RuntimeException e) {
            deleteQuietly(temp, e);
            deleteQuietly(archive, e);
            throw e;
        }
    }

    private void pruneAutomatic(Backup backup) {
        int keep = config.get().autoKeep();
        if (keep > 0) {
            prune(new PruneRequest(backup.provider(), backup.target(), BackupOrigin.AUTOMATIC, keep, null));
        }
    }

    private List<String> normalize(List<String> ignored) {
        return ignored.stream()
            .map(String::trim)
            .filter(pattern -> !pattern.isEmpty())
            .distinct()
            .toList();
    }

    private String createId(Path dir, String target, Instant created) {
        String base = toFileName(target) + "-" + ID_FORMAT.format(LocalDateTime.ofInstant(created, clock.getZone()));
        Set<String> ids = list().stream().map(Backup::id).collect(Collectors.toSet());
        String id = base;
        for (int i = 2; ids.contains(id) || Files.exists(dir.resolve(id + JSON)) || Files.exists(dir.resolve(id + ZIP)); i++) {
            id = base + "-" + i;
        }

        return id;
    }

    private Path getTargetDir(String provider, String target) {
        return fileService.getPath(getBackupDir(), toFileName(provider), toFileName(target));
    }

    private String toFileName(String name) {
        return name.toLowerCase(Locale.ENGLISH).replaceAll("[^a-z0-9._-]", "-");
    }

    private void deleteIfEmpty(@Nullable Path dir) {
        if (dir == null || !Files.isDirectory(dir)) {
            return;
        }

        try (Stream<Path> entries = Files.list(dir)) {
            if (entries.findFirst().isEmpty()) {
                fileService.delete(dir);
            }
        } catch (IOException e) {
            log.warn("Failed to check if backup directory {} is empty", dir, e);
        }
    }

    private Optional<Backup> readBackup(Path json) {
        try {
            BackupVersionObject version = jsonService.parse(json, BackupVersionObject.class);
            if (version.hmcVersion() > Backup.BACKUP_VERSION) {
                log.warn(
                    "Backup {} is from a newer version {}, expected {}, ignoring, update HeadlessMc",
                    json, version.hmcVersion(), Backup.BACKUP_VERSION
                );

                return Optional.empty();
            }

            return Optional.of(jsonService.parse(json, Backup.class));
        } catch (JsonParseException e) {
            log.error("Failed to read backup {}", json, e);
            return Optional.empty();
        }
    }

    private void deleteQuietly(Path file, Exception cause) {
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            cause.addSuppressed(e);
        }
    }

    // check first to see if we are an outdated headlessmc version (Backup.BACKUP_VERSION)
    @RegisterForReflection
    private record BackupVersionObject(int hmcVersion) implements ReflectionRegistered {

    }

}
