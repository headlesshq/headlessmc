package io.github.headlesshq.headlessmc.version.id;

import java.util.List;

public interface VersionIDMapper {
    String getName();

    VersionID map(String name);

    boolean matches(String name);

    default void resolveConflicts(String name, List<VersionIDMapper> readers) {
        // to be implemented by subclasses that expect a conflict
    }

}
