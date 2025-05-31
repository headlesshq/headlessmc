package io.github.headlesshq.headlessmc.launcher.version;

import io.github.headlesshq.headlessmc.launcher.version.family.FamilyCleaner;
import io.github.headlesshq.headlessmc.launcher.version.family.FamilyUtil;
import lombok.CustomLog;
import lombok.val;

import java.util.HashSet;
import java.util.Map;

@CustomLog
final class ParentVersionResolver {
    public void resolveParentVersions(Map<String, Version> versions) {
        val invalid = new HashSet<Version>();
        FamilyUtil.resolveParents(versions.values(), version -> {
            val parentName = version.getParentName();
            if (parentName == null) {
                return null;
            }

            val parent = versions.get(parentName);
            if (parent == null) {
                log.warning("Couldn't find parent version "
                                + parentName
                                + " for version "
                                + version.getName()
                                + "!");
                invalid.add(version);
                return null;
            }

            return parent;
        });

        val cleaner = new FamilyCleaner<Version>();
        cleaner.clean(versions.values(), invalid);
    }

}
