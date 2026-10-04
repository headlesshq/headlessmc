package io.github.headlesshq.headlessmc.java.version;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@QuarkusTest
public class JavaScannerServiceTest {
    @Inject
    @Any
    Instance<JavaScanner> scanners;

    @Test
    public void testScannerOrder() {
        List<JavaScanner> list = scanners.stream().toList();
        assertEquals(2, list.size());
        assertInstanceOf(SettingsProcessScanner.class, list.getFirst());
        assertInstanceOf(SimpleProcessScanner.class, list.get(1));
    }

}
