package io.github.headlesshq.headlessmc.version;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ProcessedVersionTest {
    @Test
    void testEmptyHierarchy() {
        assertThrows(IllegalArgumentException.class, () -> new ProcessedVersion(List.of()));
    }

    @Test
    void idAndInheritsFromComeFromLastVersion() {
        FakeVersion parent = new FakeVersion("1.12.2");
        FakeVersion child = new FakeVersion("forge-1.12.2");
        child.inheritsFrom = "1.12.2";

        ProcessedVersion version = new ProcessedVersion(List.of(parent, child));

        assertEquals("forge-1.12.2", version.getId());
        assertEquals("1.12.2", version.getInheritsFrom());
    }

    @Test
    void childOverridesScalarValues() {
        FakeVersion parent = new FakeVersion("parent");
        parent.type = "release";
        parent.mainClass = "parent.Main";
        parent.minecraftArguments = "--parent";
        FakeVersion child = new FakeVersion("child");
        child.mainClass = "child.Main";

        ProcessedVersion version = new ProcessedVersion(List.of(parent, child));

        assertEquals("child.Main", version.getMainClass());
        assertEquals("release", version.getType(), "missing values fall back to the parent");
        assertEquals("--parent", version.getMinecraftArguments());
    }

    @Test
    void missingMainClassThrows() {
        ProcessedVersion version = new ProcessedVersion(List.of(new FakeVersion("v")));
        assertThrows(NullPointerException.class, version::getMainClass);
    }

    @Test
    void argumentsAreMergedPerKey() {
        FakeVersion parent = new FakeVersion("parent");
        Version.Argument parentGame = argument("--parentGame");
        parent.arguments = Map.of(Version.GAME_ARGUMENTS, List.of(parentGame));
        FakeVersion child = new FakeVersion("child");
        Version.Argument childGame = argument("--childGame");
        Version.Argument childJvm = argument("-Dchild");
        child.arguments = Map.of(
            Version.GAME_ARGUMENTS, List.of(childGame),
            Version.JVM_ARGUMENTS, List.of(childJvm)
        );

        Map<String, List<Version.Argument>> arguments =
            new ProcessedVersion(List.of(parent, child)).getArguments();

        assertNotNull(arguments);
        assertEquals(List.of(parentGame, childGame), arguments.get(Version.GAME_ARGUMENTS));
        assertEquals(List.of(childJvm), arguments.get(Version.JVM_ARGUMENTS));
    }

    @Test
    void argumentsAreNullWhenNoVersionHasAny() {
        ProcessedVersion version = new ProcessedVersion(List.of(new FakeVersion("v")));
        assertNull(version.getArguments());
    }

    @Test
    void mapsAreMergedWithChildWinning() {
        FakeVersion parent = new FakeVersion("parent");
        Version.Download parentClient = download("parent-client");
        Version.Download mappings = download("mappings");
        parent.downloads = Map.of(
            Version.DOWNLOAD_CLIENT, parentClient,
            Version.DOWNLOAD_CLIENT_MAPPINGS, mappings
        );
        FakeVersion child = new FakeVersion("child");
        Version.Download childClient = download("child-client");
        child.downloads = Map.of(Version.DOWNLOAD_CLIENT, childClient);

        Map<String, Version.Download> downloads = new ProcessedVersion(List.of(parent, child)).getDownloads();

        assertEquals(childClient, downloads.get(Version.DOWNLOAD_CLIENT));
        assertEquals(mappings, downloads.get(Version.DOWNLOAD_CLIENT_MAPPINGS));
    }

    @Test
    void javaVersionAndAssetIndexFallBackToParent() {
        FakeVersion parent = new FakeVersion("parent");
        parent.javaVersion = javaVersion(17);
        parent.assetIndex = assetIndex("assets-1");
        FakeVersion child = new FakeVersion("child");

        ProcessedVersion version = new ProcessedVersion(List.of(parent, child));

        assertEquals(17, version.getJavaVersion().getMajorVersion());
        assertEquals(17, version.requireJavaVersion());
        assertEquals("assets-1", version.getAssetIndex().getId());
    }

    @Test
    void missingJavaVersionAndAssetIndexThrow() {
        ProcessedVersion version = new ProcessedVersion(List.of(new FakeVersion("v")));
        assertThrows(NullPointerException.class, version::getJavaVersion);
        assertThrows(NullPointerException.class, version::getAssetIndex);
    }

    @Test
    void librariesAreOverriddenByVersionAgnosticId() {
        FakeVersion parent = new FakeVersion("parent");
        Version.Library oldLwjgl = library("org.lwjgl:lwjgl:2.9.0");
        Version.Library parentOnly = library("com.example:parent-only:1.0");
        parent.libraries = List.of(oldLwjgl, parentOnly);
        FakeVersion child = new FakeVersion("child");
        Version.Library newLwjgl = library("org.lwjgl:lwjgl:3.3.0");
        Version.Library classified = library("org.lwjgl:lwjgl:3.3.0:natives-linux");
        child.libraries = List.of(newLwjgl, classified);

        List<Version.Library> libraries = new ProcessedVersion(List.of(parent, child)).getLibraries();

        assertEquals(List.of(newLwjgl, parentOnly, classified), libraries);
    }

    @Test
    void libraryHashAlgorithmsSkipMissingHashes() {
        Version.Library library = library("com.example:lib:1.0");
        assertEquals(Map.of("SHA-1", "sha1-value"), library.getHashAlgorithms());
    }

    @Test
    void libraryArtifactIsParsedFromName() {
        Version.Library library = library("com.example:lib:1.0");
        assertEquals("com.example", library.getArtifact().group());
        assertEquals("lib", library.getArtifact().name());
        assertEquals("1.0", library.getArtifact().version());
    }

    private static Version.Argument argument(String... values) {
        return new Version.Argument() {
            @Override
            public @Nullable List<Version.Rule> getRules() {
                return null;
            }

            @Override
            public List<String> value() {
                return List.of(values);
            }
        };
    }

    private static Version.Download download(String id) {
        return new Version.Download() {
            @Override
            public String getId() {
                return id;
            }

            @Override
            public @Nullable String getSha1() {
                return null;
            }

            @Override
            public @Nullable Long getSize() {
                return null;
            }

            @Override
            public @Nullable String getUrl() {
                return null;
            }

            @Override
            public @Nullable String getPath() {
                return null;
            }
        };
    }

    private static Version.AssetIndex assetIndex(String id) {
        return new Version.AssetIndex() {
            @Override
            public String getId() {
                return id;
            }

            @Override
            public @Nullable String getSha1() {
                return null;
            }

            @Override
            public @Nullable Long getSize() {
                return null;
            }

            @Override
            public @Nullable String getUrl() {
                return null;
            }

            @Override
            public @Nullable String getPath() {
                return null;
            }

            @Override
            public @Nullable Long getTotalSize() {
                return null;
            }
        };
    }

    private static Version.JavaVersion javaVersion(int major) {
        return new Version.JavaVersion() {
            @Override
            public String getComponent() {
                return "java-runtime";
            }

            @Override
            public Integer getMajorVersion() {
                return major;
            }
        };
    }

    private static Version.Library library(String name) {
        return new Version.Library() {
            @Override
            public @Nullable Map<String, String> getNatives() {
                return null;
            }

            @Override
            public String getName() {
                return name;
            }

            @Override
            public Version.@Nullable Extract getExtract() {
                return null;
            }

            @Override
            public @Nullable List<Version.Rule> getRules() {
                return null;
            }

            @Override
            public Version.@Nullable LibraryDownloads getDownloads() {
                return null;
            }

            @Override
            public @Nullable String getMd5() {
                return null;
            }

            @Override
            public @Nullable String getSha256() {
                return null;
            }

            @Override
            public @Nullable String getSha512() {
                return null;
            }

            @Override
            public @Nullable String getId() {
                return null;
            }

            @Override
            public @Nullable String getSha1() {
                return "sha1-value";
            }

            @Override
            public @Nullable Long getSize() {
                return null;
            }

            @Override
            public @Nullable String getUrl() {
                return null;
            }

            @Override
            public @Nullable String getPath() {
                return null;
            }
        };
    }

    private static final class FakeVersion implements Version {
        private final String id;
        private @Nullable String inheritsFrom;
        private @Nullable String type;
        private @Nullable String mainClass;
        private @Nullable String minecraftArguments;
        private @Nullable Map<String, List<Argument>> arguments;
        private @Nullable AssetIndex assetIndex;
        private @Nullable Map<String, LoggingConfiguration> logging;
        private @Nullable Map<String, Download> downloads;
        private @Nullable JavaVersion javaVersion;
        private List<Library> libraries = List.of();

        private FakeVersion(String id) {
            this.id = id;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public List<Library> getLibraries() {
            return libraries;
        }

        @Override
        public @Nullable String getInheritsFrom() {
            return inheritsFrom;
        }

        @Override
        public @Nullable String getType() {
            return type;
        }

        @Override
        public @Nullable String getMainClass() {
            return mainClass;
        }

        @Override
        public @Nullable String getMinecraftArguments() {
            return minecraftArguments;
        }

        @Override
        public @Nullable Map<String, List<Argument>> getArguments() {
            return arguments;
        }

        @Override
        public @Nullable AssetIndex getAssetIndex() {
            return assetIndex;
        }

        @Override
        public @Nullable Map<String, LoggingConfiguration> getLogging() {
            return logging;
        }

        @Override
        public @Nullable Map<String, Download> getDownloads() {
            return downloads;
        }

        @Override
        public @Nullable JavaVersion getJavaVersion() {
            return javaVersion;
        }

    }

}
