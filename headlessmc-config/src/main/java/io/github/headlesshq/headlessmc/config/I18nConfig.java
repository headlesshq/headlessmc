package io.github.headlesshq.headlessmc.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

/**
 * Configuration for the localization (i18n) of HeadlessMc.
 */
@ConfigMapping(prefix = I18nConfig.PREFIX)
public interface I18nConfig extends DynamicConfig {
    String PREFIX = "hmc.i18n";

    /**
     * The locale to use for localized texts, for example {@code zh_CN}.
     *
     * <p>When set to {@link #AUTO}, the {@link java.util.Locale#getDefault() default locale}
     * of the JVM is used, falling back to English if no translation for it is available.</p>
     *
     * @return the configured locale or {@link #AUTO}.
     */
    @WithName("locale")
    @WithDefault(AUTO)
    String locale();

    /**
     * Value of {@link #locale()} telling HeadlessMc to follow the JVM default locale.
     */
    String AUTO = "auto";
}
