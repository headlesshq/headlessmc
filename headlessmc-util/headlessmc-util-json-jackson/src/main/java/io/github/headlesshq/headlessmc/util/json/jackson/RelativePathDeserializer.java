package io.github.headlesshq.headlessmc.util.json.jackson;

import io.github.headlesshq.headlessmc.util.json.JsonParseException;
import io.github.headlesshq.headlessmc.util.json.RelativePath;
import io.github.headlesshq.headlessmc.util.json.RelativePathService;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ValueDeserializer;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Path;

/**
 * Deserializes {@link Path}s written by the {@link RelativePathSerializer}.
 * Plain strings, as written by Jackson's default serializer, are parsed as before.
 */
@RequiredArgsConstructor
public class RelativePathDeserializer extends ValueDeserializer<Path> {
    private final RelativePathService relativePathService;

    @Override
    public Path deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
        if (p.currentToken() == JsonToken.VALUE_STRING) {
            return parseLegacy(p.getString(), ctxt);
        }

        if (p.currentToken() != JsonToken.START_OBJECT) {
            return (Path) ctxt.handleUnexpectedToken(Path.class, p);
        }

        JsonNode node = ctxt.readTree(p);
        JsonNode path = node.get(RelativePathSerializer.PATH);
        if (path == null || !path.isString()) {
            return (Path) ctxt.reportInputMismatch(this, "Path object without '%s': %s", RelativePathSerializer.PATH, node);
        }

        RelativePath relativePath = new RelativePath(
            text(node, RelativePathSerializer.SOURCE),
            text(node, RelativePathSerializer.SOURCE_PATH),
            path.asString()
        );

        try {
            return relativePathService.resolve(relativePath);
        } catch (JsonParseException e) {
            return (Path) ctxt.handleInstantiationProblem(Path.class, relativePath, e);
        }
    }

    @Override
    public Class<?> handledType() {
        return Path.class;
    }

    private static @Nullable String text(JsonNode node, String name) {
        JsonNode value = node.get(name);
        return value == null || value.isNull() ? null : value.asString();
    }

    /**
     * Parses paths the way Jackson's default deserializer does,
     * either as a plain path or as a {@code file:} URI.
     */
    private Path parseLegacy(String value, DeserializationContext ctxt) {
        int colon = value.indexOf(':');
        if (colon < 0 || colon == 1 && Character.isLetter(value.charAt(0))) {
            return Path.of(value);
        }

        try {
            return Path.of(new URI(value));
        } catch (URISyntaxException | IllegalArgumentException | FileSystemNotFoundException e) {
            return (Path) ctxt.handleInstantiationProblem(Path.class, value, e);
        }
    }

}
