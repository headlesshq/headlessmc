package io.github.headlesshq.headlessmc.launcher.backup.profile;

import io.github.headlesshq.headlessmc.launcher.backup.*;
import io.github.headlesshq.headlessmc.launcher.profile.Profile;
import io.github.headlesshq.headlessmc.launcher.profile.ProfileResolver;
import io.github.headlesshq.headlessmc.launcher.profile.ProfileService;
import io.github.headlesshq.headlessmc.launcher.profile.WorldService;
import io.github.headlesshq.headlessmc.version.arg.Side;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Backs up profiles and servers, which are both {@link Profile}s,
 * either entirely or only their worlds, see {@link ProfileBackupType}.
 */
@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class ProfileBackupProvider implements BackupProvider {
    /**
     * The name of this provider.
     */
    public static final String NAME = BackupProviderService.DEFAULT_PROVIDER;
    /**
     * Directories of a client game directory that are left out of {@link ProfileBackupType#FULL} backups,
     * because HeadlessMc can download them again.
     */
    public static final List<String> DEFAULT_CLIENT_IGNORED = List.of("assets", "libraries", "versions");

    private final ProfileResolver profileResolver;
    private final ProfileService profileService;
    private final WorldService worldService;
    private final ProfileUsageService usageService;
    private final BackupArchiveService archiveService;

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDescription() {
        return "Profiles and servers";
    }

    @Override
    public List<String> getTypes() {
        return Arrays.stream(ProfileBackupType.values()).map(ProfileBackupType::getName).toList();
    }

    @Override
    public List<String> getTargets() {
        return profileService.getProfiles().stream()
            .map(Profile::name)
            .sorted(String.CASE_INSENSITIVE_ORDER)
            .toList();
    }

    @Override
    public BackupPlan plan(BackupRequest request) {
        Profile profile = request.profile() == null ? resolve(request.target()) : request.profile();
        Path source = profile.path();
        if (!Files.isDirectory(source)) {
            throw new IllegalArgumentException(
                "Game directory " + source + " of profile " + profile.name() + " does not exist."
            );
        }

        String world = request.world();
        ProfileBackupType type = request.type() != null
            ? ProfileBackupType.parse(request.type())
            : world != null ? ProfileBackupType.WORLD : ProfileBackupType.FULL;
        if (world != null && type != ProfileBackupType.WORLD) {
            throw new IllegalArgumentException("A world can only be specified for backups of type world.");
        }

        List<String> paths = switch (type) {
            case FULL -> List.of("");
            case WORLDS -> {
                List<Path> worlds = worldService.listWorlds(profile);
                if (worlds.isEmpty()) {
                    throw new IllegalArgumentException("Failed to find any worlds for profile " + profile.name());
                }

                yield worlds.stream().map(dir -> archiveService.relativize(source, dir)).toList();
            }
            case WORLD -> List.of(archiveService.relativize(source, worldService.resolveWorld(profile, world)));
        };

        List<String> defaultIgnored = type == ProfileBackupType.FULL && profile.side().isClient()
            ? DEFAULT_CLIENT_IGNORED
            : List.of();
        return new BackupPlan(profile.name(), type.getName(), profile, source, paths, defaultIgnored);
    }

    @Override
    public BackupPlan replan(Backup backup) {
        Path source = backup.source().toAbsolutePath().normalize();
        Profile profile = profileService.getProfile(backup.target())
            .filter(current -> current.path().toAbsolutePath().normalize().equals(source))
            .orElse(backup.profile());
        return new BackupPlan(backup.target(), backup.type(), profile, backup.source(), backup.paths(), List.of());
    }

    @Override
    public Optional<String> getUsage(BackupScope scope, @Nullable Profile profile) {
        return usageService.getUsage(scope, profile);
    }

    /**
     * Worlds created after a {@link ProfileBackupType#WORLDS} backup are part of its diff as well.
     */
    @Override
    public List<String> getDiffPaths(Backup backup) {
        Profile profile = backup.profile();
        if (profile == null || !ProfileBackupType.WORLDS.getName().equals(backup.type())) {
            return backup.paths();
        }

        Set<String> paths = new LinkedHashSet<>(backup.paths());
        for (Path world : worldService.listWorlds(profile.withPath(backup.source()))) {
            paths.add(archiveService.relativize(backup.source(), world));
        }

        return List.copyOf(paths);
    }

    /**
     * Restoring a {@link ProfileBackupType#FULL} backup also restores the profile.
     */
    @Override
    public void afterRestore(Backup backup) {
        Profile profile = backup.profile();
        if (profile != null && ProfileBackupType.FULL.getName().equals(backup.type())) {
            profileService.save(profile);
        }
    }

    private Profile resolve(String target) {
        List<String> args = Arrays.stream(target.trim().split("\\s+")).filter(arg -> !arg.isEmpty()).toList();
        if (args.isEmpty()) {
            throw new IllegalArgumentException("Please specify the profile/server to back up.");
        }

        return profileResolver.resolve(args, Side.BOTH);
    }

}
