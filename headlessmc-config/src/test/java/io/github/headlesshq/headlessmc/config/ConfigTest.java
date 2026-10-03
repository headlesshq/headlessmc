package io.github.headlesshq.headlessmc.config;

import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.config.Config;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class ConfigTest {
    private static final String PROP = "hmc.test.properties.test-property";
    private static final String INT = "hmc.test.properties.test-int";

    @Inject
    ConfigServiceImpl configService;

    @Inject
    Holder<TestConfig> testConfig;

    @BeforeEach
    public void setup() {
        configService.remove(PROP);
        configService.remove(INT);
    }

    @Test
    public void holderIsInjectedAndReflectsBaseline() {
        TestConfig config = testConfig.get();
        assertEquals("test", config.testProperty());
        assertEquals(1, config.testInt());
    }

    @Test
    public void getConfigExposesBaselineValues() {
        Config config = configService.getConfig();
        assertEquals("test", config.getValue(PROP, String.class));
        assertEquals(1, config.getValue(INT, Integer.class));
    }

    @Test
    public void holderGetReturnsCurrentBeanOnEachCall() {
        assertEquals("test", testConfig.get().testProperty());
        configService.set(PROP, "changed", false);
        assertEquals("changed", testConfig.get().testProperty());
    }

    @Test
    public void setReturnsTrueWhenValueChanges() {
        assertTrue(configService.set(PROP, "somethingNew", false));
    }

    @Test
    public void setReturnsFalseWhenValueIsUnchanged() {
        assertTrue(configService.set(PROP, "test", false)); // first time is true
        assertFalse(configService.set(PROP, "test", false));
    }

    @Test
    public void setReturnsTrueThenFalseForRepeatedValue() {
        assertTrue(configService.set(PROP, "repeated", false));
        assertFalse(configService.set(PROP, "repeated", false));
        assertTrue(configService.set(PROP, "different", false));
    }

    @Test
    public void setUpdatesGetConfigAndHolder() {
        configService.set(PROP, "newValue", false);
        assertEquals("newValue", configService.getConfig().getValue(PROP, String.class));
        assertEquals("newValue", testConfig.get().testProperty());
    }

    @Test
    public void remoteResetsConfigAndHolder() {
        configService.set(PROP, "newValue", false);
        assertEquals("newValue", configService.getConfig().getValue(PROP, String.class));
        assertEquals("newValue", testConfig.get().testProperty());
        assertTrue(configService.remove(PROP));
        assertEquals("test", configService.getConfig().getValue(PROP, String.class));
        assertEquals("test", testConfig.get().testProperty());
        assertFalse(configService.remove(PROP));
    }

    @Test
    public void changingSetProducesANewConfigInstance() {
        Config before = configService.getConfig();
        configService.set(PROP, "brandNew", false);
        assertNotSame(before, configService.getConfig(), "a changing set() must rebuild the config");
    }

    @Test
    public void nonChangingSetKeepsTheSameConfigInstance() {
        assertTrue(configService.set(PROP, "test", false));
        Config before = configService.getConfig();
        assertFalse(configService.set(PROP, "test", false)); // unchanged
        assertSame(before, configService.getConfig(), "an unchanged set() must not rebuild the config");
    }

    @Test
    public void setUpdatesIntProperty() {
        assertTrue(configService.set(INT, "42", false));
        assertEquals(42, testConfig.get().testInt());
        assertEquals(42, configService.getConfig().getValue(INT, Integer.class));
    }

    @Test
    public void setIntToNonIntegerThrows() {
        // The value cannot be converted to int, so rebuilding the config fails validation.
        assertThrows(ConfigException.class, () -> configService.set(INT, "notAnInt", false));
    }

    @Test
    public void setIntToFloatingPointThrows() {
        assertThrows(ConfigException.class, () -> configService.set(INT, "1.5", false));
    }

    @Test
    public void setIntToEmptyStringThrows() {
        assertThrows(ConfigException.class, () -> configService.set(INT, "", false));
    }

    @Test
    public void setUnknownPropertyUnderMappedPrefixThrows() {
        assertThrows(
            ConfigException.class,
            () -> configService.set("hmc.test.properties.does-not-exist", "x", false)
        );
    }

    @Test
    public void setUnknownForeignPropertyDoesNotThrowYet() {
        // hmmm, should this be like that?
        assertTrue(configService.set("totally.foreign.property", "x", false));
    }

    @Test
    public void failedSetKeepsThePreviousValueAndConfig() {
        assertTrue(configService.set(INT, "42", false));
        Config before = configService.getConfig();

        assertThrows(ConfigException.class, () -> configService.set(INT, "notAnInt", false));

        assertSame(before, configService.getConfig(), "an invalid set() must not rebuild the config");
        assertEquals(42, testConfig.get().testInt());
        // the invalid value must not have leaked into the properties, setting the old value again is a no-op
        assertFalse(configService.set(INT, "42", false));
    }

    @Test
    public void removeOfAPropertyThatWasNeverSetReturnsFalse() {
        Config before = configService.getConfig();
        assertFalse(configService.remove("hmc.test.properties.never-set"));
        assertSame(before, configService.getConfig());
    }

    @Test
    public void setThenRemoveRoundTripsForeignPropertyNames() {
        String foreign = "some.foreign.round-trip";
        assertFalse(configService.getPropertyNames().contains(foreign));
        configService.set(foreign, "x", false);
        assertTrue(configService.getPropertyNames().contains(foreign));
        configService.remove(foreign);
        assertFalse(configService.getPropertyNames().contains(foreign));
    }

    @Test
    public void getHolderReflectsLaterChanges() {
        Holder<TestConfig> holder = configService.getHolder(TestConfig.class);
        assertEquals("test", holder.get().testProperty());
        configService.set(PROP, "viaGetHolder", false);
        assertEquals("viaGetHolder", holder.get().testProperty());
    }

    @Test
    public void getHolderWithNullPrefixThrows() {
        //noinspection DataFlowIssue
        assertThrows(NullPointerException.class, () -> configService.getHolder(TestConfig.class, null));
    }

    @Test
    @SuppressWarnings("DataFlowIssue")
    public void setNullValueThrows() {
        assertThrows(NullPointerException.class, () -> configService.set(PROP, null, false));
    }

    @Test
    public void forkInheritsPropertiesThatHaveBeenSet() {
        configService.set(PROP, "beforeFork", false);
        ConfigService fork = configService.fork();
        assertEquals("beforeFork", fork.getConfig().getValue(PROP, String.class));
        assertEquals("beforeFork", fork.getHolder(TestConfig.class).get().testProperty());
    }

    @Test
    public void setOnForkDoesNotAffectTheParent() {
        ConfigService fork = configService.fork();
        assertTrue(fork.set(PROP, "onlyInFork", false));
        assertEquals("onlyInFork", fork.getConfig().getValue(PROP, String.class));
        assertEquals("test", configService.getConfig().getValue(PROP, String.class));
        assertEquals("test", testConfig.get().testProperty());
    }

    @Test
    public void setOnParentAfterForkDoesNotAffectTheFork() {
        ConfigService fork = configService.fork();
        configService.set(PROP, "onlyInParent", false);
        assertEquals("test", fork.getConfig().getValue(PROP, String.class));
        assertEquals("test", fork.getHolder(TestConfig.class).get().testProperty());
    }

    @Test
    public void removeOnForkFallsBackToTheBaseConfigNotTheParent() {
        configService.set(PROP, "parentValue", false);
        ConfigService fork = configService.fork();
        assertTrue(fork.remove(PROP));
        assertEquals("test", fork.getConfig().getValue(PROP, String.class));
        assertEquals("parentValue", configService.getConfig().getValue(PROP, String.class));
    }

    @Test
    public void repeatedSetsDoNotAccumulateConfigSources() {
        configService.set(PROP, "first", false);
        int sources = countSources(configService.getConfig());
        for (int i = 0; i < 10; i++) {
            configService.set(PROP, "value" + i, false);
        }

        assertEquals(sources, countSources(configService.getConfig()));
    }

    @Test
    public void forkDoesNotAccumulateConfigSources() {
        int sources = countSources(configService.getConfig());
        ConfigService fork = configService.fork().fork().fork();
        assertEquals(sources, countSources(fork.getConfig()));
    }

    private static int countSources(Config config) {
        int count = 0;
        for (var ignored : config.getConfigSources()) {
            count++;
        }

        return count;
    }

    @Test
    public void testGetPropertyNames() {
        assertTrue(configService.getPropertyNames().contains(PROP));
        assertTrue(configService.getPropertyNames().contains(INT));
    }

    @Test
    public void bindCachesTheSupplierResultUntilTheConfigChanges() {
        AtomicInteger calls = new AtomicInteger();
        Supplier<Object> mapping = () -> {
            calls.incrementAndGet();
            return new Object();
        };

        Object first = configService.bind("bindCachesTheSupplierResultUntilTheConfigChanges", mapping);
        Object second = configService.bind("bindCachesTheSupplierResultUntilTheConfigChanges", mapping);

        assertSame(first, second);
        assertEquals(1, calls.get(), "the second bind() must reuse the cached value");
    }

    @Test
    public void bindInvalidatesItsCacheWhenTheConfigChanges() {
        AtomicInteger calls = new AtomicInteger();
        Supplier<Object> mapping = () -> {
            calls.incrementAndGet();
            return new Object();
        };

        Object first = configService.bind("bindInvalidatesItsCacheWhenTheConfigChanges", mapping);
        configService.set(PROP, "invalidatesTheBindCache", false);
        Object second = configService.bind("bindInvalidatesItsCacheWhenTheConfigChanges", mapping);

        assertNotSame(first, second);
        assertEquals(2, calls.get());
    }

    @Test
    public void bindReturnsTheUnwrappedValueForNonObjectTypes() {
        Path expected = Path.of("some", "path");
        Path result = configService.bind("bindInvalidatesItsCacheWhenTheConfigChanges", () -> expected);

        assertSame(expected, result);
    }

    @Test
    @SuppressWarnings("DataFlowIssue")
    public void bindSupportsANullSupplierResult() {
        assertNull(configService.<String>bind("bindSupportsANullSupplierResult", () -> null));
    }

}
