package io.github.headlesshq.headlessmc.patcher.lwjgl.patched;

import io.github.headlesshq.headlessmc.os.OS;
import io.github.headlesshq.headlessmc.os.OSService;
import io.github.headlesshq.headlessmc.patcher.PatchContext;
import io.github.headlesshq.headlessmc.patcher.PatchException;
import io.github.headlesshq.headlessmc.patcher.Patcher;
import io.quarkus.runtime.annotations.RegisterResources;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Replaces the LWJGL jars on the classpath with the patched, headless jars of
 * <a href="https://github.com/headlesshq/headlessmc-lwjgl">headlessmc-lwjgl</a>, which never load native libraries,
 * and adds the input API, {@code headlessmc-lwjgl-api}, to the classpath. The jars are downloaded from the locked
 * GitHub release by {@link PatchedLwjglJars}.
 * <p>Every LWJGL 3 module jar, e.g. {@code org/lwjgl/lwjgl-glfw/3.3.3/lwjgl-glfw-3.3.3.jar}, is replaced by the
 * patched jar of the same module, which is built from the latest LWJGL 3 and contains the public API of every older
 * LWJGL 3 version Minecraft used. The LWJGL 2 jars {@code lwjgl} and {@code lwjgl_util} are replaced by the patched
 * LWJGL 2.9.4. Native jars stay on the classpath, they are never loaded.</p>
 * <p>On macOS the menu agent is added as well, because Minecraft 26.x edits the Cocoa window menu through the
 * Objective-C bridge, which does not go through LWJGL.</p>
 */
@Slf4j
@Dependent
@RequiredArgsConstructor(onConstructor_ = {@Inject})
@RegisterResources(globs = PatchedLwjglPatcher.RESOURCES + "**")
public class PatchedLwjglPatcher implements Patcher {
    static final String RESOURCES = "patcher/lwjgl-patched/";
    static final String API = "headlessmc-lwjgl-api";
    static final String JOML_NO_UNSAFE = "joml.nounsafe";
    static final String MACOS_MENU_AGENT = "headlessmc-macos-menu-agent";

    private final OSService osService;
    private final PatchedLwjglJars jars;

    @Override
    public void patch(PatchContext context) {
        // JOML would write with Unsafe at the address of the heap buffers the patched LWJGL hands out.
        context.addSystemProperty(JOML_NO_UNSAFE, "true");
        for (Path library : List.copyOf(context.getCurrentPatchResult().files())) {
            String patched = patchedJar(library);
            if (patched == null) {
                continue;
            }

            if (!jars.exists(patched)) {
                log.warn("No patched LWJGL for {}, it might load native libraries", library);
                continue;
            }

            log.info("{}: replacing {} with {}", name(), library, patched);
            context.patch(library, this, (source, destination) -> {
                copy(patched, destination, false);
                return true;
            });
        }

        try (OutputStream out = context.add(API, this); JarOutputStream jar = new JarOutputStream(out)) {
            copy(API + ".jar", jar, true);
        } catch (IOException e) {
            throw new PatchException("Failed to add " + API + " to the classpath", e);
        }

        if (OS.McType.OSX.equals(osService.getOS().type().mcType())) {
            try (InputStream in = openResource(MACOS_MENU_AGENT + ".jar"); OutputStream out = context.addAgent(MACOS_MENU_AGENT, this)) {
                in.transferTo(out);
            } catch (IOException e) {
                throw new PatchException("Failed to add the " + MACOS_MENU_AGENT, e);
            }
        }
    }

    /**
     * @param library a jar on the classpath, in the maven layout of the Minecraft libraries.
     * @return the path of the patched jar that replaces it, a key of {@link PatchedLwjglLock#jars()}, or {@code null}
     * if it is not an LWJGL jar.
     */
    static @Nullable String patchedJar(Path library) {
        Path versionDirectory = library.getParent();
        Path artifactDirectory = versionDirectory == null ? null : versionDirectory.getParent();
        Path groupDirectory = artifactDirectory == null ? null : artifactDirectory.getParent();
        if (groupDirectory == null || groupDirectory.getFileName() == null) {
            return null;
        }

        String version = versionDirectory.getFileName().toString();
        String artifact = artifactDirectory.getFileName().toString().toLowerCase(Locale.ENGLISH);
        String fileName = library.getFileName().toString();
        if (!"lwjgl".equals(groupDirectory.getFileName().toString()) || !fileName.equals(artifact + "-" + version + ".jar")) {
            return null;
        }

        if (version.startsWith("3.") && (artifact.equals("lwjgl") || artifact.startsWith("lwjgl-"))) {
            return "lwjgl3/" + artifact + ".jar";
        } else if (version.startsWith("2.") && (artifact.equals("lwjgl") || artifact.equals("lwjgl_util"))) {
            return "lwjgl2/" + artifact + ".jar";
        }

        return null;
    }

    /**
     * Copies a jar, optionally without its module descriptors. The API is only used on the classpath, and old class
     * scanners, like the one of Forge for Minecraft 1.12.2, fail to read its module descriptor.
     */
    private void copy(String patched, JarOutputStream destination, boolean stripModuleDescriptors) throws IOException {
        try (ZipInputStream in = new ZipInputStream(Files.newInputStream(jars.get(patched)))) {
            ZipEntry entry;
            while ((entry = in.getNextEntry()) != null) {
                if (stripModuleDescriptors && isModuleDescriptor(entry.getName())) {
                    continue;
                }

                destination.putNextEntry(new ZipEntry(entry.getName()));
                if (!entry.isDirectory()) {
                    in.transferTo(destination);
                }

                destination.closeEntry();
            }
        }
    }

    static boolean isModuleDescriptor(String entry) {
        return entry.equals("module-info.class") || entry.endsWith("/module-info.class");
    }

    private InputStream openResource(String jar) throws IOException {
        InputStream in = getClass().getClassLoader().getResourceAsStream(RESOURCES + jar);
        if (in == null) {
            throw new IOException("Failed to find resource " + RESOURCES + jar);
        }

        return in;
    }

    @Override
    public String name() {
        return "lwjgl";
    }

    @Override
    public long version() {
        return 1;
    }

}
