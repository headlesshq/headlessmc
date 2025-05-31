package io.github.headlesshq.headlessmc.launcher.specifics;

/**
 * Common version specific mods we use.
 *
 * @see <a href=https://github.com/3arthqu4ke/hmc-specifics>https://github.com/3arthqu4ke/hmc-specifics</a>
 * @see <a href=https://github.com/headlesshq/mc-runtime-test>https://github.com/headlesshq/mc-runtime-test</a>
 * @see <a href=https://github.com/3arthqu4ke/hmc-optimizations>https://github.com/3arthqu4ke/hmc-optimizations</a>
 */
public class VersionSpecificMods {
    /**
     * Version specific implementations of the HeadlessMc runtime.
     * @see <a href=https://github.com/3arthqu4ke/hmc-specifics>https://github.com/3arthqu4ke/hmc-specifics</a>
     */
    public static final VersionSpecificModRepository HMC_SPECIFICS =
        new VersionSpecificModRepository(URI.create("https://github.com/3arthqu4ke/hmc-specifics/releases/download/"), "hmc-specifics", "2.2.1", "-release");

    /**
     * Mods that join a SinglePlayer world, run GameTests and exit the game, for testing purposes.
     * @see <a href=https://github.com/headlesshq/mc-runtime-test>https://github.com/headlesshq/mc-runtime-test</a>
     */
    public static final VersionSpecificModRepository MC_RUNTIME_TEST =
        new VersionSpecificModRepository(URI.create("https://github.com/headlesshq/mc-runtime-test/releases/download/"), "mc-runtime-test", "3.1.1", "-release");

    /**
     * Version specific optimizations for HeadlessMc. In addition to HeadlessMc's lwjgl patching it patches out all rendering related code.
     * @see <a href=https://github.com/3arthqu4ke/hmc-optimizations>https://github.com/3arthqu4ke/hmc-optimizations</a>
     */
    public static final VersionSpecificModRepository HMC_OPTIMIZATIONS =
        new VersionSpecificModRepository(URI.create("https://github.com/3arthqu4ke/hmc-optimizations/releases/download/"), "hmc-optimizations", "0.4.0", "");

}
