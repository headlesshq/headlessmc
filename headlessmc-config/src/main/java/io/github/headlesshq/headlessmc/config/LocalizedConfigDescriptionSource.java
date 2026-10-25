package io.github.headlesshq.headlessmc.config;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcIOException;
import io.quarkus.runtime.annotations.RegisterResources;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;
import java.util.Properties;

/**
 * Provides localized {@link ConfigDescriptionSource config descriptions}.
 *
 * <p>Looks up a file named {@code config-descriptions_<locale>.properties}, for
 * example {@code config-descriptions_zh_CN.properties}. The locale comes from
 * {@link I18nService#locale()}, so {@code hmc.i18n.locale} selects the language.</p>
 *
 * <p>Has a higher priority than {@link DefaultConfigDescriptionSource}, so that
 * a translated description wins over the English one. Properties without a
 * translation are still served by the default source.</p>
 */
@ApplicationScoped
@Priority(100)
@RegisterResources(globs = LocalizedConfigDescriptionSource.RESOURCE_PATTERN)
public class LocalizedConfigDescriptionSource implements ConfigDescriptionSource {
    static final String RESOURCE_PATTERN = "config-descriptions_*.properties";
    private static final String RESOURCE_PREFIX = "config-descriptions_";
    private static final String RESOURCE_SUFFIX = ".properties";

    private final @Nullable Properties properties;

    @Inject
    public LocalizedConfigDescriptionSource(I18nService i18nService) {
        this.properties = load(i18nService.locale());
    }

    private static @Nullable Properties load(Locale locale) {
        String resource = RESOURCE_PREFIX + toBundleSuffix(locale) + RESOURCE_SUFFIX;
        try (InputStream inputStream = LocalizedConfigDescriptionSource.class
            .getClassLoader()
            .getResourceAsStream(resource)) {
            if (inputStream == null) {
                return null;
            }

            Properties properties = new Properties();
            properties.load(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            return properties;
        } catch (IOException e) {
            throw new HeadlessMcIOException("Failed to read resource " + resource, e);
        }
    }

    /**
     * Builds the part of the resource name identifying the locale, in the
     * {@code language_COUNTRY} form used by {@link java.util.ResourceBundle}.
     *
     * @param locale the locale to build the suffix for.
     * @return the suffix, for example {@code zh_CN}.
     */
    static String toBundleSuffix(Locale locale) {
        return locale.getCountry().isEmpty()
            ? locale.getLanguage()
            : locale.getLanguage() + "_" + locale.getCountry();
    }

    @Override
    public Optional<String> getDescription(String name) {
        return properties == null
            ? Optional.empty()
            : Optional.ofNullable(properties.getProperty(name));
    }
}
