package io.github.headlesshq.headlessmc.console.jline;

import io.github.headlesshq.headlessmc.config.Holder;
import io.github.headlesshq.headlessmc.console.cache.CachedConsole;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class JlineConsoleTest {
    private final Holder<JlineConfig> config = () -> config();

    @Test
    void testProviderReturnsSameConsole() {
        JlineConsoleProvider provider = new JlineConsoleProvider(config);
        assertSame(provider.get(), provider.get());
    }

    @Test
    void testOnlyOneTerminalIsOpenAtATime() throws Exception {
        JlineConsole console = new JlineConsole(config);
        try (JlineConsole.TerminalLease first = console.openTerminal()) {
            try (JlineConsole.TerminalLease second = console.openTerminal()) {
                assertSame(first.state(), second.state());
            }

            assertSame(first.state(), console.currentTerminal(), "terminal closed while a lease is still open");
            first.close();
            assertNull(console.currentTerminal(), "terminal not closed after the last lease");
            // closing a lease twice must not release the terminal of another lease
            try (JlineConsole.TerminalLease third = console.openTerminal()) {
                first.close();
                assertSame(third.state(), console.currentTerminal());
            }

            assertNull(console.currentTerminal());
        }
    }

    @Test
    void testCachedConsoleSharesTerminal() throws Exception {
        JlineConsole console = new JlineConsole(config);
        try (CachedConsole cached = console.cache()) {
            assertSame(console, cached.get());
            try (JlineConsole.TerminalLease lease = console.openTerminal()) {
                assertSame(lease.state(), console.wrap(state -> state));
            }
        }

        assertNull(console.currentTerminal());
    }

    private static JlineConfig config() {
        return (JlineConfig) Proxy.newProxyInstance(
            JlineConfig.class.getClassLoader(),
            new Class<?>[]{JlineConfig.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "enabled" -> true;
                case "graphemeCluster" -> GraphemeClusterMode.OFF;
                case "tooManyCandidates" -> Optional.empty();
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == args[0];
                case "toString" -> "JlineConfig";
                default -> method.getReturnType() == boolean.class ? false : null;
            });
    }

}
