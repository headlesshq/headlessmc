package io.github.headlesshq.headlessmc.lwjgl.redirections;

import io.github.headlesshq.headlessmc.lwjgl.api.RedirectionManager;

import java.lang.reflect.Proxy;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

/** Provider types and shader storage for the no-render graphics path. */
public final class GraphicsProviderRedirections {
    private GraphicsProviderRedirections() {
    }

    public static void register(RedirectionManager manager) {
        for (String owner : new String[]{"opengl/GL", "vulkan/VK"}) {
            manager.redirect("Lorg/lwjgl/" + owner
                                 + ";getFunctionProvider()Lorg/lwjgl/system/FunctionProvider;",
                             (obj, desc, type, args) -> libraryProxy(manager, type));
        }

        // RenderPearl consumes shader bytes before reaching the redirected GL
        // calls. No native compiler receives them in headless mode; returning
        // an empty buffer avoids inventing a SPIR-V module. SPVC resource
        // reflection follows the existing empty-array redirections.
        AtomicBoolean reportedShaderSkip = new AtomicBoolean();
        manager.redirect("Lorg/lwjgl/util/shaderc/Shaderc;"
                             + "shaderc_result_get_bytes(J)Ljava/nio/ByteBuffer;",
                         (obj, desc, type, args) -> {
                             if (reportedShaderSkip.compareAndSet(false, true)) {
                                 System.out.println("[HeadlessMC] No-render mode skips shader compilation and reflection; shader errors are not checked.");
                             }
                             return ByteBuffer.allocate(0);
                         });
    }

    private static Object libraryProxy(RedirectionManager manager, Class<?> type)
        throws ClassNotFoundException {
        // RenderPearl treats the provider as a library. Resolve this interface
        // through LWJGL's loader rather than adding a runtime LWJGL dependency.
        Class<?> library = Class.forName("org.lwjgl.system.SharedLibrary",
                                         false, type.getClassLoader());
        return Proxy.newProxyInstance(type.getClassLoader(),
                                      new Class<?>[]{library},
                                      new ProxyRedirection(manager, "Lorg/lwjgl/system/SharedLibrary;"));
    }
}
