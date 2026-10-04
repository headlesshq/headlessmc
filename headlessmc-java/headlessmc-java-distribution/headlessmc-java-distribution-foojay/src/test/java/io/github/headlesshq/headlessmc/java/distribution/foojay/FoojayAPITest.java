package io.github.headlesshq.headlessmc.java.distribution.foojay;

import io.github.headlesshq.headlessmc.distribution.JavaDistribution;
import io.github.headlesshq.headlessmc.net.rest.ApiException;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.ProcessingException;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class FoojayAPITest {
    @Inject
    @RestClient
    FoojayAPI foojayAPI;

    @Test
    @Disabled
    void testGetDistributions() throws ApiException, ProcessingException {
        List<Distribution> result = foojayAPI.getDistributions();
        assertTrue(result.stream().anyMatch(distribution -> "Temurin".equals(distribution.name())));
    }

    @Test
    @Disabled
    void testDefaultDistribution() throws ApiException, ProcessingException {
        Distribution result = foojayAPI.getDistribution(JavaDistribution.DEFAULT);
        assertEquals("Temurin", result.name());
        assertEquals("temurin", result.api_parameter());
    }

}
