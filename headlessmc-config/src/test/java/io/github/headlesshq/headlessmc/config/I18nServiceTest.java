package io.github.headlesshq.headlessmc.config;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.ResourceBundle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
public class I18nServiceTest {
    private static final String LOCALE = I18nConfig.PREFIX + ".locale";

    @Inject
    ConfigServiceImpl configService;

    @Inject
    I18nService i18nService;

    @BeforeEach
    public void setup() {
        configService.remove(LOCALE);
    }

    @Test
    public void autoFollowsTheDefaultLocale() {
        configService.set(LOCALE, I18nConfig.AUTO, false);
        assertEquals(Locale.getDefault(), i18nService.locale());
    }

    @Test
    public void unsetLocaleBehavesLikeAuto() {
        assertEquals(Locale.getDefault(), i18nService.locale());
    }

    @Test
    public void configuredLocaleWins() {
        configService.set(LOCALE, "zh_CN", false);
        assertEquals(Locale.SIMPLIFIED_CHINESE, i18nService.locale());
    }

    @Test
    public void acceptsLanguageTagForm() {
        configService.set(LOCALE, "zh-CN", false);
        assertEquals(Locale.SIMPLIFIED_CHINESE, i18nService.locale());
    }

    @Test
    public void parsesLanguageOnly() {
        assertEquals(Locale.CHINESE, I18nService.parseLocale("zh"));
    }

    @Test
    public void unparsableLocaleFallsBackToTheUntranslatedDefault() {
        assertEquals(Locale.ROOT, I18nService.parseLocale("---"));
    }

    @Test
    public void bundleIsLoadedForAConfiguredLocaleWithTranslation() {
        configService.set(LOCALE, "zh_CN", false);
        ResourceBundle bundle = i18nService.bundle();
        assertNotNull(bundle);
        assertEquals("显示此帮助信息并退出。", bundle.getString("help"));
    }

    @Test
    public void bundleIsNullForALocaleWithoutTranslation() {
        configService.set(LOCALE, "ja_JP", false);
        assertNull(i18nService.bundle());
    }

    @Test
    public void bundleSuffixUsesLanguageAndCountry() {
        assertEquals("zh_CN", LocalizedConfigDescriptionSource.toBundleSuffix(Locale.SIMPLIFIED_CHINESE));
        assertEquals("zh", LocalizedConfigDescriptionSource.toBundleSuffix(Locale.CHINESE));
    }

    @Test
    public void localizedConfigDescriptionsAreUsed() {
        configService.set(LOCALE, "zh_CN", false);
        LocalizedConfigDescriptionSource source = new LocalizedConfigDescriptionSource(i18nService);
        assertEquals(
            "每次下载的重试次数（0 表示不重试）。",
            source.getDescription("hmc.net.retries").orElse(null)
        );
    }

    @Test
    public void untranslatedConfigDescriptionsAreSkipped() {
        configService.set(LOCALE, "zh_CN", false);
        LocalizedConfigDescriptionSource source = new LocalizedConfigDescriptionSource(i18nService);
        // Descriptions missing from the localized file are served by the default source.
        assertTrue(source.getDescription("hmc.nonexistent").isEmpty());
    }

}
