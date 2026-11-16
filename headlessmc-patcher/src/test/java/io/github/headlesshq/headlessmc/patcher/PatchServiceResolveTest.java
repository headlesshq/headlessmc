package io.github.headlesshq.headlessmc.patcher;

import io.github.headlesshq.headlessmc.files.McFiles;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link PatchService#resolve(java.util.Collection)} turns the names of the selected patchers into the patchers
 * to apply, and leaves out the ones that are {@link Patcher#excludes() excluded} by another selected patcher.
 */
class PatchServiceResolveTest {
    private record TestPatcher(String name, Set<String> excludes) implements Patcher {
        @Override
        public void patch(PatchContext context) {
        }

        @Override
        public long version() {
            return 1L;
        }
    }

    private static final class Service implements PatchService {
        private final List<Patcher> patchers = new ArrayList<>();

        private Service with(String name, String... excludes) {
            patchers.add(new TestPatcher(name, Set.of(excludes)));
            return this;
        }

        @Override
        public PatchResult patch(McFiles mcFiles, PatchResult patchResult, int javaVersion, List<Patcher> patchers) {
            return patchResult;
        }

        @Override
        public List<Patcher> getPatchers() {
            return patchers;
        }

        @Override
        public Optional<Patcher> getPatcher(String name) {
            return patchers.stream().filter(patcher -> patcher.name().equalsIgnoreCase(name)).findFirst();
        }
    }

    private final Service service = new Service()
        .with("log4j")
        .with("lwjgl")
        .with("lwjgl-pre", "LWJGL")
        .with("paulscode");

    private List<String> resolve(String... names) {
        return service.resolve(List.of(names)).stream().map(Patcher::name).toList();
    }

    @Test
    void patchersDoNotExcludeAnythingByDefault() {
        assertEquals(Set.of(), TestPatchers.patcher("a", 1L).excludes());
    }

    @Test
    void patchersAreResolvedInTheOrderTheyAreSelectedIn() {
        assertEquals(List.of("paulscode", "log4j", "lwjgl"), resolve("paulscode", "log4j", "lwjgl"));
        assertEquals(List.of(), resolve());
    }

    @Test
    void namesAreNotCaseSensitiveAndDuplicatesAreIgnored() {
        assertEquals(List.of("log4j", "lwjgl"), resolve("LOG4J", "lwjgl", "log4j", "Lwjgl"));
    }

    @Test
    void anExcludedPatcherIsNotApplied() {
        assertEquals(List.of("log4j", "paulscode", "lwjgl-pre"), resolve("log4j", "lwjgl", "paulscode", "lwjgl-pre"));
    }

    @Test
    void itDoesNotMatterWhereTheExcludingPatcherIsSelected() {
        // e.g. selected by a parent profile, before the launch selects the patcher it replaces
        assertEquals(List.of("lwjgl-pre", "log4j", "paulscode"), resolve("lwjgl-pre", "log4j", "lwjgl", "paulscode"));
    }

    @Test
    void anExclusionOnlyMattersIfTheExcludedPatcherIsSelected() {
        assertEquals(List.of("log4j", "lwjgl-pre"), resolve("log4j", "lwjgl-pre"));
        assertEquals(List.of("log4j", "lwjgl"), resolve("log4j", "lwjgl"));
    }

    @Test
    void aPatcherExcludingItselfIsStillApplied() {
        service.with("selfish", "selfish");

        assertEquals(List.of("selfish", "log4j"), resolve("selfish", "log4j"));
    }

    @Test
    void anExclusionOfAPatcherThatDoesNotExistIsIgnored() {
        service.with("careful", "not-installed");

        assertEquals(List.of("careful", "log4j"), resolve("careful", "log4j"));
    }

    @Test
    void patchersExcludingEachOtherCannotBeSelectedTogether() {
        service.with("a", "b").with("b", "a");

        PatchException e = assertThrows(PatchException.class, () -> resolve("a", "b"));
        assertTrue(e.getMessage().contains("exclude each other"), e.getMessage());
        assertEquals(List.of("a"), resolve("a"));
        assertEquals(List.of("b"), resolve("b"));
    }

    @Test
    void anUnknownPatcherFails() {
        PatchException e = assertThrows(PatchException.class, () -> resolve("log4j", "missing"));
        assertTrue(e.getMessage().contains("missing"), e.getMessage());
    }

}
