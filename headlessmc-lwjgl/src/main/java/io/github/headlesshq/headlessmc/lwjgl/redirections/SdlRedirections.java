package io.github.headlesshq.headlessmc.lwjgl.redirections;

import io.github.headlesshq.headlessmc.lwjgl.api.RedirectionManager;

import java.lang.reflect.Proxy;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import static io.github.headlesshq.headlessmc.lwjgl.api.Redirection.of;

/** SDL and RenderPearl redirections used by Minecraft 26.3. */
public final class SdlRedirections {
    private SdlRedirections() {
    }

    public static void register(RedirectionManager manager) {
        AtomicLong handles = new AtomicLong(1);
        Map<Integer, Integer> glAttributes = new ConcurrentHashMap<>();
        long start = System.nanoTime();
        manager.redirect("Lorg/lwjgl/sdl/SDLInit;SDL_Init(I)Z", of(true));
        manager.redirect("Lorg/lwjgl/sdl/SDLTimer;SDL_GetTicksNS()J",
            (obj, desc, type, args) -> System.nanoTime() - start);
        manager.redirect("Lorg/lwjgl/sdl/SDLTimer;SDL_GetTicks()J",
            (obj, desc, type, args) -> (System.nanoTime() - start) / 1000000L);
        manager.redirect("Lorg/lwjgl/system/MemoryStack;" +
            "callocInt(I)Ljava/nio/IntBuffer;",
            (obj, desc, type, args) -> IntBuffer.allocate((int) args[0]));
        manager.redirect("Lorg/lwjgl/system/MemoryStack;" +
            "callocFloat(I)Ljava/nio/FloatBuffer;",
            (obj, desc, type, args) -> FloatBuffer.allocate((int) args[0]));
        manager.redirect("Lorg/lwjgl/system/MemoryStack;" +
            "mallocFloat(I)Ljava/nio/FloatBuffer;",
            (obj, desc, type, args) -> FloatBuffer.allocate((int) args[0]));
        manager.redirect("Lorg/lwjgl/system/MemoryUtil;" +
            "memCalloc(I)Ljava/nio/ByteBuffer;",
            (obj, desc, type, args) -> ByteBuffer.allocate((int) args[0]));
        manager.redirect("Lorg/lwjgl/system/MemoryUtil;" +
            "memUTF8(Ljava/lang/CharSequence;Z)Ljava/nio/ByteBuffer;",
            (obj, desc, type, args) -> utf8(args[0], (boolean) args[1]));
        manager.redirect("Lorg/lwjgl/system/MemoryUtil;" +
            "memUTF8(Ljava/lang/CharSequence;)Ljava/nio/ByteBuffer;",
            (obj, desc, type, args) -> utf8(args[0], true));
        // RenderPearl asks shaderc for a module before calling OpenGL.
        // Drawing and SPVC reflection are stubbed, so no instructions are
        // needed here. This buffer is not valid SPIR-V for a native compiler.
        manager.redirect("Lorg/lwjgl/util/shaderc/Shaderc;" +
            "shaderc_result_get_bytes(J)Ljava/nio/ByteBuffer;",
            (obj, desc, type, args) -> ByteBuffer.allocate(0));
        manager.redirect("Lorg/lwjgl/sdl/SDLVideo;" +
            "SDL_GetDisplays()Ljava/nio/IntBuffer;", of(null));
        manager.redirect("Lorg/lwjgl/opengl/GL;" +
            "getFunctionProvider()Lorg/lwjgl/system/FunctionProvider;",
            (obj, desc, type, args) -> libraryProxy(manager, type));
        // A fresh 26.3 client probes Vulkan before choosing a backend. Supply
        // the right provider type, but report Vulkan unavailable so it can
        // fall back to the headless OpenGL path.
        manager.redirect("Lorg/lwjgl/vulkan/VK;" +
            "getFunctionProvider()Lorg/lwjgl/system/FunctionProvider;",
            (obj, desc, type, args) -> libraryProxy(manager, type));
        manager.redirect("Lorg/lwjgl/sdl/SDLVulkan;" +
            "SDL_Vulkan_LoadLibrary(Ljava/lang/CharSequence;)Z", of(false));
        manager.redirect("Lorg/lwjgl/sdl/SDLVideo;" +
            "SDL_GL_LoadLibrary(Ljava/lang/CharSequence;)Z", of(true));
        manager.redirect("Lorg/lwjgl/sdl/SDLVideo;SDL_GL_SetAttribute(II)Z",
            (obj, desc, type, args) -> {
                glAttributes.put((int) args[0], (int) args[1]);
                return true;
            });
        manager.redirect("Lorg/lwjgl/sdl/SDLVideo;" +
            "SDL_GL_GetAttribute(ILjava/nio/IntBuffer;)Z",
            (obj, desc, type, args) -> {
                IntBuffer buffer = (IntBuffer) args[1];
                buffer.put(buffer.position(),
                    glAttributes.getOrDefault((int) args[0], 0));
                return true;
            });
        manager.redirect("Lorg/lwjgl/sdl/SDLVideo;" +
            "SDL_CreateWindow(Ljava/lang/CharSequence;IIJ)J",
            (obj, desc, type, args) -> handles.getAndIncrement());
        manager.redirect("Lorg/lwjgl/sdl/SDLVideo;SDL_GL_CreateContext(J)J",
            (obj, desc, type, args) -> handles.getAndIncrement());
        manager.redirect("Lorg/lwjgl/sdl/SDLVideo;" +
            "SDL_GL_MakeCurrent(JJ)Z", of(true));
        manager.redirect("Lorg/lwjgl/sdl/SDLVideo;" +
            "SDL_GetPrimaryDisplay()I", of(1));
        manager.redirect("Lorg/lwjgl/sdl/SDLVideo;" +
            "SDL_GetDisplayForWindow(J)I", of(1));
        manager.redirect("Lorg/lwjgl/sdl/SDLVideo;" +
            "SDL_GetWindowPixelDensity(J)F", of(1.0f));
        manager.redirect("Lorg/lwjgl/sdl/SDLVideo;" +
            "SDL_GetWindowPosition(JLjava/nio/IntBuffer;Ljava/nio/IntBuffer;)Z",
            (obj, desc, type, args) -> writePair(args, 0, 0));
        manager.redirect("Lorg/lwjgl/sdl/SDLVideo;" +
            "SDL_GetWindowSizeInPixels(JLjava/nio/IntBuffer;Ljava/nio/IntBuffer;)Z",
            (obj, desc, type, args) -> writePair(args,
                LwjglConfig.SCREEN_WIDTH, LwjglConfig.SCREEN_HEIGHT));
        manager.redirect("Lorg/lwjgl/sdl/SDLVideo;" +
            "SDL_GetWindowSize(JLjava/nio/IntBuffer;Ljava/nio/IntBuffer;)Z",
            (obj, desc, type, args) -> writePair(args,
                LwjglConfig.SCREEN_WIDTH, LwjglConfig.SCREEN_HEIGHT));
        manager.redirect("Lorg/lwjgl/sdl/SDLVideo;" +
            "SDL_SetWindowMinimumSize(JII)Z", of(true));
        manager.redirect("Lorg/lwjgl/sdl/SDLVideo;" +
            "SDL_SetWindowFullscreen(JZ)Z", of(true));
        manager.redirect("Lorg/lwjgl/sdl/SDLVideo;SDL_GL_SwapWindow(J)Z",
            (obj, desc, type, args) -> {
                Thread.sleep(10L);
                return true;
            });
    }

    private static boolean writePair(Object[] args, int x, int y) {
        IntBuffer first = (IntBuffer) args[1];
        IntBuffer second = (IntBuffer) args[2];
        if (first != null) {
            first.put(first.position(), x);
        }
        if (second != null) {
            second.put(second.position(), y);
        }
        return true;
    }

    private static ByteBuffer utf8(Object input, boolean terminated) {
        byte[] bytes = input.toString().getBytes(StandardCharsets.UTF_8);
        ByteBuffer result = ByteBuffer.allocate(
            bytes.length + (terminated ? 1 : 0));
        result.put(bytes);
        result.position(0);
        return result;
    }

    private static Object libraryProxy(RedirectionManager manager, Class<?> type)
        throws ClassNotFoundException {
        Class<?> library = Class.forName("org.lwjgl.system.SharedLibrary",
            false, type.getClassLoader());
        return Proxy.newProxyInstance(type.getClassLoader(),
            new Class<?>[]{library},
            new ProxyRedirection(manager, "Lorg/lwjgl/system/SharedLibrary;"));
    }
}
