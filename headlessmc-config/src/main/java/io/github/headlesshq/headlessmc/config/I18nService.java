package io.github.headlesshq.headlessmc.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Resolves the {@link Locale} and {@link ResourceBundle} used to localize
 * user facing texts.
 *
 * <p>The locale is taken from {@link I18nConfig#locale()} if that is set,
 * and from {@link Locale#getDefault()} otherwise. If no bundle exists for the
 * resolved locale, {@link #bundle()} returns {@code null}, which makes picocli
 * fall back to the texts given in the annotations. This keeps English the
 * default for everyone who has no translation installed.</p>
 */
@Slf4j
@ApplicationScoped
public class I18nService {
    /**
     * Base names of the bundles holding localized texts, in lookup order.
     */
    public static final List<String> BUNDLE_NAMES = List.of(
        "io.github.headlesshq.headlessmc.i18n.Messages"
    );

    private final ConfigService configService;

    @Inject
    public I18nService(ConfigService configService) {
        this.configService = configService;
    }

    /**
     * The configured locale, or the JVM default locale if none is configured.
     *
     * @return the locale to localize texts for, never {@code null}.
     */
    public Locale locale() {
        String configured = configService
            .getHolder(I18nConfig.class)
            .get()
            .locale();

        if (I18nConfig.AUTO.equalsIgnoreCase(configured)) {
            return Locale.getDefault();
        }

        return parseLocale(configured);
    }

    /**
     * The resource bundle for {@link #locale()}, or {@code null} if there is no
     * translation for that locale.
     *
     * <p>Picocli accepts {@code null} and then uses the descriptions from the
     * annotations, so a missing translation degrades to English instead of
     * failing.</p>
     *
     * @return the bundle to use, or {@code null}.
     */
    public @Nullable ResourceBundle bundle() {
        Locale locale = locale();
        for (String name : BUNDLE_NAMES) {
            ResourceBundle bundle = getBundleOrNull(name, locale);
            if (bundle != null) {
                return bundle;
            }
        }

        return null;
    }

    private static @Nullable ResourceBundle getBundleOrNull(String name, Locale locale) {
        try {
            return ResourceBundle.getBundle(name, locale);
        } catch (MissingResourceException e) {
            log.debug("No resource bundle {} for locale {}", name, locale);
            return null;
        }
    }

    /**
     * Parses a locale given either as a language tag ({@code zh-CN}) or in the
     * underscore form used by {@link ResourceBundle} ({@code zh_CN}).
     *
     * @param value the configured value.
     * @return the parsed locale, or {@link Locale#getDefault()} if it cannot be parsed.
     */
    static Locale parseLocale(String value) {
        Locale locale = Locale.forLanguageTag(value.replace('_', '-'));
        if (locale.getLanguage().isEmpty()) {
            log.warn("Failed to parse locale '{}', falling back to the default locale", value);
            return Locale.getDefault();
        }

        return locale;
    }
}
