package io.github.headlesshq.headlessmc.test;

import io.github.headlesshq.headlessmc.wrapper.HeadlessMcWrapper;
import io.github.headlesshq.headlessmc.wrapper.Main;
import io.github.headlesshq.headlessmc.wrapper.plugin.TransformerPlugin;
import io.github.headlesshq.headlessmc.wrapper.plugin.TransformingClassloader;
import io.github.headlesshq.headlessmc.wrapper.plugin.TransformingPluginFinder;
import lombok.extern.java.Log;
import org.junit.jupiter.api.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Log
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class IntegrationTest {
    private static final Path ROOT = Paths.get("HeadlessMC");

    @BeforeAll
    public static void beforeAll() throws IOException {
        deleteHeadlessMC();
    }

    @Test
    @Order(0)
    public void runIntegrationTest() throws Exception {
        // set the property in gradle.properties
        assertNotNull(System.getProperty("hmc.integration.test.enabled"));
        Assumptions.assumeTrue(Boolean.parseBoolean(System.getProperty("hmc.integration.test.enabled")));

        System.setProperty("integrationTestRunServer", "false");
        // assertFalse(Files.exists(Paths.get("HeadlessMC")), "HeadlessMC directory should be deleted!");

        prepareTest();

        HeadlessMcWrapper.setPluginFinderFactory(path -> new TransformingPluginFinder(ROOT.resolve("plugins")) {
            @Override
            protected TransformingClassloader newTransformingClassloader(URL[] urls, ClassLoader parent, URLClassLoader transformerClassloader, List<TransformerPlugin> plugins) {
                return new TransformingClassloaderWithJacoco(urls, parent, transformerClassloader, plugins);
            }
        });

        log.info("Running integration test!");
        Main.main(new String[0]);
    }

    @Test
    @Order(1)
    public void runServerIntegrationTest() throws Exception {// set the property in gradle.properties
        assertNotNull(System.getProperty("hmc.integration.test.enabled"));
        Assumptions.assumeTrue(Boolean.parseBoolean(System.getProperty("hmc.integration.test.enabled")));
        System.setProperty("hmc.server.test.cache", "true");

        prepareTest();
        System.setProperty("integrationTestRunServer", "true");
        Main.main(new String[0]);
    }

    @Test
    @Order(2) // run in memory last!
    public void runIntegrationTestInMemory() throws Exception {// set the property in gradle.properties
        assertNotNull(System.getProperty("hmc.integration.test.enabled"));
        Assumptions.assumeTrue(Boolean.parseBoolean(System.getProperty("hmc.integration.test.enabled")));

        System.setProperty("integrationTestRunServer", "false");
        prepareTest();
        System.setProperty("integrationTestRunInMemory", "true");
        Main.main(new String[0]);
    }

    private void prepareTest() throws IOException {
        System.setProperty("hmc.mcdir", ROOT.resolve("mcdir").toAbsolutePath().toString());
        System.setProperty("hmc.gamedir", ROOT.resolve("gamedir").toAbsolutePath().toString());

        Path testPlugin = Paths.get("build").resolve("libs").resolve("testPlugin.jar");
        assertTrue(Files.exists(testPlugin));
        Path plugins = ROOT.resolve("plugins");
        Files.createDirectories(plugins);
        assertTrue(Files.exists(Files.copy(testPlugin, plugins.resolve("testPlugin.jar"), StandardCopyOption.REPLACE_EXISTING)),
                   "Test plugin jar should exit in plugins!");
    }

    private static void deleteHeadlessMC() throws IOException {
        if (Files.exists(ROOT)) {
            try (Stream<Path> pathStream = Files.walk(ROOT)) {
                //noinspection ResultOfMethodCallIgnored
                pathStream.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
            }
        }
    }

}
