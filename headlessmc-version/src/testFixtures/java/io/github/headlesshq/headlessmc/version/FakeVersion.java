package io.github.headlesshq.headlessmc.version;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A configurable {@link Version} for tests.
 */
public class FakeVersion implements Version {
    private final String id;
    private final List<Library> libraries = new ArrayList<>();
    private @Nullable String inheritsFrom;
    private @Nullable String type;
    private @Nullable String mainClass;
    private @Nullable String minecraftArguments;
    private @Nullable Map<String, List<Argument>> arguments;
    private @Nullable AssetIndex assetIndex;
    private @Nullable Map<String, LoggingConfiguration> logging;
    private @Nullable Map<String, Download> downloads;
    private @Nullable JavaVersion javaVersion;

    public FakeVersion(String id) {
        this.id = id;
    }

    public FakeVersion withInheritsFrom(@Nullable String inheritsFrom) {
        this.inheritsFrom = inheritsFrom;
        return this;
    }

    public FakeVersion withType(@Nullable String type) {
        this.type = type;
        return this;
    }

    public FakeVersion withMainClass(@Nullable String mainClass) {
        this.mainClass = mainClass;
        return this;
    }

    public FakeVersion withMinecraftArguments(@Nullable String minecraftArguments) {
        this.minecraftArguments = minecraftArguments;
        return this;
    }

    public FakeVersion withArguments(@Nullable Map<String, List<Argument>> arguments) {
        this.arguments = arguments;
        return this;
    }

    public FakeVersion withAssetIndex(@Nullable AssetIndex assetIndex) {
        this.assetIndex = assetIndex;
        return this;
    }

    public FakeVersion withLogging(@Nullable Map<String, LoggingConfiguration> logging) {
        this.logging = logging;
        return this;
    }

    public FakeVersion withDownload(String key, Download download) {
        if (downloads == null) {
            downloads = new LinkedHashMap<>();
        }

        downloads.put(key, download);
        return this;
    }

    public FakeVersion withJavaVersion(int majorVersion) {
        this.javaVersion = new JavaVersion() {
            @Override
            public String getComponent() {
                return "java-runtime";
            }

            @Override
            public Integer getMajorVersion() {
                return majorVersion;
            }
        };
        return this;
    }

    public FakeVersion withLibrary(Library library) {
        libraries.add(library);
        return this;
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
