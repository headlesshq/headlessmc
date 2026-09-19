package io.github.headlesshq.headlessmc.lwjgl.redirections;

import io.github.headlesshq.headlessmc.lwjgl.RedirectionManagerImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.system.FunctionProvider;
import org.lwjgl.system.SharedLibrary;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SdlRedirectionsTest {
    private RedirectionManagerImpl manager;

    @BeforeEach
    public void setUp() {
        manager = new RedirectionManagerImpl();
    }

    private Object invoke(String desc, Class<?> type, Object... args)
        throws Throwable {
        return manager.invoke(null, desc, type, args);
    }

    @Test
    public void initReportsSuccess() throws Throwable {
        assertTrue((Boolean) invoke(
            "Lorg/lwjgl/sdl/SDLInit;SDL_Init(I)Z", boolean.class, 0));
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
    public void callocIntIsZeroed() throws Throwable {
        IntBuffer result = (IntBuffer) invoke(
            "Lorg/lwjgl/system/MemoryStack;callocInt(I)Ljava/nio/IntBuffer;",
            IntBuffer.class, 3);
        assertEquals(3, result.remaining());
        assertEquals(0, result.get(0));
        assertEquals(0, result.get(1));
        assertEquals(0, result.get(2));
    }

    @Test
    public void mouseBuffersAndShaderStorageAreAllocated() throws Throwable {
        FloatBuffer mouse = (FloatBuffer) invoke(
            "Lorg/lwjgl/system/MemoryStack;mallocFloat(I)Ljava/nio/FloatBuffer;",
            FloatBuffer.class, 2);
        assertEquals(2, mouse.remaining());
        mouse.put(0, 7.5f);
        assertEquals(7.5f, mouse.get(0));
        ByteBuffer shader = (ByteBuffer) invoke(
            "Lorg/lwjgl/system/MemoryUtil;memCalloc(I)Ljava/nio/ByteBuffer;",
            ByteBuffer.class, 4);
        assertEquals(4, shader.remaining());
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
    }

    @Test
    public void windowPositionsAreZeroAndPreservePositions() throws Throwable {
        String desc = "Lorg/lwjgl/sdl/SDLVideo;"
            + "SDL_GetWindowPosition(JLjava/nio/IntBuffer;Ljava/nio/IntBuffer;)Z";
        IntBuffer x = IntBuffer.allocate(2);
        x.position(1);
        IntBuffer y = IntBuffer.allocate(2);
        y.position(1);
        assertTrue((Boolean) invoke(desc, boolean.class, 1L, x, y));
        assertEquals(1, x.position());
        assertEquals(1, y.position());
        assertEquals(0, x.get(1));
        assertEquals(0, y.get(1));
    }

    @Test
    public void handlesAreUniqueAndNonzero() throws Throwable {
        String window = "Lorg/lwjgl/sdl/SDLVideo;"
            + "SDL_CreateWindow(Ljava/lang/CharSequence;IIJ)J";
        String context = "Lorg/lwjgl/sdl/SDLVideo;SDL_GL_CreateContext(J)J";
        long first = (Long) invoke(window, long.class, "test", 800, 600, 0L);
        long second = (Long) invoke(window, long.class, "test", 800, 600, 0L);
        long third = (Long) invoke(context, long.class, first);
        assertTrue(first > 0);
        assertTrue(second > 0);
        assertTrue(third > 0);
        assertTrue(first != second);
        assertTrue(second != third);
    }

    @Test
    public void timerIsMonotonicAndEventsAreEmpty() throws Throwable {
        String timer = "Lorg/lwjgl/sdl/SDLTimer;SDL_GetTicksNS()J";
        long before = (Long) invoke(timer, long.class);
        Thread.sleep(10L);
        long after = (Long) invoke(timer, long.class);
        assertTrue(after > before);
        assertFalse((Boolean) invoke(
            "Lorg/lwjgl/sdl/SDLEvents;SDL_PollEvent(Lorg/lwjgl/sdl/SDL_Event;)Z",
            boolean.class, (Object) null));
    }
}
