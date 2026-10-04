package io.github.headlesshq.headlessmc.version.util;

import java.util.Comparator;

/**
 * {@link Comparator} for version strings like {@code 1.2.1}.
 */
public final class VersionComparator implements Comparator<String> {
    public static final VersionComparator INSTANCE = new VersionComparator();

    @Override
    public int compare(String o1, String o2) {
        String[] parts1 = o1.split("\\.");
        String[] parts2 = o2.split("\\.");

        int maxLength = Math.max(parts1.length, parts2.length);
        for (int i = 0; i < maxLength; i++) {
            int p1;
            try {
                p1 = i < parts1.length ? Integer.parseInt(parts1[i]) : 0;
            } catch (NumberFormatException e) {
                return Integer.compare(0, 1);
            }

            int p2;
            try {
                p2 = i < parts2.length ? Integer.parseInt(parts2[i]) : 0;
            } catch (NumberFormatException e) {
                return Integer.compare(1, 0);
            }

            if (p1 != p2) {
                return Integer.compare(p1, p2);
            }
        }

        return 0;
    }

}
