package io.github.headlesshq.headlessmc.platform;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Reads the version lists that the platforms bundle as resources, e.g. {@code forge/forge-versions.json},
 * so that tests can match against real version data without network access.
 */
public final class BundledVersions {
    public static final String VANILLA = "vanilla/vanilla-versions.json";

    private static final JsonMapper MAPPER = JsonMapper.builder()
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .build();

    private BundledVersions() {
    }

    /**
     * @param resource a bundled cache file, which wraps its content as {@code {"version": 0, "object": ...}}.
     * @return the content of the cache file.
     */
    public static JsonNode cacheObject(String resource) {
        return Objects.requireNonNull(read(resource).get("object"), "No object in " + resource);
    }

    /**
     * @param resource a bundled cache file, which wraps its content as {@code {"version": 0, "object": ...}}.
     * @param type     the type of the content.
     * @return the content of the cache file.
     */
    public static <T> T cacheObject(String resource, Class<T> type) {
        return MAPPER.treeToValue(cacheObject(resource), type);
    }

    /**
     * @return a {@link VanillaVersionService} with all versions of the bundled {@value #VANILLA}.
     */
    public static FakeVanillaVersionService vanillaVersionService() {
        List<String> names = new ArrayList<>();
        for (JsonNode version : read(VANILLA).get("versions")) {
            names.add(version.get("id").asString());
        }

        return new FakeVanillaVersionService(names.toArray(String[]::new));
    }

    public static JsonNode read(String resource) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        try (InputStream is = Objects.requireNonNull(classLoader.getResourceAsStream(resource), resource)) {
            return MAPPER.readTree(is);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
