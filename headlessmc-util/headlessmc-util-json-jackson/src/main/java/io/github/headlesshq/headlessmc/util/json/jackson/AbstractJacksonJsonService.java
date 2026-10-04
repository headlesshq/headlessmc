package io.github.headlesshq.headlessmc.util.json.jackson;

import io.github.headlesshq.headlessmc.reflection.ReflectionRegistered;
import io.github.headlesshq.headlessmc.util.json.JsonParseException;
import io.github.headlesshq.headlessmc.util.json.JsonService;
import jakarta.enterprise.util.TypeLiteral;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public abstract class AbstractJacksonJsonService implements JsonService {
    protected abstract ObjectMapper mapper();

    @Override
    public <V extends ReflectionRegistered> V parse(InputStream inputStream, Class<V> type) throws JsonParseException {
        try {
            return mapper().readValue(inputStream, type);
        } catch (JacksonException e) {
            throw new JsonParseException("Failed to parse " + type, e);
        }
    }

    @Override
    public <V extends ReflectionRegistered> V parse(
        InputStream inputStream,
        TypeLiteral<V> type
    ) throws JsonParseException {
        try {
            JavaType javaType = mapper().constructType(type.getType());
            return mapper().readValue(inputStream, javaType);
        } catch (JacksonException e) {
            throw new JsonParseException("Failed to parse " + type, e);
        }
    }

    @Override
    public <V extends ReflectionRegistered> List<V> parseList(
        InputStream inputStream,
        TypeLiteral<List<V>> type
    ) throws JsonParseException {
        try {
            JavaType javaType = mapper().constructType(type.getType());
            return mapper().readValue(inputStream, javaType);
        } catch (JacksonException e) {
            throw new JsonParseException("Failed to parse " + type, e);
        }
    }

    @Override
    public <V extends ReflectionRegistered> V parse(Path file, Class<V> type) throws JsonParseException {
        try {
            return mapper().readValue(file, type);
        } catch (JacksonException e) {
            throw new JsonParseException("Failed to parse " + file, e);
        }
    }

    @Override
    public <V extends ReflectionRegistered> V parse(Path file, TypeLiteral<V> type) throws JsonParseException {
        try {
            JavaType javaType = mapper().constructType(type.getType());
            return mapper().readValue(file, javaType);
        } catch (JacksonException e) {
            throw new JsonParseException("Failed to parse " + file, e);
        }
    }

    @Override
    public <V> V parseRegisteredForReflection(InputStream inputStream, TypeLiteral<V> type) throws JsonParseException {
        try {
            JavaType javaType = mapper().constructType(type.getType());
            return mapper().readValue(inputStream, javaType);
        } catch (JacksonException e) {
            throw new JsonParseException("Failed to parse " + type, e);
        }
    }

    @Override
    public void write(
        OutputStream outputStream,
        ReflectionRegistered object,
        boolean pretty
    ) throws JsonParseException {
        try {
            if (pretty) {
                mapper().writerWithDefaultPrettyPrinter().writeValue(outputStream, object);
            } else {
                mapper().writeValue(outputStream, object);
            }
        } catch (JacksonException e) {
            throw new JsonParseException("Failed to write " + object + " to OutputStream", e);
        }
    }

    @Override
    public void write(Path file, ReflectionRegistered object, boolean pretty) throws JsonParseException {
        try {
            Path parent = file.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            if (pretty) {
                mapper().writerWithDefaultPrettyPrinter().writeValue(file, object);
            } else {
                mapper().writeValue(file, object);
            }
        } catch (IOException | JacksonException e) {
            throw new JsonParseException("Failed to write " + object + " to " + file, e);
        }
    }

    @Override
    public <V> Optional<V> parseVersioned(
        InputStream inputStream,
        TypeLiteral<V> type,
        int version
    ) throws JsonParseException {
        try {
            JsonNode node = mapper().readTree(inputStream);
            JsonNode versionNode = node.get("version");
            if (versionNode == null || versionNode.asInt() != version) {
                return Optional.empty();
            }

            JavaType javaType = mapper().constructType(type.getType());
            return Optional.of(mapper().treeToValue(node.get("object"), javaType));
        } catch (JacksonException e) {
            throw new JsonParseException("Failed to parse " + type, e);
        }
    }

    @Override
    public String getName() {
        return JACKSON;
    }

}
