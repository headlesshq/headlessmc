package io.github.headlesshq.headlessmc.commands.backup;

import io.github.headlesshq.headlessmc.launcher.backup.Backup;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Duration;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Formats {@link Backup}s for the backup commands and parses their arguments.
 */
@ApplicationScoped
public class BackupFormatter {
    private static final DateTimeFormatter CREATED_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Pattern DURATION = Pattern.compile("(\\d+)\\s*([smhdw])");
    private static final String[] UNITS = {"B", "KiB", "MiB", "GiB", "TiB"};

    /**
     * @param bytes an amount of bytes.
     * @return the amount in a human-readable unit, e.g. {@code 1.5 MiB}.
     */
    public String size(long bytes) {
        double value = bytes;
        int unit = 0;
        while (value >= 1024 && unit < UNITS.length - 1) {
            value /= 1024;
            unit++;
        }

        return unit == 0 ? bytes + " B" : String.format(Locale.ENGLISH, "%.1f %s", value, UNITS[unit]);
    }

    /**
     * @param backup the backup.
     * @return when the backup was created, in the local time zone.
     */
    public String created(Backup backup) {
        return CREATED_FORMAT.format(backup.created().atZone(ZoneId.systemDefault()));
    }

    /**
     * @param backup the backup.
     * @return the directories the backup contains, {@code *} for the entire directory.
     */
    public String contents(Backup backup) {
        if (backup.paths().equals(List.of(""))) {
            return "*";
        }

        return String.join(", ", backup.paths());
    }

    /**
     * Parses a duration like {@code 30d}, {@code 12h}, {@code 90m}, {@code 2w} or {@code 1d12h}.
     *
     * @param duration the duration to parse.
     * @return the parsed duration.
     * @throws IllegalArgumentException if the duration cannot be parsed.
     */
    public Duration duration(String duration) {
        String normalized = duration.trim().toLowerCase(Locale.ENGLISH);
        Matcher matcher = DURATION.matcher(normalized);
        Duration result = Duration.ZERO;
        int end = 0;
        while (matcher.find() && matcher.start() == end) {
            long amount = Long.parseLong(matcher.group(1));
            result = result.plus(switch (matcher.group(2)) {
                case "s" -> Duration.ofSeconds(amount);
                case "m" -> Duration.ofMinutes(amount);
                case "h" -> Duration.ofHours(amount);
                case "d" -> Duration.ofDays(amount);
                default -> Duration.ofDays(amount * 7);
            });
            end = matcher.end();
        }

        if (end == 0 || end != normalized.length()) {
            throw new IllegalArgumentException(
                "Failed to parse duration '" + duration + "', expected e.g. 30d, 12h, 90m or 2w."
            );
        }

        return result;
    }

}
