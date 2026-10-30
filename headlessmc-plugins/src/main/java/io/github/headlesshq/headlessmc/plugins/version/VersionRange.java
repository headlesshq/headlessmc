package io.github.headlesshq.headlessmc.plugins.version;

import org.jspecify.annotations.Nullable;

import java.util.Optional;

/**
 * A range of {@link Version}s, using the Maven range syntax:
 * <ul>
 *     <li>{@code [1.0,2.0)}: {@code 1.0 <= x < 2.0}</li>
 *     <li>{@code (1.0,]} or {@code (1.0,)}: {@code x > 1.0}</li>
 *     <li>{@code [1.0]}: exactly {@code 1.0}</li>
 *     <li>{@code *}: any version</li>
 * </ul>
 * Unlike Maven, a bare version {@code 1.2.3} means {@code [1.2.3,2)}, i.e. any compatible version of the same major.
 * An exclusive upper bound without qualifier excludes its pre-releases, too:
 * {@code [1.0,2)} does not contain {@code 2.0.0-alpha}.
 *
 * @param lower          the lower bound, or {@code null} if unbounded.
 * @param lowerInclusive whether the lower bound is part of the range.
 * @param upper          the upper bound, or {@code null} if unbounded.
 * @param upperInclusive whether the upper bound is part of the range.
 */
public record VersionRange(
    @Nullable Version lower,
    boolean lowerInclusive,
    @Nullable Version upper,
    boolean upperInclusive
) {
    /**
     * The range containing all versions.
     */
    public static final VersionRange ANY = new VersionRange(null, false, null, false);

    /**
     * Parses a range.
     *
     * @param value the range to parse.
     * @return the parsed range.
     * @throws IllegalArgumentException if the range is invalid.
     */
    public static VersionRange parse(String value) {
        String range = value.trim();
        if (range.isEmpty() || range.equals("*")) {
            return ANY;
        }

        char first = range.charAt(0);
        if (first != '[' && first != '(') {
            return compatible(Version.parse(range));
        }

        char last = range.charAt(range.length() - 1);
        if (last != ']' && last != ')') {
            throw new IllegalArgumentException("Invalid version range '" + value + "', missing closing ] or )");
        }

        String content = range.substring(1, range.length() - 1);
        int comma = content.indexOf(',');
        if (comma < 0) {
            if (first != '[' || last != ']') {
                throw new IllegalArgumentException("Invalid version range '" + value + "', use [version] for an exact version");
            }

            Version version = Version.parse(content);
            return new VersionRange(version, true, version, true);
        }

        if (content.indexOf(',', comma + 1) >= 0) {
            throw new IllegalArgumentException("Invalid version range '" + value + "', unions of ranges are not supported");
        }

        String lower = content.substring(0, comma).trim();
        String upper = content.substring(comma + 1).trim();
        return new VersionRange(
            lower.isEmpty() ? null : Version.parse(lower), first == '[',
            upper.isEmpty() ? null : Version.parse(upper), last == ']'
        );
    }

    /**
     * @param version the minimum version.
     * @return {@code [version, nextMajor)}.
     */
    public static VersionRange compatible(Version version) {
        return new VersionRange(version, true, version.nextMajor(), false);
    }

    /**
     * @param version the version to check.
     * @return {@code true} if this range contains the given version.
     */
    public boolean contains(Version version) {
        Version lower = this.lower;
        if (lower != null) {
            int result = version.compareTo(lower);
            if (result < 0 || result == 0 && !lowerInclusive) {
                return false;
            }
        }

        Version upper = this.upper;
        if (upper != null) {
            int result = version.compareTo(upper);
            if (result > 0 || result == 0 && !upperInclusive) {
                return false;
            }

            return upperInclusive || !upper.qualifier().isEmpty() || version.qualifier().isEmpty() || !version.sameNumbers(upper);
        }

        return true;
    }

    /**
     * @param other the range to intersect with.
     * @return the range of versions contained in both ranges, or empty if there are none.
     */
    public Optional<VersionRange> intersect(VersionRange other) {
        Version lower = this.lower;
        boolean lowerInclusive = this.lowerInclusive;
        Version otherLower = other.lower;
        if (lower == null || otherLower != null && otherLower.compareTo(lower) > 0) {
            lower = otherLower;
            lowerInclusive = other.lowerInclusive;
        } else if (otherLower != null && otherLower.compareTo(lower) == 0) {
            lowerInclusive &= other.lowerInclusive;
        }

        Version upper = this.upper;
        boolean upperInclusive = this.upperInclusive;
        Version otherUpper = other.upper;
        if (upper == null || otherUpper != null && otherUpper.compareTo(upper) < 0) {
            upper = otherUpper;
            upperInclusive = other.upperInclusive;
        } else if (otherUpper != null && otherUpper.compareTo(upper) == 0) {
            upperInclusive &= other.upperInclusive;
        }

        if (lower != null && upper != null) {
            int result = lower.compareTo(upper);
            if (result > 0 || result == 0 && !(lowerInclusive && upperInclusive)) {
                return Optional.empty();
            }
        }

        return Optional.of(new VersionRange(lower, lowerInclusive, upper, upperInclusive));
    }

    @Override
    public String toString() {
        if (lower == null && upper == null) {
            return "*";
        }

        if (lower != null && lower.equals(upper) && lowerInclusive && upperInclusive) {
            return "[" + lower + "]";
        }

        return (lowerInclusive ? "[" : "(")
            + (lower == null ? "" : lower.toString())
            + ","
            + (upper == null ? "" : upper.toString())
            + (upperInclusive ? "]" : ")");
    }

}
