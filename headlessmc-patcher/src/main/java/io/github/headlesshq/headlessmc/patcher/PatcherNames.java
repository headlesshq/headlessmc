package io.github.headlesshq.headlessmc.patcher;

/**
 * The {@link Patcher#name() names} of the patchers HeadlessMc ships with, for modules that select patchers by name
 * without depending on the patcher modules.
 */
public final class PatcherNames {
    /**
     * Patches Log4J against Log4Shell.
     */
    public static final String LOG4J = "log4j";
    /**
     * Turns LWJGL into stubs with bytecode transformations.
     */
    public static final String LWJGL = "lwjgl";
    /**
     * Replaces LWJGL with the patched jars of headlessmc-lwjgl, opt-in while in preview, replaces {@link #LWJGL}.
     */
    public static final String LWJGL_PRE = "lwjgl-pre";
    /**
     * Patches the paulscode sound system of old Minecraft versions.
     */
    public static final String PAULSCODE = "paulscode";

    private PatcherNames() {
        throw new AssertionError();
    }

}
