package io.github.headlesshq.headlessmc.lwjgl.redirections;

import io.github.headlesshq.headlessmc.lwjgl.api.RedirectionManager;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Map;
import java.util.Collections;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import static io.github.headlesshq.headlessmc.lwjgl.api.Redirection.of;

/** Window and timer redirections for the SDL backend introduced in 26.3. */
public final class SdlRedirections {
    // SDL3 window flags; keep this module independent of the native SDL jar.
    private static final long FULLSCREEN = 0x1L;
    private static final long MOUSE_GRABBED = 0x100L;
    private static final long MOUSE_RELATIVE_MODE = 0x8000L;

    private SdlRedirections() {
    }

    public static void register(RedirectionManager manager) {
        AtomicLong handles = new AtomicLong(1);
        Map<Integer, Integer> glAttributes = new ConcurrentHashMap<>();
        Map<Long, WindowState> windows = new ConcurrentHashMap<>();
        Map<Object, int[]> rectangles = Collections.synchronizedMap(new WeakHashMap<>());
        long start = System.nanoTime();
        DisplayUpdater updater = new DisplayUpdater();
        // SDL_SCANCODE_COUNT is 512. Headless clients have no physical key
        // input, but callers still need a valid array indexed by scancode.
        ByteBuffer keyboardState = ByteBuffer.allocate(512).asReadOnlyBuffer();
        manager.redirect("Lorg/lwjgl/sdl/SDLKeyboard;"
                             + "SDL_GetKeyboardState()Ljava/nio/ByteBuffer;",
                         (obj, desc, type, args) -> keyboardState.duplicate());
        manager.redirect("Lorg/lwjgl/sdl/SDLInit;SDL_Init(I)Z", of(true));
        manager.redirect("Lorg/lwjgl/sdl/SDLTimer;SDL_GetTicksNS()J",
                         (obj, desc, type, args) -> System.nanoTime() - start);
        manager.redirect("Lorg/lwjgl/sdl/SDLTimer;SDL_GetTicks()J",
                         (obj, desc, type, args) -> (System.nanoTime() - start) / 1000000L);
        manager.redirect("Lorg/lwjgl/sdl/SDLVideo;" +
                             "SDL_GetDisplays()Ljava/nio/IntBuffer;",
                         (obj, desc, type, args) -> IntBuffer.wrap(new int[]{1}));
        // Keep RenderPearl on the OpenGL path, which our redirections handle.
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
                         (obj, desc, type, args) -> {
                             long handle = handles.getAndIncrement();
                             windows.put(handle, new WindowState((long) args[3]));
                             return handle;
                         });
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
                         (obj, desc, type, args) -> {
                             WindowState window = windows.get((long) args[0]);
                             return window != null && writePair(args, window.position);
                         });
        manager.redirect("Lorg/lwjgl/sdl/SDLVideo;" +
                             "SDL_GetWindowSizeInPixels(JLjava/nio/IntBuffer;Ljava/nio/IntBuffer;)Z",
                         (obj, desc, type, args) -> {
                             WindowState window = windows.get((long) args[0]);
                             return window != null && writePair(args, window.size);
                         });
        manager.redirect("Lorg/lwjgl/sdl/SDLVideo;" +
                             "SDL_GetWindowSize(JLjava/nio/IntBuffer;Ljava/nio/IntBuffer;)Z",
                         (obj, desc, type, args) -> {
                             WindowState window = windows.get((long) args[0]);
                             return window != null && writePair(args, window.size);
                         });
        manager.redirect("Lorg/lwjgl/sdl/SDLVideo;" +
                             "SDL_SetWindowMinimumSize(JII)Z", (obj, desc, type, args) -> {
            WindowState window = windows.get((long) args[0]);
            int width = (int) args[1];
            int height = (int) args[2];
            if (window == null || width < 0 || height < 0) {
                return false;
            }
            window.minimumSize = new int[]{width, height};
            return true;
        });
        manager.redirect("Lorg/lwjgl/sdl/SDLVideo;"
                             + "SDL_GetWindowMinimumSize(JLjava/nio/IntBuffer;Ljava/nio/IntBuffer;)Z",
                         (obj, desc, type, args) -> {
                             WindowState window = windows.get((long) args[0]);
                             return window != null && writePair(args, window.minimumSize);
                         });
        manager.redirect("Lorg/lwjgl/sdl/SDLVideo;" +
                             "SDL_SetWindowFullscreen(JZ)Z",
                         (obj, desc, type, args) -> setFlag(windows.get((long) args[0]), FULLSCREEN, (boolean) args[1]));
        manager.redirect("Lorg/lwjgl/sdl/SDLVideo;SDL_GetWindowFlags(J)J",
                         (obj, desc, type, args) -> {
                             WindowState window = windows.get((long) args[0]);
                             return window == null ? 0L : window.flags;
                         });
        // A virtual window can accept these requests without a window manager.
        for (String method : new String[]{"SDL_RestoreWindow(J)Z", "SDL_SyncWindow(J)Z",
            "SDL_SetWindowIcon(JLorg/lwjgl/sdl/SDL_Surface;)Z"}) {
            manager.redirect("Lorg/lwjgl/sdl/SDLVideo;" + method,
                             (obj, desc, type, args) -> windows.containsKey((long) args[0]));
        }
        manager.redirect("Lorg/lwjgl/sdl/SDLVideo;SDL_SetWindowSize(JII)Z",
                         (obj, desc, type, args) -> {
                             WindowState window = windows.get((long) args[0]);
                             int width = (int) args[1];
                             int height = (int) args[2];
                             if (window == null || width <= 0 || height <= 0) {
                                 return false;
                             }
                             window.size = new int[]{width, height};
                             return true;
                         });
        manager.redirect("Lorg/lwjgl/sdl/SDLVideo;SDL_SetWindowPosition(JII)Z",
                         (obj, desc, type, args) -> {
                             WindowState window = windows.get((long) args[0]);
                             if (window == null) {
                                 return false;
                             }
                             int[] previous = window.position;
                             int[] size = window.size;
                             window.position = new int[]{
                                 position((int) args[1], previous[0], LwjglConfig.SCREEN_WIDTH, size[0]),
                                 position((int) args[2], previous[1], LwjglConfig.SCREEN_HEIGHT, size[1])};
                             return true;
                         });
        manager.redirect("Lorg/lwjgl/sdl/SDLSurface;SDL_AddSurfaceAlternateImage("
                             + "Lorg/lwjgl/sdl/SDL_Surface;Lorg/lwjgl/sdl/SDL_Surface;)Z", of(true));
        manager.redirect("Lorg/lwjgl/sdl/SDLVideo;SDL_SetWindowFullscreenMode("
                             + "JLorg/lwjgl/sdl/SDL_DisplayMode;)Z", (obj, desc, type, args) -> {
            WindowState window = windows.get((long) args[0]);
            if (window == null) {
                return false;
            }
            window.fullscreenMode = args[1];
            return true;
        });
        manager.redirect("Lorg/lwjgl/sdl/SDLVideo;SDL_GetWindowFullscreenMode("
                             + "J)Lorg/lwjgl/sdl/SDL_DisplayMode;",
                         (obj, desc, type, args) -> {
                             WindowState window = windows.get((long) args[0]);
                             return window == null ? null : window.fullscreenMode;
                         });
        manager.redirect("Lorg/lwjgl/sdl/SDLMouse;SDL_SetWindowRelativeMouseMode(JZ)Z",
                         (obj, desc, type, args) -> setFlag(windows.get((long) args[0]), MOUSE_RELATIVE_MODE, (boolean) args[1]));
        manager.redirect("Lorg/lwjgl/sdl/SDLMouse;SDL_GetWindowRelativeMouseMode(J)Z",
                         (obj, desc, type, args) -> hasFlag(windows.get((long) args[0]), MOUSE_RELATIVE_MODE));
        manager.redirect("Lorg/lwjgl/sdl/SDLVideo;SDL_SetWindowMouseGrab(JZ)Z",
                         (obj, desc, type, args) -> setFlag(windows.get((long) args[0]), MOUSE_GRABBED, (boolean) args[1]));
        manager.redirect("Lorg/lwjgl/sdl/SDLVideo;SDL_GetWindowMouseGrab(J)Z",
                         (obj, desc, type, args) -> hasFlag(windows.get((long) args[0]), MOUSE_GRABBED));
        manager.redirect("Lorg/lwjgl/sdl/SDLVideo;SDL_DestroyWindow(J)V",
                         (obj, desc, type, args) -> {
                             windows.remove((long) args[0]);
                             return null;
                         });
        for (String method : new String[]{"SDL_GetDisplayBounds", "SDL_GetDisplayUsableBounds"}) {
            manager.redirect("Lorg/lwjgl/sdl/SDLVideo;" + method
                                 + "(ILorg/lwjgl/sdl/SDL_Rect;)Z", (obj, desc, type, args) -> {
                if ((int) args[0] != 1 || args[1] == null) {
                    return false;
                }
                rectangles.put(args[1], new int[]{0, 0,
                    LwjglConfig.SCREEN_WIDTH, LwjglConfig.SCREEN_HEIGHT});
                return true;
            });
        }
        String[] fields = {"x", "y", "w", "h"};
        for (int i = 0; i < fields.length; i++) {
            final int index = i;
            manager.redirect("Lorg/lwjgl/sdl/SDL_Rect;" + fields[i] + "()I",
                             (obj, desc, type, args) -> rectangles.getOrDefault(obj, new int[4])[index]);
            manager.redirect("Lorg/lwjgl/sdl/SDL_Rect;" + fields[i]
                                 + "(I)Lorg/lwjgl/sdl/SDL_Rect;", (obj, desc, type, args) -> {
                rectangles.computeIfAbsent(obj, key -> new int[4])[index] = (int) args[0];
                return obj;
            });
        }
        manager.redirect("Lorg/lwjgl/sdl/SDL_DisplayMode;w()I", of(LwjglConfig.SCREEN_WIDTH));
        manager.redirect("Lorg/lwjgl/sdl/SDL_DisplayMode;h()I", of(LwjglConfig.SCREEN_HEIGHT));
        manager.redirect("Lorg/lwjgl/sdl/SDL_DisplayMode;refresh_rate()F", of((float) LwjglConfig.REFRESH_RATE));
        for (String field : new String[]{"Rbits", "Gbits", "Bbits"}) {
            manager.redirect("Lorg/lwjgl/sdl/SDL_PixelFormatDetails;" + field + "()B", of((byte) 8));
        }
        manager.redirect("Lorg/lwjgl/sdl/SDLVideo;SDL_GL_SwapWindow(J)Z",
                         (obj, desc, type, args) -> {
                             updater.invoke(obj, desc, type, args);
                             return true;
                         });
    }

    private static int position(int requested, int previous, int displaySize, int windowSize) {
        // Resolve SDL's centered/undefined markers against our virtual display.
        if ((requested & 0xFFFF0000) == 0x2FFF0000) {
            return (displaySize - windowSize) / 2;
        }
        return (requested & 0xFFFF0000) == 0x1FFF0000 ? previous : requested;
    }

    private static boolean writePair(Object[] args, int[] pair) {
        IntBuffer first = (IntBuffer) args[1];
        IntBuffer second = (IntBuffer) args[2];
        if (first != null) {
            first.put(first.position(), pair[0]);
        }
        if (second != null) {
            second.put(second.position(), pair[1]);
        }
        return true;
    }

    private static boolean setFlag(WindowState window, long flag, boolean enabled) {
        if (window == null) {
            return false;
        }
        window.flags = enabled ? window.flags | flag : window.flags & ~flag;
        return true;
    }

    private static boolean hasFlag(WindowState window, long flag) {
        return window != null && (window.flags & flag) != 0;
    }

    private static final class WindowState {
        private int[] size = {LwjglConfig.SCREEN_WIDTH, LwjglConfig.SCREEN_HEIGHT};
        private int[] position = {0, 0};
        private int[] minimumSize = {0, 0};
        private long flags;
        private Object fullscreenMode;

        private WindowState(long flags) {
            this.flags = flags;
        }
    }

}
