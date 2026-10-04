package io.github.headlesshq.headlessmc.patcher.lwjgl;

import io.github.headlesshq.headlessmc.lwjgl.transformer.LwjglTransformer;
import io.github.headlesshq.headlessmc.os.OS;
import io.github.headlesshq.headlessmc.os.OSService;
import io.github.headlesshq.headlessmc.patcher.PatchContext;
import io.github.headlesshq.headlessmc.patcher.PatchException;
import io.github.headlesshq.headlessmc.patcher.asm.AbstractClassPatcher;
import io.quarkus.runtime.annotations.RegisterResources;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;

@Dependent
@RegisterResources(
    globs = {
        LwjglPatcher.RESOURCE_NAME,
        LwjglPatcher.MAC_OS_RESOURCE_NAME
    }
)
@RequiredArgsConstructor
public class LwjglPatcher extends AbstractClassPatcher {
    static final String LWJGL_JAR = "headlessmc-lwjgl.jar";
    static final String MACOS_MENU_AGENT_JAR = "headlessmc-macos-menu-agent.jar";
    static final String RESOURCE_NAME = "patcher/lwjgl/" + LWJGL_JAR;
    static final String MAC_OS_RESOURCE_NAME = "patcher/lwjgl/" + MACOS_MENU_AGENT_JAR;

    private final LwjglTransformer transformer;
    private final OSService osService;

    @Inject
    public LwjglPatcher(OSService osService) {
        this(new LwjglTransformer(), osService);
    }

    @Override
    public void patch(PatchContext context) {
        super.patch(context);
        try (
            InputStream inputStream = Objects.requireNonNull(
                getClass().getClassLoader().getResourceAsStream(RESOURCE_NAME),
                "Failed to find resource " + RESOURCE_NAME
            );
            OutputStream outputStream = context.add("headlessmc-lwjgl", this)) {
            inputStream.transferTo(outputStream);
        } catch (IOException e) {
            throw new PatchException("Failed to add " + LWJGL_JAR + " to classpath", e);
        }

        if (OS.McType.OSX.equals(osService.getOS().type().mcType())) {
            try (
                InputStream inputStream = Objects.requireNonNull(
                    getClass().getClassLoader().getResourceAsStream(MAC_OS_RESOURCE_NAME),
                    "Failed to find resource " + MAC_OS_RESOURCE_NAME
                );
                OutputStream outputStream = context.addAgent("headlessmc-macos-menu-agent", this)) {
                inputStream.transferTo(outputStream);
            } catch (IOException e) {
                throw new PatchException("Failed to add " + MACOS_MENU_AGENT_JAR + " agent", e);
            }
        }
    }

    @Override
    public void patch(ClassNode classNode) {
        transformer.transform(classNode);
    }

    @Override
    public String name() {
        return "lwjgl";
    }

    @Override
    public long version() {
        return 0;
    }

    @Override
    public boolean matches(String entry) {
        String name = entry.toLowerCase(Locale.ENGLISH);
        return name.contains("lwjgl") && name.endsWith(".class")
            || name.endsWith("module-info.class");
    }

    @Override
    protected boolean shouldPatch(Path library) {
        String path = library.toString().toLowerCase(Locale.ENGLISH);
        return path.contains("lwjgl") && !path.endsWith(LWJGL_JAR);
    }

}
