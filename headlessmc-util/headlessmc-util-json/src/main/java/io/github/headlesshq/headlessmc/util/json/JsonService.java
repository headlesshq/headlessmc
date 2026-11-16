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

    /**
     * Converts an untyped json tree into a typed value.
     * A field declared as {@link Object} is parsed into a tree of maps, lists and primitives.
     * This allows a class to carry json it does not know the type of, e.g. the data of a
     * launch target that is provided by a plugin, and lets the owner of that data
     * convert it into its own type later.
     * Values that already are of the given type are returned as they are.
     *
     * @param tree the tree to convert, as parsed for a field of type {@link Object}, or a value of the given type.
     * @param type the type to convert the tree to.
     * @param <V>  the type to convert the tree to.
     * @return the converted value.
     * @throws JsonParseException if the tree cannot be converted into the given type.
     */
    <V extends io.github.headlesshq.headlessmc.reflection.ReflectionRegistered> V convert(Object tree, Class<V> type) throws JsonParseException;

    void write(OutputStream outputStream, io.github.headlesshq.headlessmc.reflection.ReflectionRegistered object, boolean pretty) throws JsonParseException;

    void write(Path file, io.github.headlesshq.headlessmc.reflection.ReflectionRegistered object, boolean pretty) throws JsonParseException;

    String getName();

}
