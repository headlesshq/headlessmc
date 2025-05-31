package io.github.headlesshq.headlessmc.launcher.launch;

import io.github.headlesshq.headlessmc.api.util.ResourceUtil;
import io.github.headlesshq.headlessmc.java.Java;
import io.github.headlesshq.headlessmc.launcher.Launcher;
import io.github.headlesshq.headlessmc.launcher.LauncherMock;
import io.github.headlesshq.headlessmc.launcher.LauncherProperties;
import io.github.headlesshq.headlessmc.launcher.command.LaunchContext;
import io.github.headlesshq.headlessmc.launcher.util.IOUtil;
import lombok.Cleanup;
import lombok.SneakyThrows;
import lombok.val;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class LaunchTest {
    private final Launcher launcher = LauncherMock.INSTANCE;

    @Test
    @SneakyThrows
    public void testLaunch() {
        System.setProperty(LauncherProperties.SET_LIBRARY_DIR.getName(), "false");
        setupLauncher();
        ProcessBuilder builder = ((MockProcessFactory) launcher.getProcessFactory())
            .getBuilder();
        assertNull(builder);

        launcher.getCommandLine().getCommandContext().execute("launch 1.19-launch-test -stay");

        builder = ((MockProcessFactory) launcher.getProcessFactory()).getBuilder();
        assertNotNull(builder);

        List<String> command = builder.command();
        assertEquals(11, command.size());
        assertEquals("java17", command.get(0));
        assertEquals("-Djoml.nounsafe=true", command.get(1));
        assertTrue(command.get(2).startsWith("-Djava.library.path="));
        assertEquals("-cp", command.get(3));
        assertEquals(3, command.get(4).split(File.pathSeparator).length);
        assertEquals("-DSomeSystemProperty=${some_arg}", command.get(5));
        assertEquals("path.to.MainClass", command.get(6));
        assertEquals("--username", command.get(7));
        assertEquals("Offline", command.get(8));
        assertEquals("--versionType", command.get(9));
        assertEquals("release", command.get(10));
    }

    @Test
    public void testWithVmArgs() {
        System.setProperty(LauncherProperties.SET_LIBRARY_DIR.getName(), "false");
        setupLauncher();
        launcher.getCommandLine().getCommandContext().execute(
            // TODO: all this escaping is kinda ehh
            //   maybe allow multiple --jvm flags?
            "launch 1.19-launch-test -stay --jvm \"-testVmArg -testVmArg\\\\" +
                " withEscapedSpace \\\"-testVmArg2 with space\\\"" +
                " -DVMSystemProp=\\\\\\\"systemProp\\\\ with\\\\" +
                " space\\\\\\\"\"");

        val cmd = ((MockProcessFactory) launcher.getProcessFactory())
            .getBuilder()
            .command();

        assertEquals(15, cmd.size());
        assertEquals("java17", cmd.get(0));
        assertEquals("-testVmArg", cmd.get(1));
        assertEquals("-testVmArg withEscapedSpace", cmd.get(2));
        assertEquals("-testVmArg2 with space", cmd.get(3));
        assertEquals("-DVMSystemProp=\"systemProp with space\"", cmd.get(4));
    }

    private void setupLauncher() {
        resetLauncher();
        unpackVersion();
        val vs = launcher.getVersionService();
        vs.refresh();
        assertTrue(vs.iterator().hasNext());
        assertEquals(1, vs.size());
        assertEquals("1.19-launch-test", vs.iterator().next().getName());
    }

    private void resetLauncher() {
        launcher.getJavaService().clear();
        launcher.getJavaService().add(new Java("java17", 17));
        launcher.getJavaService().add(new Java("java8", 8));
        launcher.getCommandLine().setCommandContext(new LaunchContext(launcher));
    }

    @SneakyThrows
    private void unpackVersion() {
        val versionFile = launcher.getMcFiles().create(
            "versions", "version", "version.json");
        @Cleanup
        val is = ResourceUtil.getResource("launch.json");
        @Cleanup
        val os = new FileOutputStream(versionFile);
        IOUtil.copy(is, os);
    }

}
