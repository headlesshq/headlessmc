package io.github.headlesshq.headlessmc.patcher.asm;

import io.github.headlesshq.headlessmc.patcher.Classpath;
import io.github.headlesshq.headlessmc.patcher.PatchContext;
import io.github.headlesshq.headlessmc.patcher.PatchException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class EntryClassWriterTest {
    private static SuperClassService service(List<SuperClassStrategy> strategies) {
        return new SuperClassService() {
            @Override
            public List<SuperClassStrategy> getStrategies() {
                return strategies;
            }

            @Override
            public SuperClassResolver resolver(PatchContext context) {
                return new AggregateSuperClassResolver(strategies, context);
            }
        };
    }

    private static PatchContext context(Path root, List<SuperClassStrategy> strategies) {
        return new FakePatchContext(
            root.resolve("base"),
            new Classpath(new LinkedHashSet<>()),
            List.of(service(strategies))
        );
    }

    @Test
    public void resolvesCommonSuperClassViaStrategies(@TempDir Path root) {
        EntryClassWriter writer = new EntryClassWriter(
            context(root, List.of(new ClassLoaderSuperClassStrategy())));

        assertEquals("java/lang/Number", writer.getCommonSuperClass("java/lang/Integer", "java/lang/Long"));
    }

    @Test
    public void skipsInapplicableStrategies(@TempDir Path root) {
        SuperClassStrategy inapplicable = new SuperClassStrategy() {
            @Override
            public SuperClassResolver apply(PatchContext context) {
                throw new AssertionError("must not be applied");
            }

            @Override
            public boolean isApplicable() {
                return false;
            }

            @Override
            public int sort() {
                return SORT_NON_CLASS_LOADING;
            }
        };
        EntryClassWriter writer = new EntryClassWriter(
            context(root, List.of(inapplicable, new ClassLoaderSuperClassStrategy())));

        assertEquals("java/lang/Number", writer.getCommonSuperClass("java/lang/Integer", "java/lang/Long"));
    }

    @Test
    public void throwsWithoutWorkingStrategy(@TempDir Path root) {
        EntryClassWriter writer = new EntryClassWriter(context(root, List.of()));

        assertThrows(PatchException.class,
            () -> writer.getCommonSuperClass("java/lang/Integer", "java/lang/Long"));
    }

}
