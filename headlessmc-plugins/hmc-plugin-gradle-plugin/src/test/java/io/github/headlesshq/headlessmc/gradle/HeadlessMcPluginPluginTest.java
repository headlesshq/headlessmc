package io.github.headlesshq.headlessmc.gradle;

import io.github.headlesshq.headlessmc.net.hash.DefaultHashAlgorithmProvider;
import io.github.headlesshq.headlessmc.net.hash.HashAlgorithmProvider;
import io.github.headlesshq.headlessmc.net.hash.HashServiceImpl;
import io.github.headlesshq.headlessmc.plugins.PluginFile;
import io.github.headlesshq.headlessmc.plugins.PluginManifest;
import io.github.headlesshq.headlessmc.plugins.PluginScanner;
import io.github.headlesshq.headlessmc.util.json.jackson.DefaultJacksonJsonService;
import jakarta.enterprise.inject.Instance;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class HeadlessMcPluginPluginTest {
    @TempDir
    Path dir;

    private Path repository;
    private Path project;

    @BeforeEach
    void setup() throws IOException {
        repository = dir.resolve("repository");
        project = dir.resolve("project");
        // HeadlessMc ships commons-lang3 3.21.0, plugins never nest it
        publish("io.github.headlesshq", "headlessmc-application", "1.0.0", "org.apache.commons:commons-lang3:3.21.0");
        publish("org.apache.commons", "commons-lang3", "3.21.0");
        publish("org.apache.commons", "commons-lang3", "3.20.0");
        publish("org.apache.commons", "commons-lang3", "4.0.0");
        publish("com.example", "lib", "1.2.3", "org.apache.commons:commons-lang3:3.20.0", "com.example:transitive:2.0.0");
        publish("com.example", "transitive", "2.0.0");
        publish("com.example", "next-major", "1.0.0", "org.apache.commons:commons-lang3:4.0.0");
        Files.createDirectories(project.resolve("src/main/java/com/example"));
        Files.writeString(project.resolve("src/main/java/com/example/Example.java"), "package com.example; public class Example {}");
        Files.writeString(project.resolve("settings.gradle"), "rootProject.name = 'example-plugin'\n");
        Files.writeString(project.resolve("gradle.properties"), "version=1.0.0\n");
    }

    @Test
    void testJar() throws IOException {
        buildGradle("""
            headlessmcPlugin {
                headlessmcVersion = '1.0.0'
                id = 'example'
                description = 'An example plugin'
                authors = ['someone']
                license = 'MIT'
                dependency 'required', '[1,2)'
                optionalDependency 'optional', '*'
                libraryVersions 'com.example:lib', '[1.2,3)'
            }

            dependencies {
                include 'com.example:lib:1.2.3'
            }
            """);
        runner("jar").build();

        Path jar = project.resolve("build/libs/example-plugin-1.0.0.jar");
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            List<String> entries = Collections.list(zip.entries()).stream().map(ZipEntry::getName).toList();
            assertTrue(entries.contains("com/example/Example.class"), entries.toString());
            assertTrue(entries.contains("META-INF/jars/lib-1.2.3.jar"), entries.toString());
            assertTrue(entries.contains("META-INF/jars/transitive-2.0.0.jar"), entries.toString());
            assertTrue(entries.stream().noneMatch(entry -> entry.contains("commons-lang3")), entries.toString());
        }

        Instance<HashAlgorithmProvider> providers = Mockito.mock(Instance.class);
        Mockito.when(providers.stream()).thenAnswer(_ -> Stream.of(new DefaultHashAlgorithmProvider()));
        PluginFile file = new PluginScanner(new HashServiceImpl(providers), new DefaultJacksonJsonService()).read(jar);
        assertNull(file.error());
        PluginManifest manifest = file.manifest();
        assertNotNull(manifest);
        assertEquals("example", manifest.id());
        assertEquals("1.0.0", manifest.version());
        assertEquals("example", manifest.displayName());
        assertEquals("An example plugin", manifest.description());
        assertEquals(List.of("someone"), manifest.authors());
        assertEquals("1.0.0", manifest.headlessmc());
        assertEquals(25, manifest.java());
        assertEquals(List.of(
            new PluginManifest.Dependency("optional", "*", true),
            new PluginManifest.Dependency("required", "[1,2)", false)
        ), manifest.dependencyList());

        List<PluginManifest.Library> libraries = manifest.libraryList();
        assertEquals(2, libraries.size());
        PluginManifest.Library lib = libraries.stream().filter(l -> l.artifact().equals("lib")).findFirst().orElseThrow();
        assertEquals("com.example", lib.group());
        assertEquals("1.2.3", lib.version());
        assertEquals("[1.2,3)", lib.versions());
        assertEquals("META-INF/jars/lib-1.2.3.jar", lib.path());
        assertEquals(GeneratePluginManifest.sha256(repository.resolve("com/example/lib/1.2.3/lib-1.2.3.jar").toFile()), lib.sha256());
    }

    @Test
    void testCoreLibraryWithOtherMajorVersion() throws IOException {
        buildGradle("""
            headlessmcPlugin {
                headlessmcVersion = '1.0.0'
                id = 'example'
            }

            dependencies {
                include 'com.example:next-major:1.0.0'
            }
            """);
        BuildResult result = runner("jar").buildAndFail();
        assertTrue(result.getOutput().contains("plugins cannot replace libraries of HeadlessMc"), result.getOutput());
    }

    @Test
    void testInvalidId() throws IOException {
        buildGradle("""
            headlessmcPlugin {
                headlessmcVersion = '1.0.0'
                id = 'Invalid Id'
            }
            """);
        BuildResult result = runner("jar").buildAndFail();
        assertTrue(result.getOutput().contains("Invalid plugin id 'Invalid Id'"), result.getOutput());
    }

    @Test
    void testDefaultVersionIsPluginVersion() {
        String version = HeadlessMcPluginPlugin.getVersion();
        assertNotNull(version);
        assertFalse(version.contains("${"), version);
    }

    private GradleRunner runner(String... arguments) {
        return GradleRunner.create()
            .withProjectDir(project.toFile())
            .withPluginClasspath()
            .withArguments(Stream.concat(Stream.of(arguments), Stream.of("--stacktrace", "--configuration-cache", "--warning-mode=fail")).toList());
    }

    private void buildGradle(String configuration) throws IOException {
        Files.writeString(project.resolve("build.gradle"), """
            plugins {
                id 'io.github.headlesshq.headlessmc-plugin'
            }

            repositories {
                maven { url = uri('%s') }
            }

            """.formatted(repository.toUri()) + configuration);
    }

    private void publish(String group, String artifact, String version, String... dependencies) throws IOException {
        Path directory = repository.resolve(group.replace('.', '/')).resolve(artifact).resolve(version);
        Files.createDirectories(directory);
        StringBuilder pom = new StringBuilder("""
            <project xmlns="http://maven.apache.org/POM/4.0.0">
              <modelVersion>4.0.0</modelVersion>
              <groupId>%s</groupId>
              <artifactId>%s</artifactId>
              <version>%s</version>
              <dependencies>
            """.formatted(group, artifact, version));
        for (String dependency : dependencies) {
            String[] parts = dependency.split(":");
            pom.append("""
                    <dependency><groupId>%s</groupId><artifactId>%s</artifactId><version>%s</version></dependency>
                """.formatted(parts[0], parts[1], parts[2]));
        }

        pom.append("  </dependencies>\n</project>\n");
        Files.writeString(directory.resolve(artifact + "-" + version + ".pom"), pom.toString());
        try (OutputStream outputStream = Files.newOutputStream(directory.resolve(artifact + "-" + version + ".jar"));
             ZipOutputStream zip = new ZipOutputStream(outputStream)) {
            zip.putNextEntry(new ZipEntry(artifact + ".txt"));
            zip.write((group + ":" + artifact + ":" + version).getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
    }

}
