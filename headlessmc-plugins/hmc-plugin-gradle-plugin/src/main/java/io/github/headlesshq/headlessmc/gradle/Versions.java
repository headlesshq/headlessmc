package io.github.headlesshq.headlessmc.gradle;

/**
 * A simple comparison of dot or dash separated versions, numeric parts are compared as numbers.
 */
final class Versions {
    private static final String SEPARATOR = "[.\\-]";

    private Versions() {
        throw new AssertionError();
    }

    static String major(String version) {
        return version.split(SEPARATOR)[0];
    }

    static int compare(String first, String second) {
        String[] a = first.split(SEPARATOR);
        String[] b = second.split(SEPARATOR);
        for (int i = 0; i < Math.min(a.length, b.length); i++) {
            int result = isNumber(a[i]) && isNumber(b[i])
                ? Long.compare(Long.parseLong(a[i]), Long.parseLong(b[i]))
                : a[i].compareTo(b[i]);
            if (result != 0) {
                return result;
            }
        }

        return Integer.compare(a.length, b.length);
    }

    private static boolean isNumber(String part) {
        return !part.isEmpty() && part.length() < 19 && part.chars().allMatch(Character::isDigit);
    }

}
