package io.github.headlesshq.headlessmc.updater;

import io.github.headlesshq.headlessmc.os.CPU;
import io.github.headlesshq.headlessmc.os.OS;
import io.quarkus.test.component.QuarkusComponentTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusComponentTest(UpdateService.class)
public class UpdateServiceTest {
    @Inject
    UpdateService updateService;

    @Test
    public void testIsUpdate() {
        assertTrue(updateService.isUpdate("3.0.0-RC8", release("3.0.0-RC9")));
        assertTrue(updateService.isUpdate("3.0.0-RC8", release("3.0.0-RC7")));
        assertFalse(updateService.isUpdate("3.0.0-RC8", release("3.0.0-RC8")));
        assertFalse(updateService.isUpdate("3.0.0-RC8", release(" 3.0.0-rc8 ")));
    }

    @Test
    public void testGetAssetName() {
        assertEquals("headlessmc.jar", updateService.getAssetName(Installation.Type.JAR, OS.Type.WINDOWS, CPU.X64));
        assertEquals("headlessmc-launcher-linux-x64", updateService.getAssetName(Installation.Type.NATIVE, OS.Type.LINUX, CPU.X64));
        assertEquals("headlessmc-launcher-linux-arm64", updateService.getAssetName(Installation.Type.NATIVE, OS.Type.LINUX, CPU.AARCH64));
        assertEquals("headlessmc-launcher-macos-arm64", updateService.getAssetName(Installation.Type.NATIVE, OS.Type.MACOS, CPU.AARCH64));
        assertEquals("headlessmc-launcher-macos-x64", updateService.getAssetName(Installation.Type.NATIVE, OS.Type.MACOS, CPU.X64));
        assertEquals("headlessmc-launcher-windows-x64.exe", updateService.getAssetName(Installation.Type.NATIVE, OS.Type.WINDOWS, CPU.X64));
        assertNull(updateService.getAssetName(Installation.Type.NATIVE, OS.Type.FREE_BSD, CPU.X64));
        assertNull(updateService.getAssetName(Installation.Type.NATIVE, OS.Type.LINUX, CPU.X32));
        assertNull(updateService.getAssetName(Installation.Type.UNKNOWN, OS.Type.LINUX, CPU.X64));
    }

    private static GitHubRelease release(String tag) {
        return new GitHubRelease(tag, "https://github.com/headlesshq/headlessmc/releases/tag/" + tag, List.of());
    }

}
