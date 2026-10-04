package io.github.headlesshq.headlessmc.os.apache;

import io.github.headlesshq.headlessmc.os.OS;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusTest
public class ApacheOSProviderTest {
    @Inject
    OS os;

    @Test
    public void testApacheOSProvider() {
        assertNotNull(os);
    }

}
