package io.github.headlesshq.headlessmc.launcher.libraries;

import io.github.headlesshq.headlessmc.files.McFiles;
import io.github.headlesshq.headlessmc.version.Version;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

final class TestLibraries {
    private TestLibraries() {
    }

    static McFiles mcFiles(Path root) {
        return new McFiles() {
            @Override
            public Path getMcDir() {
                return root.resolve("mc");
            }

            @Override
            public Path getVersionsDir() {
                return getMcDir().resolve("versions");
            }

            @Override
            public Path getLibraryDir() {
                return getMcDir().resolve("libraries");
            }

            @Override
            public Path getAssetsDir() {
                return getMcDir().resolve("assets");
            }

            @Override
            public Path getResourcesDir() {
                return getMcDir().resolve("resources");
            }
        };
    }

    static Version.Download download(@Nullable String path, @Nullable Long size) {
        return download(path, size, null, null);
    }

    static Version.Download download(
        @Nullable String path,
        @Nullable Long size,
        @Nullable String url,
        @Nullable String sha1
    ) {
        return new Version.Download() {
            @Override
            public @Nullable String getId() {
                return null;
            }

            @Override
            public @Nullable String getSha1() {
                return sha1;
            }

            @Override
            public @Nullable Long getSize() {
                return size;
            }

            @Override
            public @Nullable String getUrl() {
                return url;
            }

            @Override
            public @Nullable String getPath() {
                return path;
            }
        };
    }

    static Version.LibraryDownloads downloads(
        Version.@Nullable Download artifact,
        @Nullable Map<String, Version.Download> classifiers
    ) {
        return new Version.LibraryDownloads() {
            @Override
            public Version.@Nullable Download getArtifact() {
                return artifact;
            }

            @Override
            public @Nullable Map<String, Version.Download> getClassifiers() {
                return classifiers;
            }
        };
    }

    static Version.Rule rule(String action, @Nullable String osName) {
        return new Version.Rule() {
            @Override
            public String getAction() {
                return action;
            }

            @Override
            public @Nullable OS getOs() {
                if (osName == null) {
                    return null;
                }

                return new OS() {
                    @Override
                    public @Nullable String getName() {
                        return osName;
                    }

                    @Override
                    public @Nullable String getVersion() {
                        return null;
                    }

                    @Override
                    public @Nullable String getArch() {
                        return null;
                    }

                    @Override
                    public @Nullable VersionRange getVersionRange() {
                        return null;
                    }
                };
            }

            @Override
            public @Nullable Map<String, Boolean> getFeatures() {
                return null;
            }
        };
    }

    static final class FakeLibrary implements Version.Library {
        private final String name;
        @Nullable Map<String, String> natives;
        @Nullable List<Version.Rule> rules;
        Version.@Nullable LibraryDownloads downloads;
        Version.@Nullable Extract extract;

        FakeLibrary(String name) {
            this.name = name;
        }

        @Override
        public @Nullable Map<String, String> getNatives() {
            return natives;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Version.@Nullable Extract getExtract() {
            return extract;
        }

        @Override
        public @Nullable List<Version.Rule> getRules() {
            return rules;
        }

        @Override
        public Version.@Nullable LibraryDownloads getDownloads() {
            return downloads;
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
    }

    static final class FakeVersion implements Version {
        private final List<Library> libraries;
        @Nullable Map<String, Download> downloads;

        FakeVersion(List<Library> libraries) {
            this.libraries = libraries;
        }

        @Override
        public String getId() {
            return "test-version";
        }

        @Override
        public List<Library> getLibraries() {
            return libraries;
        }

        @Override
        public @Nullable String getInheritsFrom() {
            return null;
        }

        @Override
        public @Nullable String getType() {
            return null;
        }

        @Override
        public @Nullable String getMainClass() {
            return null;
        }

        @Override
        public @Nullable String getMinecraftArguments() {
            return null;
        }

        @Override
        public @Nullable Map<String, List<Argument>> getArguments() {
            return null;
        }

        @Override
        public @Nullable AssetIndex getAssetIndex() {
            return null;
        }

        @Override
        public @Nullable Map<String, LoggingConfiguration> getLogging() {
            return null;
        }

        @Override
        public @Nullable Map<String, Download> getDownloads() {
            return downloads;
        }

        @Override
        public @Nullable JavaVersion getJavaVersion() {
            return null;
        }
    }

}
