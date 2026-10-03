package io.github.headlesshq.headlessmc.config;

import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.config.Config;
import io.smallrye.config.PropertiesConfigSource;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.SysPropConfigSource;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the config.properties file support of the {@link ConfigServiceImpl}.
 */
@QuarkusTest
public class ConfigFileTest {
    private static final String PROP = "hmc.test.properties.test-property";
    private static final String INT = "hmc.test.properties.test-int";

    @Inject
    @Any
    Instance<DynamicConfig> dynamicConfigs;

    @TempDir
    Path dir;

    @AfterEach
    public void clearSystemProperties() {
        System.clearProperty(PROP);
        System.clearProperty(INT);
    }

    private Path file() {
        return dir.resolve("config.properties");
    }

    private void writeFile(String content) throws Exception {
        Files.writeString(file(), content, StandardCharsets.UTF_8);
    }

    private String readFile() throws Exception {
        return Files.readString(file(), StandardCharsets.UTF_8);
    }

    private ConfigServiceImpl newConfigServiceWithoutFile() {
        SmallRyeConfig base = new SmallRyeConfigBuilder()
            .withSources(new PropertiesConfigSource(
                Map.of(PROP, "fromApplicationProperties", INT, "1"), "application.properties", 250
            ))
            .withSources(new SysPropConfigSource())
            .build();
        return new ConfigServiceImpl(dynamicConfigs, new ConcurrentHashMap<>(), base);
    }

    /**
     * @return a new ConfigService using {@link #file()}, simulating a (re)start of HeadlessMc.
     */
    private ConfigServiceImpl newConfigService() {
        ConfigServiceImpl configService = newConfigServiceWithoutFile();
        configService.loadConfigFile(file());
        return configService;
    }

    private static String prop(ConfigService configService) {
        String value = configService.getConfig().getValue(PROP, String.class);
        assertEquals(value, configService.getHolder(TestConfig.class).get().testProperty());
        return value;
    }

    // --- loading ---

    @Test
    public void withoutAConfigFileThereIsNoConfigFile() {
        assertEquals(Optional.empty(), newConfigServiceWithoutFile().getConfigFile());
    }

    @Test
    public void aMissingConfigFileIsFineAndNotCreatedByLoading() {
        ConfigServiceImpl configService = newConfigService();
        assertEquals(Optional.of(file()), configService.getConfigFile());
        assertEquals("fromApplicationProperties", prop(configService));
        assertFalse(Files.exists(file()));
    }

    @Test
    public void configFileTakesPrecedenceOverApplicationProperties() throws Exception {
        writeFile("# comment\n" + PROP + "=fromFile\n" + INT + "=5\n");
        ConfigServiceImpl configService = newConfigService();
        assertEquals("fromFile", prop(configService));
        assertEquals(5, configService.getHolder(TestConfig.class).get().testInt());
    }

    @Test
    public void systemPropertiesTakePrecedenceOverTheConfigFile() throws Exception {
        writeFile(PROP + "=fromFile\n");
        System.setProperty(PROP, "fromSystem");
        assertEquals("fromSystem", prop(newConfigService()));
    }

    @Test
    public void configFileIsUsedAgainWhenTheSystemPropertyIsGone() throws Exception {
        writeFile(PROP + "=fromFile\n");
        System.setProperty(PROP, "fromSystem");
        assertEquals("fromSystem", prop(newConfigService()));
        System.clearProperty(PROP);
        assertEquals("fromFile", prop(newConfigService()));
    }

    @Test
    public void configFileSourceHasTheExpectedOrdinal() throws Exception {
        writeFile(PROP + "=fromFile\n");
        Config config = newConfigService().getConfig();
        assertEquals(ConfigServiceImpl.CONFIG_FILE_ORDINAL, config.getConfigValue(PROP).getSourceOrdinal());
        assertTrue(ConfigServiceImpl.CONFIG_FILE_ORDINAL > 250, "must beat application.properties");
        assertTrue(ConfigServiceImpl.CONFIG_FILE_ORDINAL < 300, "env variables must beat the config file");
        assertTrue(ConfigServiceImpl.CONFIG_FILE_ORDINAL < 400, "system properties must beat the config file");
    }

    @Test
    public void loadingKeepsPropertiesSetAtRuntime() throws Exception {
        writeFile(PROP + "=fromFile\n" + INT + "=5\n");
        ConfigServiceImpl configService = newConfigServiceWithoutFile();
        configService.set(PROP, "fromSet", false);
        configService.loadConfigFile(file());
        assertEquals("fromSet", prop(configService));
        assertEquals(5, configService.getHolder(TestConfig.class).get().testInt());
    }

    @Test
    public void invalidValueInTheConfigFileFailsLoadingAndKeepsTheOldConfig() throws Exception {
        writeFile(INT + "=notAnInt\n");
        ConfigServiceImpl configService = newConfigServiceWithoutFile();
        Config before = configService.getConfig();

        ConfigException e = assertThrows(ConfigException.class, () -> configService.loadConfigFile(file()));

        assertTrue(e.getMessage().contains(file().toString()), e.getMessage());
        assertSame(before, configService.getConfig());
        assertEquals(Optional.empty(), configService.getConfigFile());
    }

    @Test
    public void malformedConfigFileFailsLoadingWithAConfigException() throws Exception {
        writeFile(PROP + "=\\uZZZZ\n");
        ConfigServiceImpl configService = newConfigServiceWithoutFile();
        assertThrows(ConfigException.class, () -> configService.loadConfigFile(file()));
    }

    @Test
    public void unreadableConfigFileFailsLoadingWithAConfigException() throws Exception {
        Files.createDirectories(file());
        ConfigServiceImpl configService = newConfigServiceWithoutFile();
        assertThrows(ConfigException.class, () -> configService.loadConfigFile(file()));
    }

    // --- set(persist = true) ---

    @Test
    public void persistentSetWritesTheConfigFileAndTakesEffectImmediately() throws Exception {
        ConfigServiceImpl configService = newConfigService();
        assertTrue(configService.set(PROP, "persisted", true));
        assertEquals("persisted", prop(configService));
        assertEquals(Map.of(PROP, "persisted"), PropertiesFile.load(file()));
    }

    @Test
    public void persistentSetSurvivesARestart() {
        newConfigService().set(PROP, "persisted", true);
        assertEquals("persisted", prop(newConfigService()));
    }

    @Test
    public void persistentSetKeepsTheRestOfTheConfigFile() throws Exception {
        writeFile("# === HeadlessMc Config ===\n# hmc.jline.enabled=true\n" + INT + "=5\n");
        newConfigService().set(PROP, "persisted", true);
        assertEquals(
            "# === HeadlessMc Config ===\n# hmc.jline.enabled=true\n" + INT + "=5\n" + PROP + "=persisted\n",
            readFile()
        );
    }

    @Test
    public void persistentSetReplacesTheExistingEntry() throws Exception {
        writeFile("# header\n" + PROP + "=old\n" + INT + "=5\n");
        newConfigService().set(PROP, "new", true);
        assertEquals("# header\n" + PROP + "=new\n" + INT + "=5\n", readFile());
    }

    @Test
    public void persistentSetTakesEffectImmediatelyEvenIfASystemPropertyIsSet() {
        System.setProperty(PROP, "fromSystem");
        ConfigServiceImpl configService = newConfigService();
        configService.set(PROP, "persisted", true);
        assertEquals("persisted", prop(configService));
        // but after a restart, the system property takes precedence again
        assertEquals("fromSystem", prop(newConfigService()));
    }

    @Test
    public void persistentSetOfTheSameValueTwiceOnlyChangesOnce() throws Exception {
        ConfigServiceImpl configService = newConfigService();
        assertTrue(configService.set(PROP, "persisted", true));
        Config before = configService.getConfig();
        String file = readFile();

        assertFalse(configService.set(PROP, "persisted", true));

        assertSame(before, configService.getConfig());
        assertEquals(file, readFile());
    }

    @Test
    public void persistentSetOfAValueThatIsAlreadyInTheFileButNotInMemoryChanges() throws Exception {
        writeFile(PROP + "=fromFile\n");
        ConfigServiceImpl configService = newConfigService();
        assertTrue(configService.set(PROP, "fromFile", true));
        assertEquals("fromFile", prop(configService));
        assertFalse(configService.set(PROP, "fromFile", true));
    }

    @Test
    public void persistentSetAfterATemporarySetOfTheSameValueStillWritesTheFile() throws Exception {
        ConfigServiceImpl configService = newConfigService();
        assertTrue(configService.set(PROP, "value", false));
        assertFalse(Files.exists(file()));
        assertTrue(configService.set(PROP, "value", true), "the file still changes");
        assertEquals(Map.of(PROP, "value"), PropertiesFile.load(file()));
    }

    @Test
    public void invalidPersistentSetThrowsAndChangesNothing() throws Exception {
        writeFile(INT + "=5\n");
        ConfigServiceImpl configService = newConfigService();
        Config before = configService.getConfig();
        String file = readFile();

        assertThrows(ConfigException.class, () -> configService.set(INT, "notAnInt", true));

        assertSame(before, configService.getConfig());
        assertEquals(file, readFile());
        assertEquals(5, configService.getHolder(TestConfig.class).get().testInt());
        assertEquals(5, newConfigService().getHolder(TestConfig.class).get().testInt());
    }

    @Test
    public void persistentSetWithoutAConfigFileThrowsAndChangesNothing() {
        ConfigServiceImpl configService = newConfigServiceWithoutFile();
        Config before = configService.getConfig();
        assertThrows(ConfigException.class, () -> configService.set(PROP, "persisted", true));
        assertSame(before, configService.getConfig());
    }

    @Test
    public void failingToWriteTheConfigFileThrowsAndChangesNothing() throws Exception {
        ConfigServiceImpl configService = newConfigService();
        Config before = configService.getConfig();
        Files.createDirectories(file()); // cannot write a file where a directory is

        assertThrows(ConfigException.class, () -> configService.set(PROP, "persisted", true));

        assertSame(before, configService.getConfig());
        assertEquals("fromApplicationProperties", prop(configService));
    }

    @Test
    public void persistentSetCreatesMissingParentDirectories() {
        ConfigServiceImpl configService = newConfigServiceWithoutFile();
        Path nested = dir.resolve("does").resolve("not").resolve("exist").resolve("config.properties");
        configService.loadConfigFile(nested);
        configService.set(PROP, "persisted", true);
        assertTrue(Files.exists(nested));
    }

    // --- set(persist = false) ---

    @Test
    public void temporarySetDoesNotWriteTheConfigFile() throws Exception {
        writeFile("# header\n" + PROP + "=fromFile\n");
        ConfigServiceImpl configService = newConfigService();
        assertTrue(configService.set(PROP, "temporary", false));
        assertEquals("temporary", prop(configService));
        assertEquals("# header\n" + PROP + "=fromFile\n", readFile());
        assertEquals("fromFile", prop(newConfigService()));
    }

    @Test
    public void temporarySetDoesNotCreateTheConfigFile() {
        newConfigService().set(PROP, "temporary", false);
        assertFalse(Files.exists(file()));
    }

    // --- remove ---

    @Test
    public void removeFallsBackToTheConfigFile() throws Exception {
        writeFile(PROP + "=fromFile\n");
        ConfigServiceImpl configService = newConfigService();
        configService.set(PROP, "temporary", false);
        assertTrue(configService.remove(PROP));
        assertEquals("fromFile", prop(configService));
    }

    @Test
    public void removeAfterAPersistentSetFallsBackToThePersistedValue() {
        ConfigServiceImpl configService = newConfigService();
        configService.set(PROP, "persisted", true);
        configService.set(PROP, "temporary", false);
        assertTrue(configService.remove(PROP));
        assertEquals("persisted", prop(configService));
    }

    @Test
    public void removeDoesNotTouchTheConfigFile() throws Exception {
        ConfigServiceImpl configService = newConfigService();
        configService.set(PROP, "persisted", true);
        String file = readFile();
        assertTrue(configService.remove(PROP));
        assertEquals(file, readFile());
    }

    // --- fork ---

    @Test
    public void forkInheritsTheConfigFileProperties() throws Exception {
        writeFile(PROP + "=fromFile\n");
        ConfigService fork = newConfigService().fork();
        assertEquals("fromFile", prop(fork));
    }

    @Test
    public void forkCannotPersist() throws Exception {
        writeFile(PROP + "=fromFile\n");
        ConfigService fork = newConfigService().fork();
        assertThrows(ConfigException.class, () -> fork.set(PROP, "persisted", true));
        assertEquals(PROP + "=fromFile\n", readFile());
        assertTrue(fork.set(PROP, "temporary", false));
        assertEquals("temporary", prop(fork));
    }

    @Test
    public void persistentSetOnTheParentDoesNotAffectAFork() {
        ConfigServiceImpl configService = newConfigService();
        ConfigService fork = configService.fork();
        configService.set(PROP, "persisted", true);
        assertEquals("fromApplicationProperties", prop(fork));
    }

}
