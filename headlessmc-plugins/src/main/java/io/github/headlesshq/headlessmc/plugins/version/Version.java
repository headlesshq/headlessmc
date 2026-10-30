package io.github.headlesshq.headlessmc.plugins.version;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A version like {@code 1.2.3}, {@code 3.0.0-RC8} or {@code 33.0.0-jre}.
 * Versions are compared by their numeric components first, missing components count as {@code 0}.
 * A version with a qualifier is lower than the same version without one ({@code 3.0.0-RC8 < 3.0.0}),
 * qualifiers are compared token by token, numbers numerically ({@code RC8 < RC10}).
 *
 * @param value      the original string.
 * @param numbers    the leading numeric components.
 * @param qualifier  everything after the numeric components, without the separator, empty if none.
 */
public record Version(String value, List<Integer> numbers, String qualifier) implements Comparable<Version> {
    /**
     * Parses a version.
     *
     * @param value the version to parse.
     * @return the parsed version.
     * @throws IllegalArgumentException if the version does not start with a number.
     */
    public static Version parse(String value) {
        String version = value.trim();
        List<Integer> numbers = new ArrayList<>();
        int index = 0;
        while (index < version.length()) {
            int start = index;
            while (index < version.length() && Character.isDigit(version.charAt(index))) {
                index++;
            }

            if (start == index) {
                break;
            }

            numbers.add(Integer.parseInt(version.substring(start, index)));
            if (index < version.length() && version.charAt(index) == '.'
                && index + 1 < version.length() && Character.isDigit(version.charAt(index + 1))) {
                index++;
            } else {
                break;
            }
        }

        if (numbers.isEmpty()) {
            throw new IllegalArgumentException("Invalid version '" + value + "', must start with a number");
        }

        String qualifier = version.substring(index);
        if (!qualifier.isEmpty() && (qualifier.charAt(0) == '-' || qualifier.charAt(0) == '.' || qualifier.charAt(0) == '+')) {
            qualifier = qualifier.substring(1);
        }

        return new Version(version, List.copyOf(numbers), qualifier);
    }

    /**
     * @return the first numeric component.
     */
    public int major() {
        return numbers.getFirst();
    }

    /**
     * @return the next major version, {@code 2} for {@code 1.2.3}.
     */
    public Version nextMajor() {
        return parse(String.valueOf(major() + 1));
    }

    /**
     * @param other the version to compare with.
     * @return {@code true} if both versions have the same numeric components.
     */
    boolean sameNumbers(Version other) {
        return new Version(value, numbers, "").compareTo(new Version(other.value, other.numbers, "")) == 0;
    }

    @Override
    public int compareTo(Version other) {
        int size = Math.max(numbers.size(), other.numbers.size());
        for (int i = 0; i < size; i++) {
            int result = Integer.compare(get(numbers, i), get(other.numbers, i));
            if (result != 0) {
                return result;
            }
        }

        if (qualifier.isEmpty() || other.qualifier.isEmpty()) {
            return Boolean.compare(qualifier.isEmpty(), other.qualifier.isEmpty());
        }

        return compareQualifiers(qualifier, other.qualifier);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Version version && compareTo(version) == 0;
    }

    @Override
    public int hashCode() {
        List<Integer> trimmed = new ArrayList<>(numbers);
        while (trimmed.size() > 1 && trimmed.getLast() == 0) {
            trimmed.removeLast();
        }

        return 31 * trimmed.hashCode() + qualifier.toLowerCase(Locale.ROOT).hashCode();
    }

    @Override
    public String toString() {
        return value;
    }

    private static int get(List<Integer> numbers, int index) {
        return index < numbers.size() ? numbers.get(index) : 0;
    }

    private static int compareQualifiers(String first, String second) {
        List<String> firstTokens = tokenize(first);
        List<String> secondTokens = tokenize(second);
        for (int i = 0; i < Math.min(firstTokens.size(), secondTokens.size()); i++) {
            String a = firstTokens.get(i);
            String b = secondTokens.get(i);
            boolean aNumber = Character.isDigit(a.charAt(0));
            boolean bNumber = Character.isDigit(b.charAt(0));
            int result;
            if (aNumber && bNumber) {
                result = Long.compare(Long.parseLong(a), Long.parseLong(b));
            } else if (aNumber != bNumber) {
                result = aNumber ? 1 : -1;
            } else {
                result = a.compareToIgnoreCase(b);
            }

            if (result != 0) {
                return result;
            }
        }

        return Integer.compare(firstTokens.size(), secondTokens.size());
    }

    private static List<String> tokenize(String qualifier) {
        List<String> tokens = new ArrayList<>();
        StringBuilder token = new StringBuilder();
        for (char c : qualifier.toCharArray()) {
            boolean separator = c == '.' || c == '-' || c == '_' || c == '+';
            if (separator || (!token.isEmpty() && Character.isDigit(c) != Character.isDigit(token.charAt(0)))) {
                if (!token.isEmpty()) {
                    tokens.add(token.toString());
                    token.setLength(0);
                }

                if (separator) {
                    continue;
                }
            }

            token.append(c);
        }

        if (!token.isEmpty()) {
            tokens.add(token.toString());
        }

        return tokens;
    }

}
