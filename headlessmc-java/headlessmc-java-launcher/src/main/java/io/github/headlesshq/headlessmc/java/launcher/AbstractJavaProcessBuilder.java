package io.github.headlesshq.headlessmc.java.launcher;

import io.github.headlesshq.headlessmc.java.Java;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.nio.file.Path;
import java.util.*;

@Data
@Slf4j
public abstract class AbstractJavaProcessBuilder implements JavaProcessBuilder {
    private final List<Map.Entry<String, @Nullable String>> systemProperties = new ArrayList<>();
    private final List<String> classPath = new ArrayList<>();
    private final List<String> jvmArgs = new ArrayList<>();
    private final List<String> args = new ArrayList<>();

    private boolean pipeIO = false;
    private @Nullable Integer version;
    private @Nullable String id;
    private @Nullable Java java;
    private @Nullable String mainClass;
    private @Nullable Path directory;
    private @Nullable Path jar;

    @Override
    public JavaProcessBuilder id(@Nullable String id) {
        this.id = id;
        return this;
    }

    @Override
    public JavaProcessBuilder classpath(List<String> path) {
        classPath.addAll(path);
        return this;
    }

    @Override
    public JavaProcessBuilder classpath(Path... path) {
        Arrays.stream(path).map(Path::toAbsolutePath).map(Path::toString).forEach(classPath::add);
        return this;
    }

    @Override
    public JavaProcessBuilder classpath(String... path) {
        classPath.addAll(Arrays.asList(path));
        return this;
    }

    @Override
    public JavaProcessBuilder jvmArg(String... arg) {
        jvmArgs.addAll(Arrays.asList(arg));
        return this;
    }

    @Override
    public JavaProcessBuilder arg(String... arg) {
        args.addAll(Arrays.asList(arg));
        return this;
    }

    @Override
    public JavaProcessBuilder mainClass(String mainClass) {
        this.mainClass = mainClass;
        return this;
    }

    @Override
    public JavaProcessBuilder directory(Path path) {
        this.directory = path;
        return this;
    }

    @Override
    public JavaProcessBuilder jar(Path jar) {
        this.jar = jar;
        return this;
    }

    @Override
    public JavaProcessBuilder version(int javaVersion) {
        this.version = javaVersion;
        return this;
    }

    @Override
    public JavaProcessBuilder java(Java java) {
        this.java = java;
        return this;
    }

    @Override
    public JavaProcessBuilder systemProperty(String name, @Nullable String value) {
        this.systemProperties.add(new AbstractMap.SimpleEntry<>(name, value));
        return this;
    }

    @Override
    public JavaProcessBuilder systemProperties(Map<String, @Nullable String> keyValuePairs) {
        this.systemProperties.addAll(keyValuePairs.entrySet());
        return this;
    }

    @Override
    public JavaProcessBuilder pipeIO(boolean pipe) {
        this.pipeIO = pipe;
        return this;
    }

    @Override
    public String id() {
        return id == null ? "<unknown>" : id;
    }

    @Override
    public List<String> buildCommand() {
        List<String> systemProperties = buildSystemProperties();
        List<String> result = new ArrayList<>(jvmArgs.size() + systemProperties.size() + 5 + args.size());
        result.addAll(jvmArgs);
        result.addAll(systemProperties);

        result.add("-cp");
        result.add(String.join(File.pathSeparator, classPath));

        Path jar = this.jar;
        String main = this.mainClass;
        if (jar == null && mainClass == null) {
            throw new IllegalStateException("Neither jar nor main-class were specified");
        }

        if (jar != null) {
            result.add("-jar");
            result.add(jar.toAbsolutePath().toString()); // is absolute path needed? depending on dir?
        } else {
            result.add(main);
        }

        result.addAll(args);
        return result;
    }

    public String getId() {
        String result = this.id;
        if (result == null) {
            String mainClass = this.mainClass;
            if (mainClass == null) {
                Path jar = this.jar;
                if (jar == null) {
                    return "Java Process";
                }

                return "Java Process " + jar;
            }

            return "Java Process " + mainClass;
        }

        return result;
    }

    protected List<String> buildSystemProperties() {
        Map<String, @Nullable String> systemProperties = systemPropertiesToMap();
        List<String> result = new ArrayList<>(systemProperties.size());
        for (Map.Entry<String, @Nullable String> entry : systemProperties.entrySet()) {
            StringBuilder property = new StringBuilder("-D").append(entry.getKey());
            if (entry.getValue() != null) {
                property.append("=").append(entry.getValue());
            }

            result.add(property.toString());
        }

        return result;
    }

    protected Map<String, @Nullable String> systemPropertiesToMap() {
        Map<String, @Nullable String> systemProperties = new LinkedHashMap<>();
        for (Map.Entry<String, @Nullable String> entry : this.systemProperties) {
            String before = systemProperties.put(entry.getKey(), entry.getValue());
            if (before != null) {
                log.warn("Property {}={} overridden by {}", entry.getKey(), before, entry.getValue());
                // but later properties should override earlier properties
                // TODO: what if SystemProperty is defined twice accidentally?
            }
        }

        return systemProperties;
    }

}
