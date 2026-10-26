package io.github.headlesshq.headlessmc.updater;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class UpdateTest {
    private static final String URL = "https://github.com/headlesshq/headlessmc/releases/download/3.0.0-RC9/";
    private static final GitHubRelease.Asset NATIVE = new GitHubRelease.Asset("headlessmc-launcher-linux-x64", URL + "headlessmc-launcher-linux-x64");
    private static final GitHubRelease.Asset JAR = new GitHubRelease.Asset("headlessmc.jar", URL + "headlessmc.jar");
    private static final GitHubRelease RELEASE = new GitHubRelease(
        "3.0.0-RC9",
        "https://github.com/headlesshq/headlessmc/releases/tag/3.0.0-RC9",
        List.of(NATIVE, JAR)
    );

    @Test
    public void testNativeUnix() {
        Path path = Path.of("/opt/it's/hmc");
        List<String> message = update(new Installation(Installation.Type.NATIVE, path), NATIVE, false).getMessage();
        assertEquals("A new version of HeadlessMc is available: 3.0.0-RC8 -> 3.0.0-RC9", message.get(0));
        assertEquals("Release: " + RELEASE.htmlUrl(), message.get(1));
        String download = quote(path.resolveSibling("hmc.new").toString());
        assertEquals("  curl -fL -o " + download + " '" + NATIVE.downloadUrl() + "'"
            + " && chmod +x " + download
            + " && mv -f " + download + " " + quote(path.toString()), message.getLast());
    }

    @Test
    public void testJarUnix() {
        Path path = Path.of("/opt/hmc/headlessmc.jar");
        String command = update(new Installation(Installation.Type.JAR, path), JAR, false).getMessage().getLast();
        assertTrue(command.startsWith("  curl -fL -o "), command);
        assertFalse(command.contains("chmod"), command);
        assertTrue(command.endsWith(" && mv -f " + quote(path + ".new") + " " + quote(path.toString())), command);
    }

    @Test
    public void testWindows() {
        Path path = Path.of("hmc", "headlessmc-launcher-windows-x64.exe").toAbsolutePath();
        List<String> message = update(new Installation(Installation.Type.NATIVE, path), NATIVE, true).getMessage();
        assertTrue(message.get(2).contains("PowerShell"), message.get(2));
        String download = "'" + path.resolveSibling("headlessmc-launcher-windows-x64.exe.new") + "'";
        assertEquals("  Invoke-WebRequest -Uri '" + NATIVE.downloadUrl() + "' -OutFile " + download
            + "; Move-Item -Force " + download + " '" + path + "'", message.getLast());
    }

    @Test
    public void testUnknownPath() {
        List<String> message = update(new Installation(Installation.Type.NATIVE, null), NATIVE, false).getMessage();
        assertEquals("Download: " + NATIVE.downloadUrl(), message.getLast());
    }

    @Test
    public void testNoAsset() {
        List<String> message = update(new Installation(Installation.Type.UNKNOWN, null), null, false).getMessage();
        assertEquals(2, message.size());
        assertEquals("Release: " + RELEASE.htmlUrl(), message.getLast());
    }

    private static Update update(Installation installation, GitHubRelease.@Nullable Asset asset, boolean windows) {
        return new Update("3.0.0-RC8", RELEASE, installation, asset, windows);
    }

    private static String quote(String value) {
        return "'" + value.replace("'", "'\\''") + "'";
    }

}
