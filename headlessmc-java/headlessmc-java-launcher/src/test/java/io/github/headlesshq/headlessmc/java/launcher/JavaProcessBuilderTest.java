package io.github.headlesshq.headlessmc.java.launcher;

import io.github.headlesshq.headlessmc.java.Java;
import io.github.headlesshq.headlessmc.java.SafePath;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JavaProcessBuilderTest {
    private static final class TestBuilder extends AbstractJavaProcessBuilder {
        @Override
        public JavaProcess start() {
            throw new UnsupportedOperationException();
        }
    }

    private TestBuilder builder() {
        return new TestBuilder();
    }

    @Test
    void buildCommandRequiresJarOrMainClass() {
        assertThrows(IllegalStateException.class, builder()::buildCommand);
    }

    @Test
    void buildCommandWithMainClass() {
        List<String> command = builder()
            .jvmArg("-Xmx2G")
            .classpath("a.jar", "b.jar")
            .mainClass("com.example.Main")
            .arg("--foo", "bar")
            .buildCommand();

        assertEquals(
            List.of("-Xmx2G", "-cp", "a.jar" + File.pathSeparator + "b.jar", "com.example.Main", "--foo", "bar"),
            command
        );
    }

    @Test
    void buildCommandWithJar() {
        Path jar = Path.of("server.jar");
        List<String> command = builder().jar(jar).buildCommand();

        assertEquals(List.of("-jar", jar.toAbsolutePath().toString()), command);
    }

    @Test
    void jarWinsOverMainClass() {
        Path jar = Path.of("server.jar");
        List<String> command = builder().jar(jar).mainClass("com.example.Main").buildCommand();

        assertTrue(command.contains("-jar"));
        assertFalse(command.contains("com.example.Main"));
    }

    @Test
    void classpathAcceptsListPathsAndStrings() {
        Path path = Path.of("c.jar");
        List<String> command = builder()
            .classpath(List.of("a.jar"))
            .classpath("b.jar")
            .classpath(path)
            .mainClass("Main")
            .buildCommand();

        String classpath = command.get(command.indexOf("-cp") + 1);
        assertEquals(
            String.join(File.pathSeparator, "a.jar", "b.jar", path.toAbsolutePath().toString()),
            classpath
        );
    }

    @Test
    void systemPropertiesAreRenderedWithAndWithoutValue() {
        List<String> command = builder()
            .systemProperty("a", "1")
            .systemProperty("b", null)
            .mainClass("Main")
            .buildCommand();

        assertTrue(command.contains("-Da=1"));
        assertTrue(command.contains("-Db"));
    }

    @Test
    void laterSystemPropertiesOverrideEarlierOnes() {
        Map<String, String> properties = new LinkedHashMap<>();
        properties.put("a", "2");

        List<String> command = builder()
            .systemProperty("a", "1")
            .systemProperties(properties)
            .mainClass("Main")
            .buildCommand();

        assertTrue(command.contains("-Da=2"));
        assertFalse(command.contains("-Da=1"));
    }

    @Test
    void idFallsBackToMainClassThenJar() {
        assertEquals("Java Process", builder().getId());

        TestBuilder withJar = builder();
        withJar.jar(Path.of("a.jar"));
        assertEquals("Java Process " + Path.of("a.jar"), withJar.getId());

        TestBuilder withMain = builder();
        withMain.mainClass("Main");
        assertEquals("Java Process Main", withMain.getId());

        TestBuilder withId = builder();
        withId.id("custom");
        assertEquals("custom", withId.getId());
    }

    @Test
    void settersAreRecorded() {
        Java java = new Java("j", 21, new SafePath(Path.of("home")), new SafePath(Path.of("home/bin/java")), false, 0);

        TestBuilder builder = builder();
        builder.version(21)
            .java(java)
            .directory(Path.of("dir"))
            .pipeIO(true);

        assertEquals(21, builder.getVersion());
        assertEquals(java, builder.getJava());
        assertEquals(Path.of("dir"), builder.getDirectory());
        assertTrue(builder.isPipeIO());
    }

    @Test
    void emptySystemPropertiesMapChangesNothing() {
        List<String> command = builder()
            .systemProperties(new HashMap<>())
            .mainClass("Main")
            .buildCommand();

        assertEquals(List.of("Main"), command);
    }

}
