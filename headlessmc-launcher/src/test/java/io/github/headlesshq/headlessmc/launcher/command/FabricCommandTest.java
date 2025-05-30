package io.github.headlesshq.headlessmc.launcher.command;

import lombok.val;
import io.github.headlesshq.headlessmc.api.command.CommandException;
import io.github.headlesshq.headlessmc.java.Java;
import io.github.headlesshq.headlessmc.launcher.LauncherMock;
import io.github.headlesshq.headlessmc.launcher.LauncherProperties;
import io.github.headlesshq.headlessmc.launcher.UsesResources;
import io.github.headlesshq.headlessmc.launcher.version.ParsesVersions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class FabricCommandTest implements UsesResources, ParsesVersions {
    @Test
    public void testDownloadFailure() {
        val command = new FabricCommand(LauncherMock.INSTANCE);
        System.setProperty(LauncherProperties.FABRIC_URL.getName(), "invalid");
        assertEquals("invalid", LauncherMock.INSTANCE.getConfig().get(LauncherProperties.FABRIC_URL));

        val version = parseVersion(getJsonObject("version_parent.json"));
        assertThrows(CommandException.class, () -> command.execute(
            version, "fabric", version.getName()));
    }

    @Test
    public void testGetCommand() {
        val command = new FabricCommand(LauncherMock.INSTANCE);
        val version = parseVersion(getJsonObject("version_parent.json"));
        assertEquals("1.19", version.getName());
        val java = new Java("dummyJava", 17);
        val jar = new File("dummyJar");

        val  result = command.getCommand(version, java, jar, new ArrayList<>(), false);
        assertEquals(Arrays.asList(
            "dummyJava", "-jar", jar.getAbsolutePath(), "client",
            "-noprofile", "-mcversion", "1.19", "-dir",
            LauncherMock.INSTANCE.getMcFiles().getBase().toPath().toAbsolutePath().toString()), result);
    }

}
