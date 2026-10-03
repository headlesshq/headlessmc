package io.github.headlesshq.headlessmc.progressbar;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcIOException;
import io.quarkus.test.component.QuarkusComponentTest;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.literal.NamedLiteral;
import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusComponentTest({ProgressBarServiceManagerImpl.class, DummyProgressBarService.class})
class ProgressBarServiceManagerImplTest {
    /** A service that either produces a bar or fails. */
    private static final class TestService implements ProgressbarService {
        private final String name;
        private final boolean failing;

        TestService(String name, boolean failing) {
            this.name = name;
            this.failing = failing;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public ProgressBar displayProgressBar(ProgressBar.Configuration configuration) {
            if (failing) {
                throw new HeadlessMcIOException("no terminal");
            }

            return bar;
        }

        @Override
        public boolean isDummy() {
            return false;
        }

        private final ProgressBar bar = new ProgressBar() {
            @Override
            public boolean isDummy() {
                return false;
            }

            @Override
            public void stepBy(long n) {
            }

            @Override
            public void stepTo(long n) {
            }

            @Override
            public void step() {
            }

            @Override
            public void maxHint(long n) {
            }

            @Override
            public void close() {
            }
        };
    }

    /** Provides a working and a broken service next to the module's own {@link DummyProgressBarService}. */
    static class Services {
        @Produces
        @Singleton
        @Named("real")
        ProgressbarService real() {
            return new TestService("real", false);
        }

        @Produces
        @Singleton
        @Named("failing")
        ProgressbarService failing() {
            return new TestService("failing", true);
        }
    }

    private static final ProgressBar.Configuration CONFIGURATION =
        new ProgressBar.Configuration("task", 100L, ProgressBar.Configuration.Unit.MB);

    @Inject
    ProgressBarServiceManagerImpl manager;

    @Inject
    @Any
    Instance<ProgressbarService> services;

    @Inject
    @Dummy
    ProgressbarService dummy;

    @Inject
    @Named("real")
    ProgressbarService real;

    /** The manager over the subset of services matching {@code qualifiers}. */
    private ProgressBarServiceManagerImpl manager(java.lang.annotation.Annotation... qualifiers) {
        return new ProgressBarServiceManagerImpl(services.select(qualifiers), dummy);
    }

    @Test
    void usesTheFirstWorkingNonDummyService() {
        assertSame(((TestService) real).bar, manager.displayProgressBar(CONFIGURATION));
    }

    @Test
    void withoutARealServiceTheDummyIsUsed() {
        ProgressBar bar = manager(new AnnotationLiteral<Dummy>() {
        }).displayProgressBar(CONFIGURATION);

        assertTrue(bar.isDummy());
    }

    @Test
    void whenEveryServiceFailsTheDummyIsUsed() {
        assertTrue(manager(NamedLiteral.of("failing")).displayProgressBar(CONFIGURATION).isDummy());
    }

    @Test
    void theDummyProgressBarDoesNothing() {
        ProgressBar bar = ProgressBar.dummy();

        assertTrue(bar.isDummy());
        assertDoesNotThrow(() -> {
            bar.step();
            bar.stepBy(1);
            bar.stepTo(2);
            bar.maxHint(3);
            bar.close();
        });
    }

    @Test
    void configurationsHaveAnOptionalUnit() {
        ProgressBar.Configuration configuration = new ProgressBar.Configuration("task", 5L);

        assertNull(configuration.unit());
        assertEquals("task", configuration.taskName());
        assertEquals(5L, configuration.initialMax());
        assertEquals(1_000_000L, ProgressBar.Configuration.Unit.MB.size());
    }

}
