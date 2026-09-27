package io.github.headlesshq.headlessmc.lwjgl.redirections;

import io.github.headlesshq.headlessmc.lwjgl.RedirectionManagerImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.system.FunctionProvider;
import org.lwjgl.system.SharedLibrary;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SdlRedirectionsTest {
    private RedirectionManagerImpl manager;

    @BeforeEach
    public void setUp() throws Throwable {
        manager = new RedirectionManagerImpl();
        invoke("Lorg/lwjgl/sdl/SDLVideo;SDL_CreateWindow(Ljava/lang/CharSequence;IIJ)J",
            long.class, "test", LwjglConfig.SCREEN_WIDTH, LwjglConfig.SCREEN_HEIGHT, 0L);
    }

    private Object invoke(String desc, Class<?> type, Object... args)
        throws Throwable {
        return manager.invoke(null, desc, type, args);
    }

    @Test
    public void virtualWindowStateIsPerWindowAndClearedOnDestroy() throws Throwable {
        String mouse = "Lorg/lwjgl/sdl/SDLMouse;";
        String video = "Lorg/lwjgl/sdl/SDLVideo;";
        long second = (Long) invoke(video + "SDL_CreateWindow(Ljava/lang/CharSequence;IIJ)J",
            long.class, "second", 640, 480, 2L);
        assertEquals(2L, invoke(video + "SDL_GetWindowFlags(J)J", long.class, second));
        assertTrue((Boolean) invoke(video + "SDL_SetWindowFullscreen(JZ)Z", boolean.class, second, true));
        assertEquals(3L, invoke(video + "SDL_GetWindowFlags(J)J", long.class, second));
        assertTrue((Boolean) invoke(video + "SDL_SetWindowFullscreen(JZ)Z", boolean.class, second, false));
        assertEquals(2L, invoke(video + "SDL_GetWindowFlags(J)J", long.class, second));
        assertTrue((Boolean) invoke(video + "SDL_SetWindowMouseGrab(JZ)Z", boolean.class, 1L, true));
        assertTrue((Boolean) invoke(video + "SDL_GetWindowMouseGrab(J)Z", boolean.class, 1L));
        assertFalse((Boolean) invoke(video + "SDL_GetWindowMouseGrab(J)Z", boolean.class, 2L));
        Object mode = new Object();
        assertTrue((Boolean) invoke(mouse + "SDL_SetWindowRelativeMouseMode(JZ)Z",
            boolean.class, 1L, true));
        assertTrue((Boolean) invoke(mouse + "SDL_GetWindowRelativeMouseMode(J)Z", boolean.class, 1L));
        assertFalse((Boolean) invoke(mouse + "SDL_GetWindowRelativeMouseMode(J)Z", boolean.class, 2L));
        assertEquals(0x8100L, invoke(video + "SDL_GetWindowFlags(J)J", long.class, 1L));
        assertTrue((Boolean) invoke(video + "SDL_SetWindowFullscreenMode(JLorg/lwjgl/sdl/SDL_DisplayMode;)Z",
            boolean.class, 1L, mode));
        assertEquals(mode, invoke(video + "SDL_GetWindowFullscreenMode(J)Lorg/lwjgl/sdl/SDL_DisplayMode;",
            Object.class, 1L));
        invoke(video + "SDL_DestroyWindow(J)V", void.class, 1L);
        assertFalse((Boolean) invoke(video + "SDL_GetWindowMouseGrab(J)Z", boolean.class, 1L));
        assertFalse((Boolean) invoke(mouse + "SDL_GetWindowRelativeMouseMode(J)Z", boolean.class, 1L));
        assertNull(invoke(video + "SDL_GetWindowFullscreenMode(J)Lorg/lwjgl/sdl/SDL_DisplayMode;",
            Object.class, 1L));
        assertFalse((Boolean) invoke(video + "SDL_SetWindowFullscreen(JZ)Z", boolean.class, 1L, true));
        assertFalse((Boolean) invoke(video + "SDL_SetWindowMouseGrab(JZ)Z", boolean.class, 1L, true));
        assertFalse((Boolean) invoke(mouse + "SDL_SetWindowRelativeMouseMode(JZ)Z", boolean.class, 1L, true));
        assertFalse((Boolean) invoke(video + "SDL_SetWindowFullscreenMode(JLorg/lwjgl/sdl/SDL_DisplayMode;)Z",
            boolean.class, 1L, mode));
        assertEquals(0L, invoke(video + "SDL_GetWindowFlags(J)J", long.class, 1L));
    }

    @Test
    public void monitorBoundsPopulateTheRequestedRectangle() throws Throwable {
        Object rect = new Object();
        String video = "Lorg/lwjgl/sdl/SDLVideo;SDL_GetDisplayBounds(ILorg/lwjgl/sdl/SDL_Rect;)Z";
        assertTrue((Boolean) invoke(video, boolean.class, 1, rect));
        assertEquals(LwjglConfig.SCREEN_WIDTH, manager.invoke(rect, "Lorg/lwjgl/sdl/SDL_Rect;w()I", int.class));
        assertEquals(LwjglConfig.SCREEN_HEIGHT, manager.invoke(rect, "Lorg/lwjgl/sdl/SDL_Rect;h()I", int.class));
        assertEquals(0, manager.invoke(rect, "Lorg/lwjgl/sdl/SDL_Rect;x()I", int.class));
        assertEquals(rect, manager.invoke(rect, "Lorg/lwjgl/sdl/SDL_Rect;x(I)Lorg/lwjgl/sdl/SDL_Rect;", Object.class, 12));
        assertEquals(12, manager.invoke(rect, "Lorg/lwjgl/sdl/SDL_Rect;x()I", int.class));
        assertFalse((Boolean) invoke(video, boolean.class, 0, rect));
        assertEquals(0, manager.invoke(new Object(), "Lorg/lwjgl/sdl/SDL_Rect;w()I", int.class));
    }

    @Test
    public void displayEnumerationAgreesWithPrimaryDisplay() throws Throwable {
        String desc = "Lorg/lwjgl/sdl/SDLVideo;";
        IntBuffer displays = (IntBuffer) invoke(desc
            + "SDL_GetDisplays()Ljava/nio/IntBuffer;", IntBuffer.class);
        assertEquals(1, displays.remaining());
        assertEquals(invoke(desc + "SDL_GetPrimaryDisplay()I", int.class),
            displays.get());
        // Callers can advance the returned buffer without changing future reads.
        assertEquals(1, ((IntBuffer) invoke(desc
            + "SDL_GetDisplays()Ljava/nio/IntBuffer;", IntBuffer.class))
            .remaining());
    }

    @Test
    public void safeUtf8AcceptsNullAndOrdinaryUtf8RejectsIt() throws Throwable {
        assertNull(invoke("Lorg/lwjgl/system/MemoryUtil;"
            + "memUTF8Safe(Ljava/lang/CharSequence;)Ljava/nio/ByteBuffer;",
            ByteBuffer.class, (Object) null));
        assertNull(invoke("Lorg/lwjgl/system/MemoryUtil;"
            + "memUTF8Safe(Ljava/lang/CharSequence;Z)Ljava/nio/ByteBuffer;",
            ByteBuffer.class, null, false));
        assertThrows(NullPointerException.class, () -> invoke(
            "Lorg/lwjgl/system/MemoryUtil;"
                + "memUTF8(Ljava/lang/CharSequence;)Ljava/nio/ByteBuffer;",
            ByteBuffer.class, (Object) null));
        assertThrows(NullPointerException.class, () -> invoke(
            "Lorg/lwjgl/system/MemoryUtil;"
                + "memUTF8(Ljava/lang/CharSequence;Z)Ljava/nio/ByteBuffer;",
            ByteBuffer.class, null, false));
    }

    @Test
    public void windowOutputCoordinatesMayBeOmitted() throws Throwable {
        IntBuffer height = IntBuffer.allocate(1);
        assertTrue((Boolean) invoke("Lorg/lwjgl/sdl/SDLVideo;"
            + "SDL_GetWindowSize(JLjava/nio/IntBuffer;Ljava/nio/IntBuffer;)Z",
            boolean.class, 1L, null, height));
        assertEquals(LwjglConfig.SCREEN_HEIGHT, height.get(0));
    }

    @Test
    public void keyboardStateHasNoPressedKeysAndIndependentCursors()
        throws Throwable {
        String desc = "Lorg/lwjgl/sdl/SDLKeyboard;"
            + "SDL_GetKeyboardState()Ljava/nio/ByteBuffer;";
        ByteBuffer state = (ByteBuffer) invoke(desc, ByteBuffer.class);
        assertEquals(512, state.remaining());
        assertTrue(state.isReadOnly());
        while (state.hasRemaining()) {
            assertEquals(0, state.get());
        }
        ByteBuffer next = (ByteBuffer) invoke(desc, ByteBuffer.class);
        assertEquals(512, next.remaining());
        assertEquals(0, next.get(511));
    }

    @Test
    public void graphicsProvidersCanBeUsedAsSharedLibraries() throws Throwable {
        for (String owner : new String[]{"opengl/GL", "vulkan/VK"}) {
            Object provider = invoke("Lorg/lwjgl/" + owner
                + ";getFunctionProvider()Lorg/lwjgl/system/FunctionProvider;",
                FunctionProvider.class);
            assertTrue(provider instanceof SharedLibrary);
            SharedLibrary library = (SharedLibrary) provider;
            assertEquals("", library.getPath());
            assertEquals(0L, library.getFunctionAddress("unused"));
        }

        assertFalse((Boolean) invoke("Lorg/lwjgl/sdl/SDLVulkan;"
            + "SDL_Vulkan_LoadLibrary(Ljava/lang/CharSequence;)Z",
            boolean.class, ""));
    }

    @Test
    public void callocUsesNativeByteOrderAndZeroedStorage() throws Throwable {
        ByteBuffer shader = (ByteBuffer) invoke(
            "Lorg/lwjgl/system/MemoryUtil;memCalloc(I)Ljava/nio/ByteBuffer;",
            ByteBuffer.class, 4);
        assertEquals(4, shader.remaining());
        assertEquals(ByteOrder.nativeOrder(), shader.order());
        assertEquals(0, shader.getInt());
    }

    @Test
    public void utf8HonorsTerminationWithoutDroppingBytes() throws Throwable {
        String descriptor = "Lorg/lwjgl/system/MemoryUtil;"
            + "memUTF8(Ljava/lang/CharSequence;Z)Ljava/nio/ByteBuffer;";
        String input = "shader \u03bb";
        byte[] expected = input.getBytes(StandardCharsets.UTF_8);
        for (boolean terminated : new boolean[]{false, true}) {
            ByteBuffer result = (ByteBuffer) invoke(descriptor,
                ByteBuffer.class, input, terminated);
            assertEquals(expected.length + (terminated ? 1 : 0),
                result.remaining());
            for (byte value : expected) {
                assertEquals(value, result.get());
            }
            if (terminated) {
                assertEquals(0, result.get());
            }
        }
    }

    @Test
    public void glAttributesRoundTripWithoutMovingBuffer() throws Throwable {
        String set = "Lorg/lwjgl/sdl/SDLVideo;SDL_GL_SetAttribute(II)Z";
        String get = "Lorg/lwjgl/sdl/SDLVideo;"
            + "SDL_GL_GetAttribute(ILjava/nio/IntBuffer;)Z";
        assertTrue((Boolean) invoke(set, boolean.class, 17, 3));
        IntBuffer out = IntBuffer.allocate(2);
        out.position(1);
        assertTrue((Boolean) invoke(get, boolean.class, 17, out));
        assertEquals(1, out.position());
        assertEquals(3, out.get(1));
    }

    @Test
    public void windowSizesUseConfiguredDimensionsAndPreservePositions()
        throws Throwable {
        String desc = "Lorg/lwjgl/sdl/SDLVideo;"
            + "SDL_GetWindowSizeInPixels(JLjava/nio/IntBuffer;Ljava/nio/IntBuffer;)Z";
        IntBuffer width = IntBuffer.allocate(2);
        width.position(1);
        IntBuffer height = IntBuffer.allocate(2);
        height.position(1);
        assertTrue((Boolean) invoke(desc, boolean.class, 1L, width, height));
        assertEquals(1, width.position());
        assertEquals(1, height.position());
        assertEquals(LwjglConfig.SCREEN_WIDTH, width.get(1));
        assertEquals(LwjglConfig.SCREEN_HEIGHT, height.get(1));
        String video = "Lorg/lwjgl/sdl/SDLVideo;";
        String minimum = video + "SDL_GetWindowMinimumSize(JLjava/nio/IntBuffer;Ljava/nio/IntBuffer;)Z";
        assertTrue((Boolean) invoke(video + "SDL_SetWindowMinimumSize(JII)Z", boolean.class, 1L, 320, 240));
        assertTrue((Boolean) invoke(minimum, boolean.class, 1L, width, height));
        assertEquals(320, width.get(1));
        assertEquals(240, height.get(1));
        assertFalse((Boolean) invoke(video + "SDL_SetWindowMinimumSize(JII)Z", boolean.class, 1L, -1, 240));
        assertTrue((Boolean) invoke(video + "SDL_SetWindowMinimumSize(JII)Z", boolean.class, 1L, 0, 0));
        assertTrue((Boolean) invoke(minimum, boolean.class, 1L, width, height));
        assertEquals(0, width.get(1));
        assertEquals(0, height.get(1));
        assertTrue((Boolean) invoke(video + "SDL_SetWindowSize(JII)Z", boolean.class, 1L, 640, 480));
        assertTrue((Boolean) invoke(desc, boolean.class, 1L, width, height));
        assertEquals(640, width.get(1));
        assertEquals(480, height.get(1));
        assertFalse((Boolean) invoke(video + "SDL_SetWindowSize(JII)Z", boolean.class, 1L, 0, 480));
        assertTrue((Boolean) invoke(desc, boolean.class, 1L, width, height));
        assertEquals(640, width.get(1));
        assertTrue((Boolean) invoke(video + "SDL_SetWindowPosition(JII)Z", boolean.class, 1L, 25, -10));
        String position = video + "SDL_GetWindowPosition(JLjava/nio/IntBuffer;Ljava/nio/IntBuffer;)Z";
        assertTrue((Boolean) invoke(position, boolean.class, 1L, width, height));
        assertEquals(25, width.get(1));
        assertEquals(-10, height.get(1));
        assertTrue((Boolean) invoke(video + "SDL_SetWindowPosition(JII)Z", boolean.class, 1L, 0x2FFF0000, 0x1FFF0000));
        assertTrue((Boolean) invoke(position, boolean.class, 1L, width, height));
        assertEquals((LwjglConfig.SCREEN_WIDTH - 640) / 2, width.get(1));
        assertEquals(-10, height.get(1));
        invoke(video + "SDL_DestroyWindow(J)V", void.class, 1L);
        assertFalse((Boolean) invoke(desc, boolean.class, 1L, width, height));
        assertFalse((Boolean) invoke(video + "SDL_SetWindowSize(JII)Z", boolean.class, 1L, 640, 480));
        assertFalse((Boolean) invoke(minimum, boolean.class, 1L, width, height));
        assertFalse((Boolean) invoke(video + "SDL_SetWindowMinimumSize(JII)Z", boolean.class, 1L, 320, 240));
    }

}
