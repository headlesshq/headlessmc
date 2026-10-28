package io.github.headlesshq.headlessmc.config;

import io.github.headlesshq.headlessmc.console.output.SecretService;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class ConfigSecretSourceTest {
    private static final String TOKEN = "hmc.test.secrets.token";

    @Inject
    ConfigService configService;

    @Inject
    ConfigSecretSource secretSource;

    @AfterEach
    public void tearDown() {
        configService.remove(TOKEN);
    }

    @Test
    public void secretsAreRecognizedByTheirName() {
        assertTrue(secretSource.isSecret("hmc.github.token"));
        assertTrue(secretSource.isSecret("hmc.curseforge.API-KEY"));
        assertTrue(secretSource.isSecret("hmc.test.password"));
        assertTrue(secretSource.isSecret("GH_TOKEN"));
        assertFalse(secretSource.isSecret("hmc.github.cache-minutes"));
        assertFalse(secretSource.isSecret("hmc.tokens"));
    }

    @Test
    public void masksOnlyNonEmptySecretValues() {
        assertEquals(SecretService.MASK, secretSource.mask("hmc.github.token", "ghp_secret"));
        assertEquals("", secretSource.mask("hmc.github.token", ""));
        assertEquals("true", secretSource.mask("hmc.java.download", "true"));
    }

    @Test
    public void suppliesTheValuesOfSecretProperties() {
        assertFalse(secretSource.getSecrets().contains("ghp_first"));

        configService.set(TOKEN, "ghp_first", false);
        assertTrue(secretSource.getSecrets().contains("ghp_first"));
        assertFalse(secretSource.getSecrets().contains("test"), "non-secret values must not be supplied");

        configService.set(TOKEN, "ghp_second", false);
        assertTrue(secretSource.getSecrets().contains("ghp_second"));
        assertFalse(secretSource.getSecrets().contains("ghp_first"));
    }

}
