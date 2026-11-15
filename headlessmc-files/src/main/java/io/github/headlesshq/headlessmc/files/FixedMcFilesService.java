package io.github.headlesshq.headlessmc.files;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@link McFilesService} over a fixed list of directories, see {@link McFilesService#of(McFiles, McFiles...)}.
 */
final class FixedMcFilesService extends AbstractMcFilesService {
    private final McFiles primary;
    private final List<McFiles> all;

    FixedMcFilesService(McFiles primary, List<McFiles> additional) {
        this.primary = primary;
        List<McFiles> all = new ArrayList<>(additional.size() + 1);
        all.add(primary);
        for (McFiles mcFiles : additional) {
            if (all.stream().noneMatch(mcFiles::isSame)) {
                all.add(mcFiles);
            }
        }

        this.all = List.copyOf(all);
    }

    @Override
    public McFiles getPrimary() {
        return primary;
    }

    @Override
    public List<McFiles> all() {
        return all;
    }

}
