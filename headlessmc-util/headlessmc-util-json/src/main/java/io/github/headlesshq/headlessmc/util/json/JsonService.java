package io.github.headlesshq.headlessmc.util.json;

import jakarta.enterprise.util.TypeLiteral;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public interface JsonService {
    String JACKSON = "jackson";

    <V extends io.github.headlesshq.headlessmc.reflection.ReflectionRegistered> V parse(InputStream inputStream, Class<V> type) throws JsonParseException;

    <V extends io.github.headlesshq.headlessmc.reflection.ReflectionRegistered> V parse(InputStream inputStream, TypeLiteral<V> type) throws JsonParseException;

    <V extends io.github.headlesshq.headlessmc.reflection.ReflectionRegistered> List<V> parseList(InputStream inputStream, TypeLiteral<List<V>> type) throws JsonParseException;

    <V extends io.github.headlesshq.headlessmc.reflection.ReflectionRegistered> V parse(Path file, Class<V> type) throws JsonParseException;

    <V extends io.github.headlesshq.headlessmc.reflection.ReflectionRegistered> V parse(Path file, TypeLiteral<V> type) throws JsonParseException;

    <V> V parseRegisteredForReflection(InputStream inputStream, TypeLiteral<V> type) throws JsonParseException;

    /**
     * Parses a value that was written as a version-stamped envelope, i.e. an
     * object with a {@code version} and an {@code object} field. The
     * {@code object} field is only parsed as {@code type} if the stored
     * {@code version} matches the given {@code version}; otherwise
     * {@link Optional#empty()} is returned, allowing stale or incompatible
     * data (e.g. after a schema change) to be skipped instead of failing to parse.
     *
     * @param inputStream the stream to parse.
     * @param type the type to parse the {@code object} field as.
     * @param version the expected version.
     * @return the parsed value, or {@link Optional#empty()} if the stored version did not match.
     */
    <V> Optional<V> parseVersioned(InputStream inputStream, TypeLiteral<V> type, int version) throws JsonParseException;

    void write(OutputStream outputStream, io.github.headlesshq.headlessmc.reflection.ReflectionRegistered object, boolean pretty) throws JsonParseException;

    void write(Path file, io.github.headlesshq.headlessmc.reflection.ReflectionRegistered object, boolean pretty) throws JsonParseException;

    String getName();

}
