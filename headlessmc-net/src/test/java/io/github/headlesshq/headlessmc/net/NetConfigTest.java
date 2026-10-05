package io.github.headlesshq.headlessmc.net;

import io.github.headlesshq.headlessmc.HeadlessMc;
import io.github.headlesshq.headlessmc.config.ConfigService;
import io.github.headlesshq.headlessmc.config.Holder;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class NetConfigTest {
    @Inject
    ConfigService configService;

    @Inject
    Holder<NetConfig> config;

    @Test
    public void testDefaults() {
        NetConfig defaultConfig = config.get();
        assertTrue(defaultConfig.httpVersion().isEmpty());
        assertTrue(defaultConfig.cookies());
        assertEquals(HeadlessMc.DEFAULT_USER_AGENT, defaultConfig.userAgent());
        assertTrue(defaultConfig.deleteFailedFiles());
        assertEquals(1, defaultConfig.retries());
    }

    @Test
    public void testConfiguredValues() {
        ConfigService fork = configService.fork();
        fork.set("hmc.net.http-version", "HTTP_1_1", false);
        fork.set("hmc.net.cookies", "false", false);
        fork.set("hmc.net.user-agent", "test", false);

        NetConfig configured = fork.getHolder(NetConfig.class).get();
        assertNotEquals(config.get(), configured);
        assertTrue(configured.httpVersion().isPresent());
        assertEquals(HttpVersion.HTTP_1_1, configured.httpVersion().get());
        assertFalse(configured.cookies());
        assertEquals("test", configured.userAgent());
    }

}
