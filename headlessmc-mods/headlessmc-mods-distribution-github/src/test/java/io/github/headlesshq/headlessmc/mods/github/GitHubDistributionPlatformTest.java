package io.github.headlesshq.headlessmc.mods.github;

import io.github.headlesshq.headlessmc.mods.distribution.ModDistributionPlatform;
import io.github.headlesshq.headlessmc.mods.distribution.ModDistributionPlatformService;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Checks that the platform wires up in a real container.
 * What it does once wired is covered by {@link GitHubPlatformTest}.
 */
@QuarkusTest
public class GitHubDistributionPlatformTest {
    @Inject
    @GitHub
    GitHubDistributionPlatform gitHubDistributionPlatform;

    @Inject
    ModDistributionPlatformService modDistributionPlatformService;

    @Test
    public void testGitHubDistributionPlatform() {
        assertEquals(GitHub.PLATFORM_NAME, gitHubDistributionPlatform.getName());
        assertTrue(gitHubDistributionPlatform.findRepository(HmcSpecificsRepository.NAME).isPresent());
        assertTrue(gitHubDistributionPlatform.findRepository(HmcOptimizationsRepository.NAME).isPresent());
        assertTrue(gitHubDistributionPlatform.findRepository(McRuntimeTestRepository.NAME).isPresent());
        assertTrue(gitHubDistributionPlatform.findRepository(HmcMcpRepository.NAME).isPresent());
        assertTrue(gitHubDistributionPlatform.findRepository(HmcPrometheusRepository.NAME).isPresent());
    }

    @Test
    public void testPlatformCanBeFoundByName() {
        assertInstanceOf(
            GitHubDistributionPlatform.class,
            modDistributionPlatformService.byName(GitHub.PLATFORM_NAME).orElseThrow()
        );
        assertTrue(modDistributionPlatformService.byName(ModDistributionPlatform.DEFAULT).isEmpty());
    }

}
