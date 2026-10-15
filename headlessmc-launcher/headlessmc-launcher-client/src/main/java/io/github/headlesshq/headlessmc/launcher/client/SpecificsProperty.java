package io.github.headlesshq.headlessmc.launcher.client;

import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * A config property understood by <a href="https://github.com/headlesshq/hmc-specifics">hmc-specifics</a>,
 * the mod HeadlessMc uses to control the game.
 * hmc-specifics reads these from SystemProperties of the game process,
 * so the {@link SpecificsPropertiesService} forwards them from the HeadlessMc config.
 *
 * @param name         the name of the property, the same in HeadlessMc and in hmc-specifics.
 * @param type         the type of the property, used to convert the HeadlessMc config value.
 * @param defaultValue the default value hmc-specifics uses if the property is not set,
 *                     {@code null} if the property has no default.
 */
public record SpecificsProperty(String name, Class<?> type, @Nullable Object defaultValue) {
    public static final SpecificsProperty JLINE_ENABLED = new SpecificsProperty("hmc.jline.enabled", Boolean.class, true);
    public static final SpecificsProperty JLINE_DUMB = new SpecificsProperty("hmc.jline.dumb", Boolean.class, false);
    public static final SpecificsProperty DEBUG_JLINE = new SpecificsProperty("hmc.debug.jline", Boolean.class, false);
    public static final SpecificsProperty ACCOUNT_REFRESH_ENABLED = new SpecificsProperty("hmc.account.refresh.enabled", Boolean.class, true);
    public static final SpecificsProperty ACCOUNT_REFRESH_INTERVAL = new SpecificsProperty("hmc.account.refresh.interval", Long.class, 23L * 60L * 60L);
    public static final SpecificsProperty RESOURCE_PACKS_AUTO_ACCEPT = new SpecificsProperty("hmc.resourcepacks.autoaccept", Boolean.class, false);
    public static final SpecificsProperty FILES_LOCATION = new SpecificsProperty("hmc.files.location", String.class, null);
    public static final SpecificsProperty LOG4J_APPENDER_ANSI = new SpecificsProperty("hmc.log4jappender.ansi", Boolean.class, true);
    public static final SpecificsProperty ADVENTURE_ENABLED = new SpecificsProperty("hmc.adventure.enabled", Boolean.class, true);
    public static final SpecificsProperty ADVENTURE_OUTPUT_THROWABLES = new SpecificsProperty("hmc.adventure.output.throwables", Boolean.class, true);
    public static final SpecificsProperty ADVENTURE_COLOR_NONE = new SpecificsProperty("hmc.adventure.color.none", Boolean.class, false);

    public static final List<SpecificsProperty> ALL = List.of(
        JLINE_ENABLED,
        JLINE_DUMB,
        DEBUG_JLINE,
        ACCOUNT_REFRESH_ENABLED,
        ACCOUNT_REFRESH_INTERVAL,
        RESOURCE_PACKS_AUTO_ACCEPT,
        FILES_LOCATION,
        LOG4J_APPENDER_ANSI,
        ADVENTURE_ENABLED,
        ADVENTURE_OUTPUT_THROWABLES,
        ADVENTURE_COLOR_NONE
    );

}
