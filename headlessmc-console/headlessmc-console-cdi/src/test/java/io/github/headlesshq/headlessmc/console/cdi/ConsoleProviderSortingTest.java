package io.github.headlesshq.headlessmc.console.cdi;

import io.github.headlesshq.headlessmc.console.ConsoleProvider;
import io.github.headlesshq.headlessmc.console.SimpleConsoleProvider;
import io.github.headlesshq.headlessmc.console.cache.CachedConsoleProvider;
import io.github.headlesshq.headlessmc.console.jline.JlineConsoleProvider;
import io.quarkus.arc.ClientProxy;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@QuarkusTest
public class ConsoleProviderSortingTest {
    @Any
    @Inject
    Instance<ConsoleProvider> providers;

    @Test
    public void testProviderSorting() {
        List<ConsoleProvider> names = providers.stream().sorted().map(ClientProxy::unwrap).toList();
        assertEquals(3, names.size(), () -> "unexpected providers: " + names);
        assertInstanceOf(CachedConsoleProvider.class, names.get(0), () -> "a cached console must be preferred, but was: " + names);
        assertInstanceOf(JlineConsoleProvider.class, names.get(1), () -> "jline must be preferred over the default, but was: " + names);
        assertInstanceOf(SimpleConsoleProvider.class, names.get(2), () -> "default must come last, but was: " + names);
    }

}
