package io.github.headlesshq.headlessmc.platform.client;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.platform.FakePlatform;
import io.github.headlesshq.headlessmc.platform.FakePlatformService;
import io.github.headlesshq.headlessmc.platform.FakeVanillaPlatform;
import io.github.headlesshq.headlessmc.platform.FakeVanillaVersionService;
import io.github.headlesshq.headlessmc.platform.FakeVersionService;
import io.github.headlesshq.headlessmc.platform.Platform;
import io.github.headlesshq.headlessmc.platform.PlatformService;
import io.github.headlesshq.headlessmc.platform.VersionID;
import io.github.headlesshq.headlessmc.version.FakeVersion;
import io.github.headlesshq.headlessmc.version.Version;
import io.github.headlesshq.headlessmc.version.VersionProcessor;
import io.github.headlesshq.headlessmc.version.arg.VersionArg;
import io.github.headlesshq.headlessmc.util.json.jackson.DefaultJacksonJsonService;
import io.github.headlesshq.headlessmc.version.service.FakeVersionJsonService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class VersionMatcherServiceImplTest {
    /** A matcher that always fails, to exercise the suppressed-exception paths. */
    private static final class FailingMatcher implements VersionMatcher {
        @Override
        public VersionID match(PlatformService platformService, Version version, VersionProcessor processor) {
            throw new VersionMatchException("nope");
        }

        @Override
        public boolean canMatch(PlatformService platformService, Version version, VersionProcessor processor) {
            throw new VersionMatchException("cannot match");
        }

        @Override
        public String getPlatformName() {
            return "broken";
        }
    }

    /** Counts calls to {@link #match}, to check if the cache has been used. */
    private static final class CountingMatcher implements VersionMatcher {
        private final VersionMatcher delegate;
        private int matches;

        private CountingMatcher(VersionMatcher delegate) {
            this.delegate = delegate;
        }

        @Override
        public VersionID match(PlatformService platformService, Version version, VersionProcessor processor) {
            matches++;
            return delegate.match(platformService, version, processor);
        }

        @Override
        public boolean canMatch(PlatformService platformService, Version version, VersionProcessor processor) {
            return delegate.canMatch(platformService, version, processor);
        }

        @Override
        public String getPlatformName() {
            return delegate.getPlatformName();
        }
    }

    private FakeVanillaVersionService vanillaVersions;
    private FakeVersionService fabricVersions;
    private FakeVanillaPlatform vanilla;
    private FakePlatform fabric;
    private FakePlatformService platformService;
    private VersionMatcherServiceImpl service;
    private FakeVersionJsonService versions;

    @BeforeEach
    void setup() {
        vanillaVersions = new FakeVanillaVersionService("1.21.1", "1.20.4");
        vanilla = new FakeVanillaPlatform("1.21.1", "1.20.4");
        vanilla.withClientSupport(new ClientSupport(
            new DefaultVersionMatcher(vanillaVersions, vanillaVersions) {
                @Override
                public boolean canMatch(PlatformService service, Version version, VersionProcessor processor) {
                    return true;
                }
            },
            (id, mcDir, args) -> new FakeVersion(id.getVersion().getName())
        ));

        fabricVersions = new FakeVersionService("fabric")
            .withBuilds("1.21.1", "0.16.9")
            .withBuilds("1.20.4", "0.15.0");
        fabric = new FakePlatform("fabric", fabricVersions);
        fabric.withClientSupport(new ClientSupport(
            new DefaultVersionMatcher(vanillaVersions, fabricVersions),
            (id, mcDir, args) -> new FakeVersion(id.getVersion().getName())
        ));

        platformService = new FakePlatformService(vanilla, fabric);
        service = new VersionMatcherServiceImpl(platformService);
        versions = new FakeVersionJsonService();
    }

    @Test
    void matchesVanillaVersion() {
        Set<VersionID> ids = service.match(new FakeVersion("1.21.1"), versions);

        assertEquals(1, ids.size());
        assertEquals("vanilla", ids.iterator().next().getPlatform().getName());
    }

    @Test
    void matchesModdedVersionOnBothPlatforms() {
        Set<VersionID> ids = service.match(new FakeVersion("fabric-loader-0.16.9-1.21.1"), versions);

        assertEquals(
            Set.of("vanilla", "fabric"),
            ids.stream().map(id -> id.getPlatform().getName()).collect(java.util.stream.Collectors.toSet())
        );
    }

    @Test
    void unmatchableVersionThrows() {
        VersionMatchException e = assertThrows(
            VersionMatchException.class, () -> service.match(new FakeVersion("something-else"), versions)
        );
        assertTrue(e.getMessage().contains("Failed to parse version"));
    }

    @Test
    void failingCanMatchIsSuppressedAndReported() {
        FakePlatform broken = new FakePlatform("broken", new FakeVersionService("broken"));
        broken.withClientSupport(new ClientSupport(new FailingMatcher(), (id, dir, args) -> new FakeVersion("x")));
        VersionMatcherServiceImpl brokenService = new VersionMatcherServiceImpl(
            new FakePlatformService(new FakeVanillaPlatform("1.21.1"), broken)
        );

        HeadlessMcException e = assertThrows(
            HeadlessMcException.class, () -> brokenService.match(new FakeVersion("1.21.1"), versions)
        );
        assertEquals(1, e.getSuppressed().length);
    }

    @Test
    void platformWithoutClientSupportIsSkipped() {
        FakePlatform noSupport = new FakePlatform("nosupport", new FakeVersionService("nosupport"));
        VersionMatcherServiceImpl withoutSupport = new VersionMatcherServiceImpl(
            new FakePlatformService(vanilla, noSupport)
        );

        assertEquals(1, withoutSupport.match(new FakeVersion("1.21.1"), versions).size());
    }

    @Test
    void reverseMatchFindsExactVersionId() {
        VersionID id = VersionID.resolve(platformService, VersionArg.parse("fabric", "1.21.1", "0.16.9"));
        Version version = new FakeVersion("fabric-loader-0.16.9-1.21.1");

        Optional<VersionMatcherService.MatchResult> result = service.match(id, List.of(version), versions);

        assertTrue(result.isPresent());
        assertEquals(id, result.get().resolvedId());
        assertEquals(version, result.get().version());
    }

    @Test
    void reverseMatchIgnoresTheSide() {
        VersionID id = VersionID.resolve(platformService, VersionArg.parse("client", "fabric", "1.21.1", "0.16.9"));
        Version version = new FakeVersion("fabric-loader-0.16.9-1.21.1");

        Optional<VersionMatcherService.MatchResult> result = service.match(id, List.of(version), versions);

        assertTrue(result.isPresent());
        assertEquals(id, result.get().resolvedId());
        assertEquals(version, result.get().version());
    }

    @Test
    void reverseMatchRequiresTheSameBuild() {
        VersionID id = VersionID.resolve(platformService, VersionArg.parse("fabric", "1.20.4", "0.15.0"));
        Version version = new FakeVersion("fabric-loader-0.16.9-1.21.1");

        assertEquals(Optional.empty(), service.match(id, List.of(version), versions));
    }

    @Test
    void reverseMatchWithAnyBuildResolvesTheBuild() {
        VersionID id = VersionID.resolve(platformService, VersionArg.parse("fabric", "1.21.1"));
        Version version = new FakeVersion("fabric-loader-0.16.9-1.21.1");

        Optional<VersionMatcherService.MatchResult> result = service.match(id, List.of(version), versions);

        assertTrue(result.isPresent());
        assertEquals("0.16.9", result.get().resolvedId().getBuild().orElseThrow().getName());
    }

    @Test
    void reverseMatchWithAnyBuildIgnoresOtherPlatformVersions() {
        VersionID id = VersionID.resolve(platformService, VersionArg.parse("fabric", "1.20.4"));
        Version version = new FakeVersion("fabric-loader-0.16.9-1.21.1");

        assertEquals(Optional.empty(), service.match(id, List.of(version), versions));
    }

    @Test
    void reverseMatchSkipsUnmatchableVersions() {
        VersionID id = VersionID.resolve(platformService, VersionArg.parse("fabric", "1.21.1", "0.16.9"));
        Version version = new FakeVersion("fabric-loader-0.16.9-1.21.1");

        Optional<VersionMatcherService.MatchResult> result =
            service.match(id, List.of(new FakeVersion("something-else"), version), versions);

        assertTrue(result.isPresent());
        assertEquals(version, result.get().version());
    }

    @Test
    void reverseMatchWithAnyBuildSkipsUnmatchableVersions() {
        VersionID id = VersionID.resolve(platformService, VersionArg.parse("fabric", "1.21.1"));
        Version version = new FakeVersion("fabric-loader-0.16.9-1.21.1");

        Optional<VersionMatcherService.MatchResult> result =
            service.match(id, List.of(new FakeVersion("something-else"), version), versions);

        assertTrue(result.isPresent());
        assertEquals(version, result.get().version());
    }

    @Test
    void reverseMatchWithOnlyUnmatchableVersionsIsEmpty() {
        VersionID id = VersionID.resolve(platformService, VersionArg.parse("fabric", "1.21.1"));
        assertEquals(Optional.empty(), service.match(id, List.of(new FakeVersion("something-else")), versions));
    }

    @Test
    void reverseMatchWithoutCandidatesIsEmpty() {
        VersionID id = VersionID.resolve(platformService, VersionArg.parse("fabric", "1.21.1", "0.16.9"));
        assertEquals(Optional.empty(), service.match(id, List.of(), versions));
    }

    @Test
    void includesPrefersTheContainingPlatform() {
        Platform forge = new FakePlatform("forge", new FakeVersionService("forge"));
        VersionMatcher neoforge = new DefaultVersionMatcher(vanillaVersions, new FakeVersionService("neoforge"));

        assertTrue(neoforge.includes(platformService, new FakeVersion("neoforge-1.21.1"), forge));
        assertFalse(neoforge.includes(platformService, new FakeVersion("forge-1.21.1"), forge));
    }

    private CountingMatcher countFabricMatches() {
        CountingMatcher matcher = new CountingMatcher(new DefaultVersionMatcher(vanillaVersions, fabricVersions));
        fabric.withClientSupport(new ClientSupport(matcher, (id, mcDir, args) -> new FakeVersion("x")));
        return matcher;
    }

    @Test
    void matchesAreCached() {
        CountingMatcher matcher = countFabricMatches();
        Version version = new FakeVersion("fabric-loader-0.16.9-1.21.1").withInheritsFrom("1.21.1");

        Set<VersionID> first = service.match(version, versions);
        Set<VersionID> second = service.match(version, versions);

        assertEquals(first, second);
        assertEquals(1, matcher.matches);
    }

    @Test
    void changedInheritsFromInvalidatesTheCache() {
        CountingMatcher matcher = countFabricMatches();
        service.match(new FakeVersion("fabric-loader-0.16.9-1.21.1").withInheritsFrom("1.21.1"), versions);
        service.match(new FakeVersion("fabric-loader-0.16.9-1.21.1").withInheritsFrom("1.20.4"), versions);

        assertEquals(2, matcher.matches);
    }

    @Test
    void failedMatchesAreNotCached() {
        assertThrows(VersionMatchException.class, () -> service.match(new FakeVersion("something-else"), versions));
        assertThrows(VersionMatchException.class, () -> service.match(new FakeVersion("something-else"), versions));
    }

    // the cache file

    /**
     * A cache file as HeadlessMc wrote it before it knew more than one {@code .minecraft} directory: one match
     * for every id. The match of the Fabric version only has the Fabric id, a fresh match in these tests also
     * finds vanilla, so a result with one id has been read from this file.
     */
    private static final String LEGACY_CACHE = """
        {
          "version" : 1,
          "object" : {
            "matches" : {
              "fabric-loader-0.16.9-1.21.1" : {
                "inheritsFrom" : "1.21.1",
                "ids" : [ {
                  "side" : null,
                  "platform" : "fabric",
                  "version" : "1.21.1",
                  "build" : "0.16.9"
                } ]
              },
              "1.20.4" : {
                "inheritsFrom" : null,
                "ids" : [ {
                  "side" : null,
                  "platform" : "vanilla",
                  "version" : "1.20.4",
                  "build" : null
                } ]
              }
            }
          }
        }
        """;

    private static final String FABRIC = "fabric-loader-0.16.9-1.21.1";

    @TempDir
    Path temp;

    private Path cacheFile() {
        return temp.resolve("cache").resolve(VersionMatcherServiceImpl.CACHE_FILE);
    }

    private Path primary() {
        return temp.resolve("primary");
    }

    private Path secondary() {
        return temp.resolve("secondary");
    }

    /** A service as it is after a restart: it only knows what is in the cache file. */
    private VersionMatcherServiceImpl restarted() {
        return new VersionMatcherServiceImpl(
            platformService,
            VersionMatcherServiceImpl.fileCache(new DefaultJacksonJsonService(), cacheFile()),
            file -> file.startsWith(primary())
        );
    }

    private void writeCache(String content) {
        try {
            Files.createDirectories(cacheFile().getParent());
            Files.writeString(cacheFile(), content);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private String readCache() {
        try {
            return Files.readString(cacheFile());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Path versionJson(Path mcDir, String id) {
        return mcDir.resolve("versions").resolve(id).resolve(id + ".json");
    }

    /** A version with a version.json that exists in the given {@code .minecraft} directory. */
    private static FakeVersion installed(Path mcDir, String id) {
        Path file = versionJson(mcDir, id);
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, "{}");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        return new FakeVersion(id).withFile(file);
    }

    private static FakeVersion fabric(Path mcDir) {
        return installed(mcDir, FABRIC).withInheritsFrom("1.21.1");
    }

    private static Set<String> platforms(Set<VersionID> ids) {
        return ids.stream().map(id -> id.getPlatform().getName()).collect(java.util.stream.Collectors.toSet());
    }

    /**
     * Makes Fabric match every version whose file is in a directory named like an instance of another launcher,
     * whatever its id is: such launchers name their instances freely, e.g. {@code 1.21.1} for a Fabric instance.
     */
    private CountingMatcher matchInstancesAsFabric() {
        CountingMatcher matcher = new CountingMatcher(new VersionMatcher() {
            @Override
            public VersionID match(PlatformService platformService, Version version, VersionProcessor processor) {
                return VersionID.resolve(platformService, VersionArg.parse("fabric", "1.21.1", "0.16.9"));
            }

            @Override
            public boolean canMatch(PlatformService platformService, Version version, VersionProcessor processor) {
                return version.asFile().filter(file -> file.toString().contains("instances")).isPresent();
            }

            @Override
            public String getPlatformName() {
                return "fabric";
            }
        });

        fabric.withClientSupport(new ClientSupport(matcher, (id, mcDir, args) -> new FakeVersion("x")));
        return matcher;
    }

    @Test
    void matchesAreRememberedInTheCacheFile() {
        Version version = new FakeVersion(FABRIC).withInheritsFrom("1.21.1");
        Set<VersionID> expected = restarted().match(version, versions);
        assertTrue(Files.exists(cacheFile()));

        CountingMatcher matcher = countFabricMatches();
        Set<VersionID> fromFile = restarted().match(version, versions);

        assertEquals(expected, fromFile);
        assertEquals(0, matcher.matches);
    }

    @Test
    void theCacheFileIsWrittenInTheCurrentFormat() {
        Version version = fabric(primary());

        restarted().match(version, versions);

        String cache = readCache().replaceAll("\\s", "");
        assertTrue(cache.contains("\"version\":" + VersionMatcherServiceImpl.CACHE_VERSION), cache);
        assertEquals(2, VersionMatcherServiceImpl.CACHE_VERSION);
        // json escapes the separators of a Windows path
        String file = version.asFile().orElseThrow().toString().replace("\\", "\\\\");
        assertTrue(cache.contains("\"file\":\"" + file + "\""), cache);
    }

    // one id, several files

    @Test
    void theSameIdInTwoDirectoriesIsMatchedForEachFile() {
        matchInstancesAsFabric();
        // vanilla 1.21.1, and an instance of another launcher that is also named 1.21.1, but comes with Fabric
        Version vanillaVersion = installed(primary(), "1.21.1");
        Version instance = installed(temp.resolve("instances"), "1.21.1");
        VersionMatcherServiceImpl matcherService = restarted();

        assertEquals(Set.of("vanilla"), platforms(matcherService.match(vanillaVersion, versions)));
        assertEquals(Set.of("vanilla", "fabric"), platforms(matcherService.match(instance, versions)));
        // and again, now that both are cached
        assertEquals(Set.of("vanilla"), platforms(matcherService.match(vanillaVersion, versions)));
        assertEquals(Set.of("vanilla", "fabric"), platforms(matcherService.match(instance, versions)));
    }

    @Test
    void theOrderInWhichFilesWithTheSameIdAreMatchedDoesNotMatter() {
        matchInstancesAsFabric();
        Version vanillaVersion = installed(primary(), "1.21.1");
        Version instance = installed(temp.resolve("instances"), "1.21.1");
        VersionMatcherServiceImpl matcherService = restarted();

        assertEquals(Set.of("vanilla", "fabric"), platforms(matcherService.match(instance, versions)));
        assertEquals(Set.of("vanilla"), platforms(matcherService.match(vanillaVersion, versions)));
        assertEquals(Set.of("vanilla", "fabric"), platforms(matcherService.match(instance, versions)));
    }

    @Test
    void everyFileOfAnIdIsRememberedInTheCacheFile() {
        CountingMatcher matcher = matchInstancesAsFabric();
        Version vanillaVersion = installed(primary(), "1.21.1");
        Version instance = installed(temp.resolve("instances"), "1.21.1");
        VersionMatcherServiceImpl first = restarted();
        first.match(vanillaVersion, versions);
        first.match(instance, versions);
        assertEquals(1, matcher.matches);

        VersionMatcherServiceImpl second = restarted();

        assertEquals(Set.of("vanilla", "fabric"), platforms(second.match(instance, versions)));
        assertEquals(Set.of("vanilla"), platforms(second.match(vanillaVersion, versions)));
        assertEquals(1, matcher.matches);
    }

    @Test
    void everyFileIsMatchedOnce() {
        CountingMatcher matcher = countFabricMatches();
        Version first = fabric(primary());
        Version second = fabric(secondary());
        VersionMatcherServiceImpl matcherService = restarted();

        for (int i = 0; i < 3; i++) {
            matcherService.match(first, versions);
            matcherService.match(second, versions);
        }

        assertEquals(2, matcher.matches);
    }

    @Test
    void lookingVersionsUpDoesNotRewriteTheCacheFile() {
        Version first = fabric(primary());
        Version second = fabric(secondary());
        VersionMatcherServiceImpl matcherService = restarted();
        matcherService.match(first, versions);
        matcherService.match(second, versions);
        String cache = readCache();
        writeCache("the file is not written again");

        for (int i = 0; i < 3; i++) {
            matcherService.match(first, versions);
            matcherService.match(second, versions);
        }

        assertEquals("the file is not written again", readCache());
        assertTrue(cache.contains(first.asFile().orElseThrow().getParent().getFileName().toString()), cache);
    }

    @Test
    void aChangedInheritsFromOnlyInvalidatesTheMatchOfThatFile() {
        CountingMatcher matcher = countFabricMatches();
        VersionMatcherServiceImpl matcherService = restarted();
        matcherService.match(fabric(primary()), versions);
        matcherService.match(fabric(secondary()), versions);
        assertEquals(2, matcher.matches);

        // the version in the primary directory has been replaced
        matcherService.match(fabric(primary()).withInheritsFrom("1.20.4"), versions);
        assertEquals(3, matcher.matches);

        matcherService.match(fabric(secondary()), versions);
        matcherService.match(fabric(primary()).withInheritsFrom("1.20.4"), versions);
        assertEquals(3, matcher.matches);
    }

    @Test
    void aVersionWithoutAFileDoesNotShareTheMatchOfAFile() {
        CountingMatcher matcher = countFabricMatches();
        VersionMatcherServiceImpl matcherService = restarted();

        matcherService.match(fabric(primary()), versions);
        matcherService.match(new FakeVersion(FABRIC).withInheritsFrom("1.21.1"), versions);
        assertEquals(2, matcher.matches);

        matcherService.match(fabric(primary()), versions);
        matcherService.match(new FakeVersion(FABRIC).withInheritsFrom("1.21.1"), versions);
        assertEquals(2, matcher.matches);
    }

    @Test
    void theSameFileIsRecognizedHoweverItsPathIsWritten() {
        CountingMatcher matcher = countFabricMatches();
        VersionMatcherServiceImpl matcherService = restarted();
        FakeVersion version = fabric(primary());
        Path file = version.asFile().orElseThrow();
        Path indirect = file.getParent().resolve("..").resolve(file.getParent().getFileName()).resolve(file.getFileName());

        matcherService.match(version, versions);
        matcherService.match(new FakeVersion(FABRIC).withFile(indirect).withInheritsFrom("1.21.1"), versions);

        assertEquals(1, matcher.matches);
    }

    @Test
    void cachedMatchesOfAPlatformThatIsGoneAreMatchedAgain() {
        restarted().match(fabric(primary()), versions);
        // e.g. the plugin providing Fabric has been removed
        platformService = new FakePlatformService(vanilla);

        Set<VersionID> ids = restarted().match(fabric(primary()), versions);

        assertEquals(Set.of("vanilla"), platforms(ids));
        assertEquals(Set.of("vanilla"), platforms(restarted().match(fabric(primary()), versions)));
    }

    // the cache file of older versions

    @Test
    void aCacheFileOfTheOldFormatIsStillRead() {
        writeCache(LEGACY_CACHE);
        CountingMatcher matcher = countFabricMatches();

        Set<VersionID> ids = restarted().match(fabric(primary()), versions);

        // not matched again, this is what the old file says
        assertEquals(0, matcher.matches);
        assertEquals(Set.of("fabric"), platforms(ids));
        assertEquals("0.16.9", ids.iterator().next().getBuild().orElseThrow().getName());
    }

    @Test
    void everyMatchOfAnOldCacheFileIsRead() {
        writeCache(LEGACY_CACHE);
        VersionMatcherServiceImpl matcherService = restarted();
        // fails if these were matched: nothing can be matched anymore
        vanilla.withClientSupport(new ClientSupport(new FailingMatcher(), (id, dir, args) -> new FakeVersion("x")));
        fabric.withClientSupport(new ClientSupport(new FailingMatcher(), (id, dir, args) -> new FakeVersion("x")));

        assertEquals(Set.of("fabric"), platforms(matcherService.match(fabric(primary()), versions)));
        assertEquals(Set.of("vanilla"), platforms(matcherService.match(installed(primary(), "1.20.4"), versions)));
    }

    @Test
    void anOldMatchIsOnlyUsedForTheDirectoryItHasBeenMadeFor() {
        writeCache(LEGACY_CACHE);
        CountingMatcher matcher = countFabricMatches();
        VersionMatcherServiceImpl matcherService = restarted();

        // the old cache only knew the primary directory: a file somewhere else with the same id is another version
        Set<VersionID> elsewhere = matcherService.match(fabric(secondary()), versions);
        assertEquals(1, matcher.matches);
        assertEquals(Set.of("vanilla", "fabric"), platforms(elsewhere));

        // the version in the primary directory still gets the old match, whichever is asked for first
        assertEquals(Set.of("fabric"), platforms(matcherService.match(fabric(primary()), versions)));
        assertEquals(Set.of("vanilla", "fabric"), platforms(matcherService.match(fabric(secondary()), versions)));
        assertEquals(1, matcher.matches);
    }

    @Test
    void anOldMatchIsNotUsedForAnInstanceWithTheSameId() {
        writeCache(LEGACY_CACHE);
        matchInstancesAsFabric();
        // the old cache says 1.20.4 is vanilla, which was true for the only 1.20.4 there was
        Version instance = installed(temp.resolve("instances"), "1.20.4");
        VersionMatcherServiceImpl matcherService = restarted();

        assertEquals(Set.of("vanilla", "fabric"), platforms(matcherService.match(instance, versions)));
        assertEquals(Set.of("vanilla"), platforms(matcherService.match(installed(primary(), "1.20.4"), versions)));
        assertEquals(Set.of("vanilla", "fabric"), platforms(restarted().match(instance, versions)));
        assertEquals(Set.of("vanilla"), platforms(restarted().match(installed(primary(), "1.20.4"), versions)));
    }

    @Test
    void anOldMatchBecomesTheMatchOfTheFileThatUsesIt() {
        writeCache(LEGACY_CACHE);
        CountingMatcher matcher = countFabricMatches();
        Version version = fabric(primary());

        restarted().match(version, versions);

        // the file has been rewritten in the new format, with the match bound to the file ...
        String cache = readCache();
        assertTrue(cache.replaceAll("\\s", "").contains("\"version\":2"), cache);
        assertTrue(cache.contains(FABRIC + ".json"), cache);
        // ... and without losing the matches nobody asked for yet
        assertTrue(cache.contains("\"1.20.4\""), cache);

        // after a restart it is read in the new format, and is still what the old file said
        VersionMatcherServiceImpl afterRestart = restarted();
        assertEquals(Set.of("fabric"), platforms(afterRestart.match(version, versions)));
        assertEquals(Set.of("vanilla"), platforms(afterRestart.match(installed(primary(), "1.20.4"), versions)));
        assertEquals(0, matcher.matches);
        // the match belongs to that file now, nothing is left over for a version without a file
        afterRestart.match(new FakeVersion(FABRIC).withInheritsFrom("1.21.1"), versions);
        assertEquals(1, matcher.matches);
    }

    @Test
    void anOldMatchIsUsedForAVersionWithoutAFile() {
        writeCache(LEGACY_CACHE);
        CountingMatcher matcher = countFabricMatches();

        Set<VersionID> ids = restarted().match(new FakeVersion(FABRIC).withInheritsFrom("1.21.1"), versions);

        assertEquals(0, matcher.matches);
        assertEquals(Set.of("fabric"), platforms(ids));
    }

    @Test
    void anOldMatchForAnotherParentIsMatchedAgainAndReplaced() {
        writeCache(LEGACY_CACHE);
        CountingMatcher matcher = countFabricMatches();
        Version replaced = fabric(primary()).withInheritsFrom("1.20.4");

        assertEquals(Set.of("vanilla", "fabric"), platforms(restarted().match(replaced, versions)));
        assertEquals(1, matcher.matches);

        // the old match is gone, also for the version it would have fit
        VersionMatcherServiceImpl afterRestart = restarted();
        afterRestart.match(replaced, versions);
        assertEquals(1, matcher.matches);
        afterRestart.match(new FakeVersion(FABRIC).withInheritsFrom("1.21.1"), versions);
        assertEquals(2, matcher.matches);
    }

    @Test
    void anOldCacheFileWithBrokenEntriesIsReadAsFarAsPossible() {
        writeCache("""
            {
              "version" : 1,
              "object" : {
                "matches" : {
                  "nothing" : null,
                  "no-ids" : { "inheritsFrom" : null, "ids" : null },
                  "no-fields" : { },
                  "1.20.4" : {
                    "inheritsFrom" : null,
                    "ids" : [ { "side" : null, "platform" : "vanilla", "version" : "1.20.4", "build" : null } ]
                  }
                }
              }
            }
            """);
        VersionMatcherServiceImpl matcherService = restarted();

        assertEquals(Set.of("vanilla"), platforms(matcherService.match(installed(primary(), "1.20.4"), versions)));
        // an entry without ids is no match, the version is matched like any other
        assertThrows(VersionMatchException.class, () -> matcherService.match(installed(primary(), "no-ids"), versions));
        assertThrows(VersionMatchException.class, () -> matcherService.match(installed(primary(), "nothing"), versions));
        assertEquals(Set.of("vanilla"), platforms(restarted().match(installed(primary(), "1.20.4"), versions)));
    }

    @Test
    void anOldCacheFileWithoutMatchesIsFine() {
        writeCache("{ \"version\" : 1, \"object\" : { \"matches\" : null } }");
        assertEquals(Set.of("vanilla"), platforms(restarted().match(installed(primary(), "1.21.1"), versions)));

        writeCache("{ \"version\" : 1, \"object\" : { } }");
        assertEquals(Set.of("vanilla"), platforms(restarted().match(installed(primary(), "1.21.1"), versions)));

        writeCache("{ \"version\" : 1 }");
        assertEquals(Set.of("vanilla"), platforms(restarted().match(installed(primary(), "1.21.1"), versions)));
    }

    // files that are gone, and other things a cache file can contain

    private void delete(Version version) {
        try {
            Files.delete(version.asFile().orElseThrow());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Test
    void matchesOfFilesThatDoNotExistAnymoreDoNotDisturb() {
        VersionMatcherServiceImpl first = restarted();
        Version gone = fabric(secondary());
        first.match(gone, versions);
        first.match(installed(secondary(), "1.20.4"), versions);
        // e.g. the disk of the secondary directory is not mounted, or the directory has been deleted
        delete(gone);
        delete(installed(secondary(), "1.20.4"));

        CountingMatcher matcher = countFabricMatches();
        VersionMatcherServiceImpl second = restarted();

        assertEquals(Set.of("vanilla", "fabric"), platforms(second.match(fabric(primary()), versions)));
        assertEquals(Set.of("vanilla"), platforms(second.match(installed(primary(), "1.21.1"), versions)));
        assertEquals(1, matcher.matches);
        assertEquals(Set.of("vanilla", "fabric"), platforms(restarted().match(fabric(primary()), versions)));
    }

    @Test
    void matchesOfFilesThatDoNotExistAnymoreAreForgottenWhenTheirIdIsMatchedAgain() {
        VersionMatcherServiceImpl first = restarted();
        Version gone = fabric(secondary());
        Version other = installed(secondary(), "1.20.4");
        first.match(gone, versions);
        first.match(other, versions);
        delete(gone);
        delete(other);
        assertTrue(readCache().contains("secondary"));

        restarted().match(fabric(primary()), versions);

        String cache = readCache();
        // the match of the file that is gone has been replaced by the one of the file that exists ...
        assertTrue(cache.contains("primary"), cache);
        assertEquals(1, cache.split(FABRIC + ".json", -1).length - 1, cache);
        // ... the cache is not searched for other files that are gone, they do no harm
        assertTrue(cache.contains("1.20.4.json"), cache);
    }

    @Test
    void aFileThatIsGoneAndComesBackIsJustMatchedAgain() {
        CountingMatcher matcher = countFabricMatches();
        Version comesBack = fabric(secondary());
        restarted().match(comesBack, versions);
        delete(comesBack);
        // forgets the match of the file that is gone
        restarted().match(fabric(primary()), versions);
        assertEquals(2, matcher.matches);

        // e.g. the disk has been mounted again
        assertEquals(Set.of("vanilla", "fabric"), platforms(restarted().match(fabric(secondary()), versions)));
        assertEquals(3, matcher.matches);
        assertEquals(Set.of("vanilla", "fabric"), platforms(restarted().match(fabric(primary()), versions)));
        assertEquals(3, matcher.matches);
    }

    @Test
    void aMatchOfAVersionThatIsGoneIsStillUsedIfThatVersionIsAskedFor() {
        // nothing checks that a version exists when it is looked up, whoever has the version has its file
        CountingMatcher matcher = countFabricMatches();
        Version version = fabric(primary());
        restarted().match(version, versions);
        delete(version);

        assertEquals(Set.of("vanilla", "fabric"), platforms(restarted().match(version, versions)));
        assertEquals(1, matcher.matches);
    }

    @Test
    void pathsThisSystemCannotParseAreForgottenWithoutAnError() {
        // e.g. a cache that has been copied from another system
        writeCache("""
            {
              "version" : 2,
              "object" : {
                "matches" : {
                  "%s" : [ {
                    "file" : "not\\u0000a\\u0000path",
                    "inheritsFrom" : "1.21.1",
                    "ids" : [ { "side" : null, "platform" : "fabric", "version" : "1.21.1", "build" : "0.16.9" } ]
                  }, {
                    "file" : "",
                    "inheritsFrom" : "1.21.1",
                    "ids" : [ { "side" : null, "platform" : "fabric", "version" : "1.21.1", "build" : "0.16.9" } ]
                  } ]
                }
              }
            }
            """.formatted(FABRIC));
        CountingMatcher matcher = countFabricMatches();

        assertEquals(Set.of("vanilla", "fabric"), platforms(restarted().match(fabric(primary()), versions)));
        assertEquals(1, matcher.matches);
        assertFalse(readCache().contains("u0000"), readCache());
        assertEquals(Set.of("vanilla", "fabric"), platforms(restarted().match(fabric(primary()), versions)));
        assertEquals(1, matcher.matches);
    }

    @Test
    void brokenEntriesInTheCacheFileAreIgnored() {
        writeCache("""
            {
              "version" : 2,
              "object" : {
                "matches" : {
                  "nothing" : null,
                  "empty" : [ ],
                  "nulls" : [ null, { "file" : null, "inheritsFrom" : null, "ids" : null } ],
                  "1.20.4" : [ {
                    "file" : null,
                    "inheritsFrom" : null,
                    "ids" : [ { "side" : null, "platform" : "vanilla", "version" : "1.20.4", "build" : null } ]
                  } ]
                }
              }
            }
            """);
        VersionMatcherServiceImpl matcherService = restarted();

        assertEquals(Set.of("vanilla"), platforms(matcherService.match(new FakeVersion("1.20.4"), versions)));
        assertEquals(Set.of("vanilla"), platforms(matcherService.match(installed(primary(), "1.21.1"), versions)));
        assertThrows(VersionMatchException.class, () -> matcherService.match(new FakeVersion("nulls"), versions));
        assertThrows(VersionMatchException.class, () -> matcherService.match(new FakeVersion("empty"), versions));
        assertThrows(VersionMatchException.class, () -> matcherService.match(new FakeVersion("nothing"), versions));
    }

    @Test
    void aCacheFileThatCannotBeReadIsReplaced() {
        for (String content : List.of(
            "this is not json",
            "",
            "[ 1, 2, 3 ]",
            "{ \"version\" : 2, \"object\" : \"nope\" }",
            "{ \"version\" : 2, \"object\" : { \"matches\" : [ ] } }",
            "{ \"version\" : 1, \"object\" : { \"matches\" : \"nope\" } }",
            "{ \"version\" : 2, \"object\" : { \"matches\" : null } }",
            // written by a newer version of HeadlessMc
            "{ \"version\" : 99, \"object\" : { \"matches\" : { \"1.21.1\" : 5 } } }"
        )) {
            writeCache(content);

            assertEquals(
                Set.of("vanilla", "fabric"), platforms(restarted().match(fabric(primary()), versions)), content
            );

            CountingMatcher matcher = countFabricMatches();
            assertEquals(
                Set.of("vanilla", "fabric"), platforms(restarted().match(fabric(primary()), versions)), content
            );
            assertEquals(0, matcher.matches, content);
            setup();
        }
    }

    @Test
    void aCacheFileThatCannotBeWrittenDoesNotBreakMatching() throws IOException {
        // a directory where the cache file should be
        Files.createDirectories(cacheFile());
        VersionMatcherServiceImpl matcherService = restarted();
        CountingMatcher matcher = countFabricMatches();

        assertEquals(Set.of("vanilla", "fabric"), platforms(matcherService.match(fabric(primary()), versions)));
        assertEquals(Set.of("vanilla", "fabric"), platforms(matcherService.match(fabric(primary()), versions)));
        // still remembered in memory
        assertEquals(1, matcher.matches);
    }

    @Test
    void reverseMatchingFindsTheVersionInTheRightDirectory() {
        matchInstancesAsFabric();
        Version vanillaVersion = installed(primary(), "1.21.1");
        Version instance = installed(temp.resolve("instances"), "1.21.1");
        VersionMatcherServiceImpl matcherService = restarted();
        VersionID fabricId = VersionID.resolve(platformService, VersionArg.parse("fabric", "1.21.1", "0.16.9"));

        // whichever comes first, the vanilla version is not mistaken for the Fabric instance with the same id
        for (List<Version> installedVersions : List.of(List.of(vanillaVersion, instance), List.of(instance, vanillaVersion))) {
            Optional<VersionMatcherService.MatchResult> result = matcherService.match(fabricId, installedVersions, versions);
            assertSame(instance, result.orElseThrow().version());
        }
    }

}
