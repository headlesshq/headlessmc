package io.github.headlesshq.headlessmc.java.launcher;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.java.Java;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public interface JavaProcessBuilder {
    JavaProcessBuilder id(@Nullable String id);

    JavaProcessBuilder classpath(List<String> path);

    JavaProcessBuilder classpath(Path... path);

    JavaProcessBuilder classpath(String... path);

    JavaProcessBuilder jvmArg(String... arg);

    JavaProcessBuilder arg(String... arg);

    JavaProcessBuilder mainClass(String mainClass);

    JavaProcessBuilder directory(Path path);

    JavaProcessBuilder jar(Path jar);

    JavaProcessBuilder version(int version);

    JavaProcessBuilder java(Java java);

    JavaProcessBuilder systemProperty(String name, @Nullable String value);

    JavaProcessBuilder systemProperties(Map<String, @Nullable String> keyValuePairs);

    JavaProcessBuilder pipeIO(boolean pipe);

    /**
     * @param provided {@code true} if the jvm args already contain a classpath argument
     *                 ({@code -cp}, {@code -classpath} or {@code --class-path}),
     *                 in which case the builder will not add its own.
     * @return this builder.
     */
    JavaProcessBuilder classpathArgProvided(boolean provided);

    List<String> buildCommand();

    /**
     * Resolves the Java runtime that {@link #start()} would launch the process with.
     * This may download a Java runtime if none matching the requested version is installed
     * and downloading is enabled.
     *
     * @return the Java runtime the process would be launched with.
     * @throws HeadlessMcException if no matching Java runtime could be found or downloaded.
     */
    Java resolveJava() throws HeadlessMcException;

    JavaProcess start() throws HeadlessMcException;

    // Should we ever want to support AoT/AppCDS we need to register mods and hash them
    // so if the mod folder changes we create a new AoT/AppCDS archive
    // JavaProcessBuilder dynamicClasspath(Path... dynamicPaths);

    // TODO: accessors for all fields?

    String id();

}
