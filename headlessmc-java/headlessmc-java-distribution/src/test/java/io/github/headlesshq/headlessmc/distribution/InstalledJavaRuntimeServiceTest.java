package io.github.headlesshq.headlessmc.distribution;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.os.CPU;
import io.github.headlesshq.headlessmc.os.OS;
import io.github.headlesshq.headlessmc.util.json.jackson.DefaultJacksonJsonService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class InstalledJavaRuntimeServiceTest {
    private final InstalledJavaRuntimeService service = new InstalledJavaRuntimeService(new DefaultJacksonJsonService());

    @TempDir
    Path root;

    @Test
    void writtenRuntimeCanBeRead() {
        JavaRuntime runtime = new JavaRuntime(
            "foojay.io", "temurin", "pkg-id", "OpenJDK21U-jre_x64_linux_hotspot_21.0.5_11", 21,
            new JavaRuntime.MetaData(123L, "tar.gz", "21.0.5+11", "jre")
        );

        service.write(root, runtime);

        assertEquals(
            Optional.of(new JavaRuntime(
                "foojay.io", "temurin", "pkg-id", "OpenJDK21U-jre_x64_linux_hotspot_21.0.5_11", 21,
                new JavaRuntime.MetaData(null, "tar.gz", "21.0.5+11", "jre")
            )),
            service.read(root)
        );
    }

    @Test
    void writesJson() throws IOException {
        service.write(root, new JavaRuntime("foojay.io", "temurin", "id", "name", 17, null));

        String json = Files.readString(root.resolve(InstalledJavaRuntimeService.FILE_NAME));
        assertTrue(json.contains("\"version\" : " + InstalledJavaRuntimeService.FORMAT_VERSION), json);
        assertTrue(json.contains("\"distribution\" : \"temurin\""), json);
        assertTrue(json.contains("\"majorVersion\" : 17"), json);
    }

    @Test
    void otherFormatVersionIsEmpty() throws IOException {
        write("{\"version\": " + (InstalledJavaRuntimeService.FORMAT_VERSION + 1) + ", \"object\": {\"something\": \"else\"}}");
        assertEquals(Optional.empty(), service.read(root));
    }

    @Test
    void missingFormatVersionIsEmpty() throws IOException {
        write("{\"provider\": \"foojay.io\", \"majorVersion\": 21}");
        assertEquals(Optional.empty(), service.read(root));
    }

    @Test
    void runtimeWithoutMetaDataCanBeRead() {
        service.write(root, new JavaRuntime("foojay.io", "temurin", "id", "name", 17, null));

        JavaRuntime runtime = service.read(root).orElseThrow();
        assertEquals(17, runtime.version());
        assertNull(runtime.javaVersion());
        assertNull(runtime.packageType());
    }

    @Test
    void missingFileIsEmpty() {
        assertEquals(Optional.empty(), service.read(root));
    }

    @Test
    void missingFieldsThrow() throws IOException {
        write(versioned("{\"provider\": \"foojay.io\", \"majorVersion\": 21}"));
        assertThrows(JavaDistributionException.class, () -> service.read(root));
    }

    @Test
    void missingMajorVersionThrows() throws IOException {
        write(versioned("{\"provider\": \"foojay.io\", \"distribution\": \"temurin\", \"id\": \"id\", \"name\": \"name\"}"));
        assertThrows(HeadlessMcException.class, () -> service.read(root));
    }

    @Test
    void invalidJsonThrows() throws IOException {
        write("{");
        assertThrows(HeadlessMcException.class, () -> service.read(root));
    }

    @Test
    void installDownloadsAndRecordsTheRuntime() {
        List<Path> downloads = new ArrayList<>();
        JavaRuntime runtime = new JavaRuntime("foojay.io", "temurin", "id", "name", 21, null);
        Path path = root.resolve("name");

        service.install(new DownloadingProvider(downloads), runtime, path);

        assertEquals(List.of(path), downloads);
        assertEquals(Optional.of(runtime.name()), service.read(path).map(JavaRuntime::name));
    }

    private void write(String json) throws IOException {
        Files.writeString(root.resolve(InstalledJavaRuntimeService.FILE_NAME), json);
    }

    private static String versioned(String object) {
        return "{\"version\": " + InstalledJavaRuntimeService.FORMAT_VERSION + ", \"object\": " + object + "}";
    }

    private record DownloadingProvider(List<Path> downloads) implements JavaDistributionProvider {
        @Override
        public String getName() {
            return "foojay.io";
        }

        @Override
        public List<JavaDistribution> getDistributions() {
            return List.of();
        }

        @Override
        public JavaDistribution getDistributionByName(String name) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<JavaRuntime> getRuntimes(JavaDistribution distribution, int version, OS os, CPU cpu) {
            return List.of();
        }

        @Override
        public Optional<JavaRuntime> getUpdate(JavaRuntime runtime, OS os, CPU cpu) {
            return Optional.empty();
        }

        @Override
        public void download(JavaRuntime runtime, Path path) {
            downloads.add(path);
            try {
                Files.createDirectories(path);
            } catch (IOException e) {
                throw new HeadlessMcException(e);
            }
        }
    }

}
