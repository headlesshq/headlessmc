package io.github.headlesshq.headlessmc.lwjgl.agent;

import com.mojang.blaze3d.platform.MacosUtil;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

public class MacosMenuAgentTest {
    @Test
    public void testMenuSetupWithoutNativeWindow() throws Exception {
        byte[] original;
        try (InputStream stream = MacosUtil.class.getResourceAsStream("MacosUtil.class");
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            assertNotNull(stream);
            byte[] buffer = new byte[1024];
            int read;
            while ((read = stream.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
            original = output.toByteArray();
        }

        byte[] transformed = new MacosMenuAgent().transform(null,
            MacosUtil.class.getName().replace('.', '/'), null, null, original);
        assertNotNull(transformed);
        Class<?> type = new ClassLoader(null) {
            Class<?> define() {
                return defineClass(MacosUtil.class.getName(), transformed, 0, transformed.length);
            }
        }.define();
        assertDoesNotThrow(() -> type.getMethod("disableCloseWindowMenuItem").invoke(null));
        assertEquals(42, type.getMethod("unrelatedMethod").invoke(null));
        assertThrows(IllegalStateException.class, MacosUtil::disableCloseWindowMenuItem);

        MacosMenuAgent wrapperAgent = new MacosMenuAgent(true);
        String name = MacosUtil.class.getName().replace('.', '/');
        assertNull(wrapperAgent.transform(new ClassLoader(null) {}, name, null, null, original));
        ClassLoader virtualWindowLoader = new ClassLoader(null) {
            @Override
            public java.net.URL getResource(String resource) {
                return resource.endsWith("/SdlRedirections.class")
                    ? MacosUtil.class.getResource("MacosUtil.class") : null;
            }
        };
        assertNotNull(wrapperAgent.transform(virtualWindowLoader, name, null, null, original));
    }

    @Test
    public void testOlderClassAndOtherLibrariesAreUnchanged() {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC,
            "com/mojang/blaze3d/platform/MacosUtil", null, "java/lang/Object", null);
        writer.visitEnd();
        MacosMenuAgent agent = new MacosMenuAgent();
        assertNull(agent.transform(null, "com/mojang/blaze3d/platform/MacosUtil",
            null, null, writer.toByteArray()));
        assertNull(agent.transform(null, "org/lwjgl/system/MemoryUtil",
            null, null, new byte[0]));
    }
}
