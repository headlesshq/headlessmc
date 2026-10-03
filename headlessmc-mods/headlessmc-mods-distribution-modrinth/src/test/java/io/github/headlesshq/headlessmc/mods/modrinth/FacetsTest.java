package io.github.headlesshq.headlessmc.mods.modrinth;

import io.github.headlesshq.headlessmc.mods.ModDirectory;
import io.github.headlesshq.headlessmc.mods.ModType;
import io.github.headlesshq.headlessmc.version.arg.Side;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class FacetsTest {
    @Test
    void testEmptyFacets() {
        assertEquals("[]", Facets.builder().build().toQuery());
    }

    @Test
    void testEmptyExplicitLists() {
        Facets facets = Facets.builder()
            .modTypes(Set.of())
            .loaders(List.of())
            .versions(List.of())
            .sides(List.of())
            .build();

        assertEquals("[]", facets.toQuery());
    }

    @Test
    void testSingleModType() {
        Facets facets = Facets.builder()
            .modTypes(Set.of(ModType.MOD))
            .build();

        assertEquals("[[\"project_type:mod\"]]", facets.toQuery());
    }

    @Test
    void testMultipleModTypesAreOred() {
        Facets facets = Facets.builder()
            .modTypes(new LinkedHashSet<>(List.of(ModType.MOD, ModType.MOD_PACK)))
            .build();

        assertEquals("[[\"project_type:mod\",\"project_type:modpack\"]]", facets.toQuery());
    }

    @Test
    void testModTypePrefersModrinthSpecificDistributionName() {
        ModType custom = new ModType(
            "custom",
            Map.of(
                ModType.DISTRIBUTION_DEFAULT, "custom_default",
                ModrinthDistributionPlatform.DEFAULT, "custom_modrinth"
            ),
            ModDirectory.of("custom")
        );

        Facets facets = Facets.builder()
            .modTypes(Set.of(custom))
            .build();

        assertEquals("[[\"project_type:custom_modrinth\"]]", facets.toQuery());
    }

    @Test
    void testModTypeFallsBackToDefaultDistributionName() {
        ModType custom = new ModType(
            "custom",
            Map.of(ModType.DISTRIBUTION_DEFAULT, "custom_default"),
            ModDirectory.of("customs")
        );

        Facets facets = Facets.builder()
            .modTypes(Set.of(custom))
            .build();

        assertEquals("[[\"project_type:custom_default\"]]", facets.toQuery());
    }

    @Test
    void testModTypeWithoutDistributionNameIsSkipped() {
        ModType noNames = new ModType("custom", Map.of(), ModDirectory.of("customs"));

        Facets facets = Facets.builder()
            .modTypes(Set.of(noNames, ModType.MOD))
            .build();

        assertEquals("[[\"project_type:mod\"]]", facets.toQuery());
    }

    @Test
    void testModTypesOnlyMissingNamesProducesEmptyQuery() {
        ModType noNames = new ModType("custom", Map.of(), ModDirectory.of("customs"));

        Facets facets = Facets.builder()
            .modTypes(Set.of(noNames))
            .build();

        assertEquals("[]", facets.toQuery());
    }

    @Test
    void testLoadersAreOredAsCategories() {
        Facets facets = Facets.builder()
            .loaders(List.of("fabric", "forge"))
            .build();

        assertEquals("[[\"categories:fabric\",\"categories:forge\"]]", facets.toQuery());
    }

    @Test
    void testSingleLoader() {
        Facets facets = Facets.builder()
            .loaders(List.of("fabric"))
            .build();

        assertEquals("[[\"categories:fabric\"]]", facets.toQuery());
    }

    @Test
    void testVersionsAreOred() {
        Facets facets = Facets.builder()
            .versions(List.of("1.20.1", "1.21"))
            .build();

        assertEquals("[[\"versions:1.20.1\",\"versions:1.21\"]]", facets.toQuery());
    }

    @Test
    void testClientSideOnly() {
        Facets facets = Facets.builder()
            .sides(List.of(Side.CLIENT))
            .build();

        assertEquals("[[\"client_side:required\"]]", facets.toQuery());
    }

    @Test
    void testServerSideOnly() {
        Facets facets = Facets.builder()
            .sides(List.of(Side.SERVER))
            .build();

        assertEquals("[[\"server_side:required\"]]", facets.toQuery());
    }

    @Test
    void testBothSidesAreSeparateAndGroupsNotOred() {
        Facets facets = Facets.builder()
            .sides(List.of(Side.CLIENT, Side.SERVER))
            .build();

        assertEquals("[[\"client_side:required\"],[\"server_side:required\"]]", facets.toQuery());
    }

    @Test
    void testSideOrderIsPreserved() {
        Facets facets = Facets.builder()
            .sides(List.of(Side.SERVER, Side.CLIENT))
            .build();

        assertEquals("[[\"server_side:required\"],[\"client_side:required\"]]", facets.toQuery());
    }

    @Test
    void testAllFieldsCombinedAreAndedTogether() {
        Facets facets = Facets.builder()
            .modTypes(Set.of(ModType.MOD))
            .loaders(List.of("fabric"))
            .versions(List.of("1.20.1"))
            .sides(List.of(Side.CLIENT, Side.SERVER))
            .build();

        assertEquals(
            "[[\"project_type:mod\"],[\"categories:fabric\"],[\"versions:1.20.1\"],"
                + "[\"client_side:required\"],[\"server_side:required\"]]",
            facets.toQuery()
        );
    }

    @Test
    void testResultHasNoTrailingCommaAndBalancedBrackets() {
        Facets facets = Facets.builder()
            .modTypes(Set.of(ModType.MOD, ModType.PLUGIN))
            .loaders(List.of("fabric"))
            .sides(List.of(Side.CLIENT))
            .build();

        String query = facets.toQuery();
        assertFalse(query.contains(",]"), "must not contain a trailing comma before a closing bracket");
        assertEquals('[', query.charAt(0));
        assertEquals(']', query.charAt(query.length() - 1));
        assertEquals(
            query.chars().filter(c -> c == '[').count(),
            query.chars().filter(c -> c == ']').count()
        );
    }

}
