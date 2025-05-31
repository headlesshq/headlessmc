package io.github.headlesshq.headlessmc.lwjgl.launchwrapper;

import io.github.headlesshq.headlessmc.lwjgl.LwjglProperties;
import io.github.headlesshq.headlessmc.lwjgl.testlaunchwrapper.LaunchWrapperTarget;
import lombok.SneakyThrows;
import lombok.val;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LaunchWrapperTest {
    public static String PASSED = "hmc.launchwrapper.test.passed";

    private static int getJavaVersion() {
        return parseVersion(System.getProperty("java.version"));
    }

    private static int parseVersion(String versionIn) {
        String version = versionIn;
        if (version.startsWith("1.")) {
            version = version.substring(2, 3);
        } else {
            int dot = version.indexOf(".");
            if (dot != -1) {
                version = version.substring(0, dot);
            }
        }

        return Integer.parseInt(version);
    }

    @Test
    public void testParseJavaVersion() {
        assertEquals(8, parseVersion("1.8.0_241"));
        assertEquals(17, parseVersion("17.0.2"));
        assertEquals(8, parseVersion("1.8.0_331"));
        assertEquals(18, parseVersion("18.0.1.1"));
    }

    @Test
    @SneakyThrows
    public void testLaunchWrapperTransformation() {
        // LaunchWrapper is not compatible with Java > 8.
        // The main problem is of course that Launch needs an URLClassloader.
        // While we can fix that with our own URLClassloader, the
        // LaunchClassloader also will not set that URLClassloader as
        // its parent, causing exclusions to get loaded through the bootloader.
        // We could actually run this test successfully, by loading the Launch
        // class with the URLClassloader, but then the Launch class will also
        // get loaded by the bootloader for the called main class, causing two
        // Launch classes to exist, the one for the mainClass with
        // Launch.classLoader set to null. Since that is not the normal
        // behaviour and I have some assertions checking if classes have been
        // loaded by Launch.classLoader I decided to skip this test on Java 9+.
        int jv = getJavaVersion();
        Assumptions.assumeFalse(jv > 8, jv + " > 8, skipping LaunchWrapper");

        System.setProperty(LwjglProperties.TWEAKER_MAIN_CLASS,
                           LaunchWrapperTarget.class.getName());

        System.setProperty("legacy.debugClassLoading", "true");
        System.setProperty("legacy.debugClassLoadingFiner", "true");
        System.setProperty("formatMsgNoLookups", "true");

        val cl = new LaunchWrapperClassloader();
        val launch = cl.loadClass("net.minecraft.launchwrapper.Launch");

        System.setProperty(PASSED, "false");
        assertEquals("false", System.getProperty(PASSED));

        val main = launch.getDeclaredMethod("main", String[].class);
        main.setAccessible(true);
        main.invoke(null, (Object) new String[]{
            "--version", "1.0.0", "--gameDir",
            new File("build").getAbsolutePath(),
            "--assetsDir", new File("build").getAbsolutePath(),
            "--tweakClass", //LwjglTweaker.class.getName()});
            "io.github.headlesshq.headlessmc.lwjgl.launchwrapper.LwjglTweaker"});

        assertEquals("true", System.getProperty(PASSED));
        System.setProperty(PASSED, "false");
    }

}
