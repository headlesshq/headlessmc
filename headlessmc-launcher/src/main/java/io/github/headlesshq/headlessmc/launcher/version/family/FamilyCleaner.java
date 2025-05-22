package io.github.headlesshq.headlessmc.launcher.version.family;

import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import io.github.headlesshq.headlessmc.api.traits.HasName;

import java.util.Collection;
import java.util.Set;

// TODO:
@CustomLog
@RequiredArgsConstructor
public class FamilyCleaner<T extends HasParent<T> & HasName> {
    public void clean(Collection<T> input, Set<T> invalid) {
        // TODO: check if this can be improved
        input.forEach(v -> {
            if (invalid.contains(v)) {
                return;
            }

            Family<T> result = FamilyUtil.getFamily(v);
            checkMembers(result, invalid);
            checkCircle(result, invalid);
        });

        input.removeIf(invalid::contains);
    }

    private void checkMembers(Family<T> family, Set<T> invalids) {
        if (invalids.stream().anyMatch(e -> family.getMembers().contains(e))) {
            family.getMembers().forEach(member -> {
                if (invalids.add(member)) {
                    log.warning(member.getName()
                                    + "'s family has malformed "
                                    + "members, ignoring it.");
                }
            });
        }
    }

    private void checkCircle(Family<T> family, Set<T> invalids) {
        if (family.isCircular()) {
            family.getMembers().forEach(parent -> {
                if (invalids.add(parent)) {
                    log.warning("Parents of "
                                    + parent.getName()
                                    + " are circular!");
                }
            });
        }
    }

}
