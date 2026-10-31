package io.github.headlesshq.headlessmc.gradle;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;

/**
 * Generates {@code META-INF/headlessmc-plugin.json}, the manifest HeadlessMc reads from every plugin jar.
 */
@CacheableTask
public abstract class GeneratePluginManifest extends DefaultTask {
    public static final String FILE_NAME = "headlessmc-plugin.json";
    public static final String JARS_DIR = "META-INF/jars";
    static final int SCHEMA_VERSION = 1;
    static final int MIN_JAVA = 25;
    private static final Pattern ID = Pattern.compile("[a-z0-9][a-z0-9_-]*");

    @Input
    public abstract Property<String> getPluginId();

    @Input
    public abstract Property<String> getPluginVersion();

    @Input
    @Optional
    public abstract Property<String> getPluginName();

    @Input
    @Optional
    public abstract Property<String> getPluginDescription();

    @Input
    public abstract ListProperty<String> getAuthors();

    @Input
    @Optional
    public abstract Property<String> getLicense();

    @Input
    @Optional
    public abstract Property<String> getHomepage();

    @Input
    public abstract Property<String> getHeadlessmc();

    @Input
    public abstract Property<Integer> getJava();

    @Input
    public abstract MapProperty<String, String> getDependencies();

    @Input
    public abstract SetProperty<String> getOptionalDependencies();

    @Input
    public abstract MapProperty<String, String> getLibraryVersions();

    /**
     * @return group:artifact:version:fileName of every nested library.
     */
    @Input
    public abstract ListProperty<String> getLibraries();

    /**
     * @return the nested library jars, to compute their hashes.
     */
    @InputFiles
    @PathSensitive(PathSensitivity.NAME_ONLY)
    public abstract ConfigurableFileCollection getLibraryFiles();

    @OutputFile
    public abstract RegularFileProperty getOutput();

    @TaskAction
    public void generate() throws IOException {
        String id = getPluginId().get();
        if (!ID.matcher(id).matches()) {
            throw new GradleException("Invalid plugin id '" + id + "', use lowercase letters, digits, - and _");
        }

        int java = getJava().get();
        if (java < MIN_JAVA) {
            throw new GradleException("HeadlessMc requires at least Java " + MIN_JAVA + ", but java is set to " + java);
        }

        Set<String> optional = getOptionalDependencies().get();
        Map<String, String> dependencyVersions = new TreeMap<>(getDependencies().get());
        for (String optionalId : optional) {
            if (!dependencyVersions.containsKey(optionalId)) {
                throw new GradleException("The optional dependency " + optionalId + " has no versions,"
                    + " declare it with optionalDependency '" + optionalId + "', '<versions>'");
            }
        }

        List<Map<String, Object>> dependencies = new ArrayList<>();
        dependencyVersions.forEach((dependencyId, versions) -> {
            Map<String, Object> dependency = new LinkedHashMap<>();
            dependency.put("id", dependencyId);
            dependency.put("versions", versions);
            dependency.put("optional", optional.contains(dependencyId));
            dependencies.add(dependency);
        });

        Map<String, Object> manifest = new LinkedHashMap<>();
        manifest.put("schemaVersion", SCHEMA_VERSION);
        manifest.put("id", id);
        manifest.put("version", getPluginVersion().get());
        putIfPresent(manifest, "name", getPluginName().getOrNull());
        putIfPresent(manifest, "description", getPluginDescription().getOrNull());
        manifest.put("authors", getAuthors().get());
        putIfPresent(manifest, "license", getLicense().getOrNull());
        putIfPresent(manifest, "homepage", getHomepage().getOrNull());
        manifest.put("headlessmc", getHeadlessmc().get());
        manifest.put("java", java);
        manifest.put("dependencies", dependencies);
        manifest.put("libraries", libraries());

        Files.writeString(getOutput().get().getAsFile().toPath(), Json.write(manifest));
    }

    private List<Map<String, Object>> libraries() {
        Map<String, File> filesByName = new HashMap<>();
        for (File file : getLibraryFiles().getFiles()) {
            filesByName.put(file.getName(), file);
        }

        Map<String, String> libraryVersions = getLibraryVersions().get();
        Set<String> paths = new HashSet<>();
        List<Map<String, Object>> libraries = new ArrayList<>();
        for (String coordinates : getLibraries().get()) {
            String[] parts = coordinates.split(":", 4);
            String group = parts[0];
            String artifact = parts[1];
            String fileName = parts[3];
            String path = JARS_DIR + "/" + fileName;
            if (!paths.add(path)) {
                throw new GradleException("Two included libraries are named " + fileName);
            }

            File file = filesByName.get(fileName);
            if (file == null) {
                throw new GradleException("Could not find the jar of included library " + coordinates);
            }

            Map<String, Object> library = new LinkedHashMap<>();
            library.put("group", group);
            library.put("artifact", artifact);
            library.put("version", parts[2]);
            putIfPresent(library, "versions", libraryVersions.get(group + ":" + artifact));
            library.put("path", path);
            library.put("sha256", sha256(file));
            libraries.add(library);
        }

        return libraries;
    }

    private static void putIfPresent(Map<String, Object> map, String key, @Nullable Object value) {
        if (value != null) {
            map.put(key, value);
        }
    }

    static String sha256(File file) {
        try (InputStream inputStream = Files.newInputStream(file.toPath())) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }

            return HexFormat.of().formatHex(digest.digest());
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to hash " + file, e);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

}
