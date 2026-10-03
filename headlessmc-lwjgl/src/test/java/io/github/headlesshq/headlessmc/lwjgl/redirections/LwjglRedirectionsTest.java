package io.github.headlesshq.headlessmc.lwjgl.redirections;

import io.github.headlesshq.headlessmc.lwjgl.api.Redirection;
import io.github.headlesshq.headlessmc.lwjgl.api.RedirectionManager;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

public class LwjglRedirectionsTest {
    private final Map<String, Redirection> redirections = new LinkedHashMap<>();

    {
        LwjglRedirections.register(new RedirectionManager() {
            @Override
            public Object invoke(String desc, Class<?> type, Object obj, Supplier<Redirection> fallback,
                                 Object... args) {
                return null;
            }

            @Override
            public void redirect(String desc, Redirection redirection) {
                redirections.put(desc, redirection);
            }

            @Override
            public Object invoke(Object obj, String desc, Class<?> type, Object... args) {
                return null;
            }
        });
    }

    private Object invoke(String desc, Object... args) throws Throwable {
        Redirection redirection = redirections.get(desc);
        assertNotNull(redirection, "no redirection for " + desc);
        return redirection.invoke(null, desc, Object.class, args);
    }

    @Test
    public void registersManyRedirections() {
        assertTrue(redirections.size() > 50, "expected many redirections, got " + redirections.size());
    }

    @Test
    public void constantsAreRedirected() throws Throwable {
        assertEquals(true, invoke("Lorg/lwjgl/glfw/GLFW;glfwInit()Z"));
        assertEquals("HeadlessMc-Lwjgl", invoke("Lorg/lwjgl/Sys;getVersion()Ljava/lang/String;"));
        assertEquals(36053, invoke("Lorg/lwjgl/opengl/GL30;glCheckFramebufferStatus(I)I", 1));
        assertEquals(1L, invoke("Lorg/lwjgl/glfw/GLFW;glfwCreateWindow(IILjava/lang/CharSequence;JJ)J",
            1, 1, "title", 0L, 0L));
        assertEquals(1, invoke("Lorg/lwjgl/opengl/GL11C;glGenTextures()I"));
        assertEquals(1, invoke("Lorg/lwjgl/opengl/GL20C;glCreateProgram()I"));
        assertNull(invoke("Lorg/lwjgl/stb/STBImage;stbi_failure_reason()Ljava/lang/String;"));
    }

    @Test
    public void timeAdvances() throws Throwable {
        double time = (double) invoke("Lorg/lwjgl/glfw/GLFW;glfwGetTime()D");
        assertTrue(time >= 0);
        long sysTime = (long) invoke("Lorg/lwjgl/Sys;getTime()J");
        assertTrue(sysTime > 0);
        assertNull(invoke("Lorg/lwjgl/glfw/GLFW;glfwWaitEventsTimeout(D)V", 0.0D));
    }

    @Test
    public void framebufferSizeIsFilled() throws Throwable {
        int[] width = new int[1];
        int[] height = new int[1];
        invoke("Lorg/lwjgl/glfw/GLFW;glfwGetFramebufferSize(J[I[I)V", 0L, width, height);
        assertEquals(LwjglConfig.SCREEN_WIDTH, width[0]);
        assertEquals(LwjglConfig.SCREEN_HEIGHT, height[0]);
    }

    @Test
    public void buffersAreAllocatedWithRequestedCapacity() throws Throwable {
        assertEquals(16, ((ByteBuffer) invoke(
            "Lorg/lwjgl/system/MemoryUtil;memAlloc(I)Ljava/nio/ByteBuffer;", 16)).capacity());
        assertEquals(16, ((ByteBuffer) invoke(
            "Lorg/lwjgl/BufferUtils;createByteBuffer(I)Ljava/nio/ByteBuffer;", 16)).capacity());
        assertEquals(16, ((ByteBuffer) invoke(
            "Lorg/lwjgl/system/MemoryStack;malloc(I)Ljava/nio/ByteBuffer;", 16)).capacity());
        assertEquals(8, ((IntBuffer) invoke(
            "Lorg/lwjgl/system/MemoryStack;mallocInt(I)Ljava/nio/IntBuffer;", 8)).capacity());
        assertEquals(8, ((IntBuffer) invoke(
            "Lorg/lwjgl/BufferUtils;createIntBuffer(I)Ljava/nio/IntBuffer;", 8)).capacity());
        assertEquals(8, ((IntBuffer) invoke(
            "Lorg/lwjgl/system/MemoryUtil;createIntBuffer(I)Ljava/nio/IntBuffer;", 8)).capacity());
        assertEquals(8, ((IntBuffer) invoke(
            "Lorg/lwjgl/system/MemoryUtil;memAllocInt(I)Ljava/nio/IntBuffer;", 8)).capacity());
        assertEquals(8, ((FloatBuffer) invoke(
            "Lorg/lwjgl/BufferUtils;createFloatBuffer(I)Ljava/nio/FloatBuffer;", 8)).capacity());
        assertEquals(8, ((FloatBuffer) invoke(
            "Lorg/lwjgl/system/MemoryUtil;memAllocFloat(I)Ljava/nio/FloatBuffer;", 8)).capacity());
        assertEquals(8, ((LongBuffer) invoke(
            "Lorg/lwjgl/system/MemoryUtil;memAllocLong(I)Ljava/nio/LongBuffer;", 8)).capacity());
        assertEquals(8, ((DoubleBuffer) invoke(
            "Lorg/lwjgl/system/MemoryUtil;memAllocDouble(I)Ljava/nio/DoubleBuffer;", 8)).capacity());
        assertEquals(8, ((ShortBuffer) invoke(
            "Lorg/lwjgl/system/MemoryUtil;memAllocShort(I)Ljava/nio/ShortBuffer;", 8)).capacity());
        assertEquals(4, ((ByteBuffer) invoke(
            "Lorg/lwjgl/system/MemoryUtil;memByteBuffer(JI)Ljava/nio/ByteBuffer;", 0L, 4)).capacity());
        assertEquals(4, ((ByteBuffer) invoke(
            "Lorg/lwjgl/system/MemoryUtil;memByteBufferSafe(JI)Ljava/nio/ByteBuffer;", 0L, 4)).capacity());
        assertEquals(4, ((IntBuffer) invoke(
            "Lorg/lwjgl/system/MemoryUtil;memIntBuffer(JI)Ljava/nio/IntBuffer;", 0L, 4)).capacity());
        assertEquals(4, ((ByteBuffer) invoke(
            "Lorg/lwjgl/opengl/GL30;glMapBufferRange(IJJI)Ljava/nio/ByteBuffer;", 0, 0L, 4L, 0)).capacity());
    }

    @Test
    public void mapBufferUsesLastBufferDataSize() throws Throwable {
        invoke("Lorg/lwjgl/opengl/GL15;glBufferData(IJI)V", 0, 12L, 0);
        ByteBuffer mapped = (ByteBuffer) invoke("Lorg/lwjgl/opengl/GL15;glMapBuffer(II)Ljava/nio/ByteBuffer;", 0, 0);
        assertEquals(12, mapped.capacity());
    }

    @Test
    public void memSliceSlicesBuffer() throws Throwable {
        String desc = "Lorg/lwjgl/system/MemoryUtil;memSlice(Ljava/nio/ByteBuffer;II)Ljava/nio/ByteBuffer;";
        ByteBuffer buffer = ByteBuffer.wrap(new byte[]{1, 2, 3, 4, 5});

        ByteBuffer slice = (ByteBuffer) invoke(desc, buffer, 1, 3);
        assertEquals(3, slice.capacity());
        assertEquals(2, slice.get(0));

        ByteBuffer fromNull = (ByteBuffer) invoke(desc, null, 0, 7);
        assertEquals(7, fromNull.capacity());

        assertThrows(IllegalArgumentException.class, () -> {
            try {
                invoke(desc, ByteBuffer.wrap(new byte[2]), -1, 1);
            } catch (Throwable t) {
                throw (Exception) t;
            }
        });
    }

    @Test
    public void glGetIntegerKnowsDeviceLimits() throws Throwable {
        String desc = "Lorg/lwjgl/opengl/GL11;glGetInteger(I)I";
        assertEquals(LwjglConfig.UNIFORM_OFFSET_ALIGNMENT,
            invoke(desc, LwjglConfig.GL_UNIFORM_BUFFER_OFFSET_ALIGNMENT));
        assertEquals(LwjglConfig.MAX_DRAW_BUFFERS, invoke(desc, LwjglConfig.GL_MAX_DRAW_BUFFERS));
        assertEquals(LwjglConfig.MAX_TEXTURE_SIZE, invoke(desc, LwjglConfig.GL_MAX_TEXTURE_SIZE));
        assertEquals(0, invoke(desc, 12345));
    }

    @Test
    public void texLevelParameterHandlesSpecialCases() throws Throwable {
        String desc = "Lorg/lwjgl/opengl/GL11;glGetTexLevelParameteri(III)I";
        assertEquals(LwjglConfig.GL_TEXTURE_INTERNAL_FORMAT,
            invoke(desc, 0, 0, LwjglConfig.GL_TEXTURE_INTERNAL_FORMAT_CONST));
        assertEquals(0, invoke(desc, 0, 1, LwjglConfig.GL_TEXTURE_WIDTH));
        assertEquals(LwjglConfig.TEXTURE_SIZE, invoke(desc, 0, 0, 12345));
    }

    @Test
    public void glCapabilitiesConstructorSetsOpenGl30() throws Throwable {
        @SuppressWarnings("unused")
        class Capabilities {
            public boolean OpenGL30;
        }

        Capabilities capabilities = new Capabilities();
        Redirection redirection = redirections.get("Lorg/lwjgl/opengl/GLCapabilities;<init>()V");
        redirection.invoke(capabilities, "Lorg/lwjgl/opengl/GLCapabilities;<init>()V", Object.class);
        assertTrue(capabilities.OpenGL30);
    }

    @Test
    public void vulkanLimitsAreRedirected() throws Throwable {
        assertEquals((long) LwjglConfig.UNIFORM_OFFSET_ALIGNMENT,
            invoke("Lorg/lwjgl/vulkan/VkPhysicalDeviceLimits;minUniformBufferOffsetAlignment()J"));
        assertEquals(LwjglConfig.MAX_TEXTURE_SIZE,
            invoke("Lorg/lwjgl/vulkan/VkPhysicalDeviceLimits;maxImageDimension2D()I"));
        assertEquals(Long.MAX_VALUE,
            invoke("Lorg/lwjgl/vulkan/VkPhysicalDeviceVulkan11Properties;maxMemoryAllocationSize()J"));
        assertEquals(LwjglConfig.MAX_DRAW_BUFFERS,
            invoke("Lorg/lwjgl/vulkan/VkPhysicalDeviceLimits;maxColorAttachments()I"));
    }

    @Test
    public void memByteBufferConvertsTypedBuffers() throws Throwable {
        ByteBuffer fromInts = (ByteBuffer) invoke(
            "Lorg/lwjgl/system/MemoryUtil;memByteBuffer(Ljava/nio/IntBuffer;)Ljava/nio/ByteBuffer;",
            IntBuffer.wrap(new int[]{1, 2}));
        assertEquals(8, fromInts.capacity());
        assertEquals(1, fromInts.asIntBuffer().get(0));

        ByteBuffer fromShorts = (ByteBuffer) invoke(
            "Lorg/lwjgl/system/MemoryUtil;memByteBuffer(Ljava/nio/ShortBuffer;)Ljava/nio/ByteBuffer;",
            ShortBuffer.wrap(new short[]{1, 2}));
        assertEquals(4, fromShorts.capacity());

        ByteBuffer fromLongs = (ByteBuffer) invoke(
            "Lorg/lwjgl/system/MemoryUtil;memByteBuffer(Ljava/nio/LongBuffer;)Ljava/nio/ByteBuffer;",
            LongBuffer.wrap(new long[]{1}));
        assertEquals(8, fromLongs.capacity());

        ByteBuffer fromFloats = (ByteBuffer) invoke(
            "Lorg/lwjgl/system/MemoryUtil;memByteBuffer(Ljava/nio/FloatBuffer;)Ljava/nio/ByteBuffer;",
            FloatBuffer.wrap(new float[]{1f}));
        assertEquals(4, fromFloats.capacity());

        ByteBuffer fromDoubles = (ByteBuffer) invoke(
            "Lorg/lwjgl/system/MemoryUtil;memByteBuffer(Ljava/nio/DoubleBuffer;)Ljava/nio/ByteBuffer;",
            DoubleBuffer.wrap(new double[]{1d}));
        assertEquals(8, fromDoubles.capacity());
    }

}
