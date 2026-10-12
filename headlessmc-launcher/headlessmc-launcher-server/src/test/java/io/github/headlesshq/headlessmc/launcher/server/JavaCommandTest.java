package io.github.headlesshq.headlessmc.launcher.server;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JavaCommandTest {
    @TempDir
    Path root;

    private JavaCommand parse(String... args) {
        return JavaCommand.parse(List.of(args), root, StandardCharsets.UTF_8);
    }

    @Test
    void argFilesAreSplitLikeTheJavaLauncherDoes() {
        assertEquals(
            List.of("-Xmx4G", "-Dkey=a value", "it's", "--opt", "a\tb", "c"),
            JavaCommand.parseArgFile("""
                # comment
                -Xmx4G   "-Dkey=a value"
                "it's" --opt # trailing comment
                'a\\tb' c""")
        );
    }

    @Test
    void mainClassAndArgsAreParsed() {
        JavaCommand command = parse("-Xmx1G", "-cp", "a.jar" + File.pathSeparator + "b.jar", "-p", "m.jar", "my.Main", "-cp", "arg");

        assertEquals(List.of("-Xmx1G", "-p", "m.jar"), command.jvmArgs());
        assertEquals(List.of("a.jar", "b.jar"), command.classpath());
        assertEquals("my.Main", command.mainClass());
        assertNull(command.jar());
        assertEquals(List.of("-cp", "arg"), command.args());
    }

    @Test
    void jarIsResolvedAgainstTheWorkingDir() {
        JavaCommand command = parse("-Xmx1G", "-jar", "server.jar", "nogui");

        assertEquals(root.resolve("server.jar"), command.jar());
        assertNull(command.mainClass());
        assertEquals(List.of("nogui"), command.args());
    }

    @Test
    void argFilesAreExpandedUntilTheMainClass() throws IOException {
        Files.writeString(root.resolve("jvm.txt"), "-Xmx1G");
        Files.writeString(root.resolve("main.txt"), "--class-path=a.jar my.Main @app.txt");

        JavaCommand command = parse("@jvm.txt", "@main.txt", "@other.txt");

        assertEquals(List.of("-Xmx1G"), command.jvmArgs());
        assertEquals(List.of("a.jar"), command.classpath());
        assertEquals("my.Main", command.mainClass());
        assertEquals(List.of("@app.txt", "@other.txt"), command.args());
    }

    @Test
    void doubleAtEscapesArgFiles() {
        assertEquals("@Main", parse("@@Main").mainClass());
    }

    @Test
    void invalidCommandsThrow() {
        assertThrows(ServerException.class, () -> parse("-Xmx1G"));
        assertThrows(ServerException.class, () -> parse("-cp"));
        assertThrows(ServerException.class, () -> parse("-m", "module/Main"));
        assertThrows(ServerException.class, () -> parse("@missing.txt"));
    }

}
