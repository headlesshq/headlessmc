package io.github.headlesshq.headlessmc.launcher.client;

import io.github.headlesshq.headlessmc.launcher.profile.Profile;
import io.github.headlesshq.headlessmc.os.LibCDetector;
import io.github.headlesshq.headlessmc.os.OS;
import io.github.headlesshq.headlessmc.patcher.Patcher;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.Collection;
import java.util.Map;
import java.util.function.BooleanSupplier;

/**
 * Support for launching the Minecraft client with a window on musl systems, e.g. Alpine Linux.
 * The LWJGL natives Minecraft ships are only built against glibc,
 * so they need a glibc compatibility layer like gcompat, which provides {@code /lib/ld-linux-x86-64.so.2}.
 * Even with gcompat, the jemalloc LWJGL 3.3.1 and newer bundles crashes the game while loading,
 * so LWJGL is told to use the system allocator instead.
 * The headless client does not load the LWJGL natives, so it needs neither.
 */
@Slf4j
@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class MuslClientSupport {
    /** The LWJGL system property that selects the memory allocator. */
    static final String LWJGL_ALLOCATOR = "org.lwjgl.system.allocator";

    private final OS os;
    private final LibCDetector libCDetector;

    /**
     * Logs a warning if the given profile launches the client with a window on a musl system without a glibc loader.
     *
     * @param profile the profile that is launched.
     */
    public void warnIfGlibcLoaderMissing(Profile profile) {
        if (isGlibcLoaderMissing(os.type(), profile.patchers(), libCDetector::isGlibcLoaderInstalled)) {
            log.warn(
                "Launching {} with a window on a musl system without a glibc loader."
                    + " The LWJGL natives of Minecraft are built against glibc and will probably fail to load."
                    + " Install gcompat (e.g. apk add gcompat on Alpine Linux) or launch the client headlessly.",
                profile.name()
            );
        }
    }

    /**
     * Returns the system properties the client needs on this system.
     *
     * @param profile the profile that is launched.
     * @return {@code org.lwjgl.system.allocator=system} if the profile launches the client with a window on musl,
     * otherwise nothing.
     */
    public Map<String, String> getSystemProperties(Profile profile) {
        return getSystemProperties(os.type(), profile.patchers());
    }

    @VisibleForTesting
    static Map<String, String> getSystemProperties(OS.Type type, Collection<String> patchers) {
        if (isWindowedOnMusl(type, patchers)) {
            return Map.of(LWJGL_ALLOCATOR, "system");
        }

        return Map.of();
    }

    @VisibleForTesting
    static boolean isGlibcLoaderMissing(OS.Type type, Collection<String> patchers, BooleanSupplier glibcLoaderInstalled) {
        return isWindowedOnMusl(type, patchers) && !glibcLoaderInstalled.getAsBoolean();
    }

    private static boolean isWindowedOnMusl(OS.Type type, Collection<String> patchers) {
        return OS.Type.LINUX_MUSL.equals(type)
            && !patchers.contains(Patcher.LWJGL)
            && !patchers.contains(Patcher.LWJGL_PRE);
    }

}
