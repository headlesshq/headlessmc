package io.github.headlesshq.headlessmc.files;

import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.Optional;

/**
 * Implements everything of {@link McFilesService} that can be derived from {@link #all()}.
 */
public abstract class AbstractMcFilesService implements McFilesService {
    private static final String VERSIONS = "versions";
    private static final String JSON = ".json";

    @Override
    public Optional<McFiles> find(Path file) {
        Path normalized = McFiles.normalize(file);
        @Nullable McFiles best = null;
        int bestDepth = -1;
        for (McFiles mcFiles : all()) {
            Path mcDir = McFiles.normalize(mcFiles.getMcDir());
            if (normalized.startsWith(mcDir) && mcDir.getNameCount() > bestDepth) {
                best = mcFiles;
                bestDepth = mcDir.getNameCount();
            }
        }

        return Optional.ofNullable(best);
    }

    @Override
    public McFiles of(Path mcDir) {
        McFiles result = McFiles.of(mcDir);
        for (McFiles mcFiles : all()) {
            if (mcFiles.isSame(result)) {
                return mcFiles;
            }
        }

        return result;
    }

    @Override
    public McFiles require(@Nullable Path versionJson) {
        if (versionJson == null) {
            return getPrimary();
        }

        Optional<McFiles> known = find(versionJson);
        if (known.isPresent()) {
            return known.get();
        }

        // <mcDir>/versions/<id>/<id>.json
        Path file = McFiles.normalize(versionJson);
        @Nullable Path versionDir = file.getParent();
        @Nullable Path versionsDir = versionDir == null ? null : versionDir.getParent();
        @Nullable Path mcDir = versionsDir == null ? null : versionsDir.getParent();
        if (mcDir != null
            && versionsDir.getFileName() != null
            && VERSIONS.equals(versionsDir.getFileName().toString())
            && versionDir.getFileName() != null
            && file.getFileName() != null
            && file.getFileName().toString().equals(versionDir.getFileName() + JSON)) {
            return McFiles.of(mcDir);
        }

        return getPrimary();
    }

}
