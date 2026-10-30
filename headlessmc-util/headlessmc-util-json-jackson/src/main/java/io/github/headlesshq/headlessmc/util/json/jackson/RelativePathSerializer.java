package io.github.headlesshq.headlessmc.util.json.jackson;

import io.github.headlesshq.headlessmc.util.json.RelativePath;
import io.github.headlesshq.headlessmc.util.json.RelativePathService;
import lombok.RequiredArgsConstructor;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

import java.nio.file.Path;

/**
 * Serializes {@link Path}s as {@link RelativePath} objects.
 */
@RequiredArgsConstructor
public class RelativePathSerializer extends ValueSerializer<Path> {
    public static final String SOURCE = "source";
    public static final String SOURCE_PATH = "sourcePath";
    public static final String PATH = "path";

    private final RelativePathService relativePathService;

    @Override
    public void serialize(Path value, JsonGenerator gen, SerializationContext ctxt) throws JacksonException {
        RelativePath relativePath = relativePathService.relativize(value);
        gen.writeStartObject();
        if (relativePath.source() != null) {
            gen.writeStringProperty(SOURCE, relativePath.source());
        }

        if (relativePath.sourcePath() != null) {
            gen.writeStringProperty(SOURCE_PATH, relativePath.sourcePath());
        }

        gen.writeStringProperty(PATH, relativePath.path());
        gen.writeEndObject();
    }

    @Override
    public Class<?> handledType() {
        return Path.class;
    }

}
