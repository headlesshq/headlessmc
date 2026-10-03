package io.github.headlesshq.headlessmc.patcher.asm;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.exceptions.HeadlessMcIOException;
import io.github.headlesshq.headlessmc.patcher.Classpath;
import io.github.headlesshq.headlessmc.patcher.PatchContext;
import io.github.headlesshq.headlessmc.patcher.PatchException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

public class AggregateSuperClassResolverTest {
    private static SuperClassResolver resolver(Function<String, String> result, Runnable onClose) {
        return new SuperClassResolver() {
            @Override
            public String getCommonSuperClass(String type1, String type2) {
                return result.apply(type1 + "+" + type2);
            }

            @Override
            public void close() {
                onClose.run();
            }
        };
    }

    private static SuperClassStrategy strategy(Function<PatchContext, SuperClassResolver> apply) {
        return new SuperClassStrategy() {
            @Override
            public SuperClassResolver apply(PatchContext context) {
                return apply.apply(context);
            }

            @Override
            public boolean isApplicable() {
                return true;
            }

            @Override
            public int sort() {
                return SORT_NON_CLASS_LOADING;
            }
        };
    }

    private static PatchContext context(Path root) {
        return new FakePatchContext(root, new Classpath(new LinkedHashSet<>()), List.of());
    }

    @Test
    public void usesFirstWorkingStrategyAndCachesResolver(@TempDir Path root) throws Exception {
        AtomicInteger applications = new AtomicInteger();
        SuperClassStrategy failing = strategy(context -> {
            throw new HeadlessMcException("cannot apply");
        });
        SuperClassStrategy working = strategy(context -> {
            applications.incrementAndGet();
            return resolver(key -> "super", () -> {
            });
        });

        AggregateSuperClassResolver aggregate =
            new AggregateSuperClassResolver(List.of(failing, working), context(root));

        assertEquals("super", aggregate.getCommonSuperClass("a", "b"));
        assertEquals("super", aggregate.getCommonSuperClass("c", "d"));
        assertEquals(1, applications.get(), "working strategy should only be applied once");
    }

    @Test
    public void fallsThroughFailingResolvers(@TempDir Path root) throws Exception {
        SuperClassStrategy failingResolver = strategy(context -> resolver(key -> {
            throw new HeadlessMcException("cannot resolve");
        }, () -> {
        }));
        SuperClassStrategy working = strategy(context -> resolver(key -> "resolved", () -> {
        }));

        AggregateSuperClassResolver aggregate =
            new AggregateSuperClassResolver(List.of(failingResolver, working), context(root));

        assertEquals("resolved", aggregate.getCommonSuperClass("a", "b"));
    }

    @Test
    public void throwsWhenNothingResolves(@TempDir Path root) {
        SuperClassStrategy failing = strategy(context -> {
            throw new HeadlessMcException("cannot apply");
        });

        AggregateSuperClassResolver aggregate =
            new AggregateSuperClassResolver(List.of(failing), context(root));

        assertThrows(PatchException.class, () -> aggregate.getCommonSuperClass("a", "b"));
    }

    @Test
    public void closeClosesOpenResolversAndAggregatesFailures(@TempDir Path root) throws Exception {
        AtomicInteger closed = new AtomicInteger();
        SuperClassStrategy failingClose = strategy(context -> resolver(key -> "one", () -> {
            throw new HeadlessMcException("close failed");
        }));
        SuperClassStrategy goodClose = strategy(context -> resolver(key -> {
            throw new HeadlessMcException("cannot resolve");
        }, closed::incrementAndGet));

        AggregateSuperClassResolver aggregate =
            new AggregateSuperClassResolver(List.of(goodClose, failingClose), context(root));
        aggregate.getCommonSuperClass("a", "b");

        HeadlessMcIOException e = assertThrows(HeadlessMcIOException.class, aggregate::close);
        assertEquals(1, e.getSuppressed().length);
        assertEquals(1, closed.get());
    }

    @Test
    public void closeWithoutOpenResolversDoesNothing(@TempDir Path root) {
        AggregateSuperClassResolver aggregate = new AggregateSuperClassResolver(List.of(), context(root));
        assertDoesNotThrow(aggregate::close);
    }

}
