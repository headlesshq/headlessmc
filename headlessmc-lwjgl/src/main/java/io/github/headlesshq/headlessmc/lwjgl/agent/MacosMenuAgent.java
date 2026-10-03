package io.github.headlesshq.headlessmc.lwjgl.agent;

import io.github.headlesshq.headlessmc.lwjgl.transformer.MacosMenuTransformer;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

/** Skips native window-menu setup for macOS no-render launches. */
public class MacosMenuAgent implements ClassFileTransformer {
    private final boolean virtualWindowsOnly;

    public MacosMenuAgent() {
        this(false);
    }

    MacosMenuAgent(boolean virtualWindowsOnly) {
        this.virtualWindowsOnly = virtualWindowsOnly;
    }

    public static void premain(String args, Instrumentation instrumentation) {
        instrumentation.addTransformer(new MacosMenuAgent());
    }

    public static void agentmain(String args, Instrumentation instrumentation) {
        if (System.getProperty("os.name", "").startsWith("Mac")) {
            // Executable jars can install the hook before an in-memory launch.
            // The wrapper also launches graphical clients, so check their loader.
            instrumentation.addTransformer(new MacosMenuAgent(true));
        }
    }

    @Override
    public byte[] transform(ClassLoader loader, String name, Class<?> type,
                            ProtectionDomain domain, byte[] bytes) {
        if (virtualWindowsOnly
            && "com/mojang/blaze3d/platform/MacosUtil".equals(name)
            && (loader == null || loader.getResource(
                "io/github/headlesshq/headlessmc/lwjgl/redirections/SdlRedirections.class") == null)) {
            return null;
        }

        return MacosMenuTransformer.transform(name, bytes);
    }
}
