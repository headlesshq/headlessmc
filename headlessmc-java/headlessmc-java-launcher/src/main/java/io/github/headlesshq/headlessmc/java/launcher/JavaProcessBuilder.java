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

    List<String> buildCommand();

    JavaProcess start() throws HeadlessMcException;

    // Should we ever want to support AoT/AppCDS we need to register mods and hash them
    // so if the mod folder changes we create a new AoT/AppCDS archive
    // JavaProcessBuilder dynamicClasspath(Path... dynamicPaths);

    // TODO: accessors for all fields?

    String id();

}
