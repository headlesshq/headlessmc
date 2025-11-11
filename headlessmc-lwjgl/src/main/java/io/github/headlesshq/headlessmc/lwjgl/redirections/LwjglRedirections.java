package io.github.headlesshq.headlessmc.lwjgl.redirections;

import lombok.experimental.UtilityClass;
import io.github.headlesshq.headlessmc.lwjgl.LwjglProperties;
import io.github.headlesshq.headlessmc.lwjgl.api.RedirectionManager;
import io.github.headlesshq.headlessmc.lwjgl.redirections.stb.STBImage;
import io.github.headlesshq.headlessmc.lwjgl.redirections.stb.STBImageRedirection;
import io.github.headlesshq.headlessmc.lwjgl.redirections.stb.STBImageRedirectionNoAWT;

import java.lang.reflect.Field;
import java.nio.*;

import static io.github.headlesshq.headlessmc.lwjgl.api.Redirection.of;

// TODO: redirect Keyboard and Mouse?
@UtilityClass
public class LwjglRedirections {
    public static final int GL_TEXTURE_WIDTH = 4096;
    public static final int GL_TEXTURE_INTERNAL_FORMAT_CONST = 4099;
    public static final int GL_UNIFORM_BUFFER_OFFSET_ALIGNMENT = 0x8A34;

    public static final int GL_TEXTURE_INTERNAL_FORMAT = Integer.parseInt(
        System.getProperty(LwjglProperties.GL_TEXTURE_INTERNAL_FORMAT, "32856")); //RGBA8
    public static final int TEXTURE_SIZE = Integer.parseInt(
        System.getProperty(LwjglProperties.TEXTURE_SIZE, "1024"));
    public static final boolean FULLSCREEN = Boolean.parseBoolean(
        System.getProperty(LwjglProperties.FULLSCREEN, "true"));
    public static final int SCREEN_WIDTH = Integer.parseInt(
        System.getProperty(LwjglProperties.SCREEN_WIDTH, "1920"));
    public static final int SCREEN_HEIGHT = Integer.parseInt(
        System.getProperty(LwjglProperties.SCREEN_HEIGHT, "1080"));
    public static final int REFRESH_RATE = Integer.parseInt(
        System.getProperty(LwjglProperties.REFRESH_RATE, "100"));
    public static final int BITS_PER_PIXEL = Integer.parseInt(
        System.getProperty(LwjglProperties.BITS_PER_PIXEL, "32"));
    public static final int JNI_VERSION = Integer.parseInt(
        System.getProperty(LwjglProperties.JNI_VERSION, "24"));
    public static final int UNIFORM_OFFSET_ALIGNMENT = Integer.parseInt(
            System.getProperty(LwjglProperties.UNIFORM_OFFSET_ALIGNMENT, "1"));
    private static final ThreadLocal<Long> CURRENT_BUFFER_SIZE =
        ThreadLocal.withInitial(() -> 0L);
    private static final long START = System.nanoTime();

    public static void register(RedirectionManager manager) {
        manager.redirect(DisplayUpdater.DESC, new DisplayUpdater());
        manager.redirect("Lorg/lwjgl/glfw/GLFW;glfwWaitEventsTimeout(D)V",
                         (obj, desc, type, args) -> {
                             Thread.sleep((long) ((double) args[0] * 1000L));
                             return null;
                         });

        // 1.16.5-forge-36.2.22, ClientVisualization
        manager.redirect("Lorg/lwjgl/glfw/GLFW;glfwCreateWindow(" +
                             "IILjava/lang/CharSequence;JJ)J", of(1L));

        // 1.16.5-forge-36.2.22, GlStateManager
        manager.redirect(
            "Lorg/lwjgl/opengl/GLCapabilities;<init>()V",
            (obj, desc, type, args) -> {
                try {
                    Field field = obj.getClass().getDeclaredField("OpenGL30");
                    field.setAccessible(true);
                    field.set(obj, true);
                } catch (ReflectiveOperationException e) {
                    //noinspection CallToPrintStackTrace
                    e.printStackTrace();
                }

                return null;
            });

        manager.redirect("Lorg/lwjgl/glfw/GLFW;glfwGetTime()D",
                         (obj, desc, type, args) ->
                             (System.nanoTime() - START) / 1_000_000_000.0D);

        // TODO: check this does what it's supposed to
        manager.redirect("Lorg/lwjgl/glfw/GLFW;glfwGetFramebufferSize(J[I[I)V",
                         (obj, desc, type, args) -> {
                             int[] width = (int[]) args[1];
                             width[0] = SCREEN_WIDTH;
                             int[] height = (int[]) args[2];
                             height[0] = SCREEN_HEIGHT;
                             return null;
                         });

        manager.redirect("Lorg/lwjgl/opengl/Display;getWidth()I",
                         of(SCREEN_WIDTH));
        manager.redirect("Lorg/lwjgl/opengl/Display;getHeight()I",
                         of(SCREEN_HEIGHT));
        manager.redirect("Lorg/lwjgl/opengl/Display;isFullscreen()Z",
                         of(FULLSCREEN));

        manager.redirect("Lorg/lwjgl/DefaultSysImplementation;getJNIVersion()I",
                         of(JNI_VERSION));

        // TODO: make this configurable?
        manager.redirect("Lorg/lwjgl/opengl/Display;isActive()Z", of(true));

        manager.redirect("Lorg/lwjgl/opengl/DisplayMode;isFullscreenCapable()Z",
                         of(FULLSCREEN));
        manager.redirect("Lorg/lwjgl/opengl/DisplayMode;getWidth()I",
                         of(SCREEN_WIDTH));
        manager.redirect("Lorg/lwjgl/opengl/DisplayMode;getHeight()I",
                         of(SCREEN_HEIGHT));
        manager.redirect("Lorg/lwjgl/opengl/DisplayMode;getFrequency()I",
                         of(REFRESH_RATE));
        manager.redirect("Lorg/lwjgl/opengl/DisplayMode;getBitsPerPixel()I",
                         of(BITS_PER_PIXEL));

        manager.redirect("Lorg/lwjgl/glfw/GLFW;glfwInit()Z",
                         of(true));
        manager.redirect("Lorg/lwjgl/Sys;getVersion()Ljava/lang/String;",
                         of("HeadlessMc-Lwjgl"));

        manager.redirect("Lorg/lwjgl/Sys;getTimerResolution()J",
                         of(1000L));
        manager.redirect("Lorg/lwjgl/Sys;getTime()J", (obj, desc, type, args)
            -> System.nanoTime() / 1000000L);

        manager.redirect("Lorg/lwjgl/opengl/GL11;glGetTexLevelParameteri(III)I",
                (obj, desc, type, args) -> {
                    if ((int) args[2] == GL_TEXTURE_INTERNAL_FORMAT_CONST) {
                        // Neoforge 1.21.5
                        // Couldn't find a matching vanilla TextureFormat for OpenGL internal format id
                        // com.mojang.blaze3d.opengl.GlDevice
                        return GL_TEXTURE_INTERNAL_FORMAT;
                    } else if ((int) args[2] == GL_TEXTURE_WIDTH && (int) args[1]/*level*/ > 0) {
                        return 0; // otherwise Neoforge 1.21.5 gets caught in an endless loop of checking width != 0; for higher levels
                    }

                    return TEXTURE_SIZE;
                });
        manager.redirect("Lorg/lwjgl/opengl/GL11;glGenLists(I)I", of(-1));

        manager.redirect(
            "Lorg/lwjgl/system/MemoryUtil$MemoryAllocator;malloc(J)J",
            of(1L));
        manager.redirect(
            "Lorg/lwjgl/system/MemoryUtil$MemoryAllocator;realloc(J)J",
            of(1L));
        manager.redirect(
            "Lorg/lwjgl/system/MemoryUtil$MemoryAllocator;calloc(J)J",
            of(1L));
        manager.redirect(
            "Lorg/lwjgl/system/MemoryUtil$MemoryAllocator;realloc(J)J",
            of(1L));
        manager.redirect(
            "Lorg/lwjgl/system/MemoryUtil$MemoryAllocator;realloc(JJ)J",
            of(1L));
        manager.redirect(
            "Lorg/lwjgl/system/MemoryUtil$MemoryAllocator;aligned_alloc(JJ)J",
            of(1L));

        // blaze3d RenderTarget
        manager.redirect("Lorg/lwjgl/opengl/GL30;glCheckFramebufferStatus(I)I",
                         of(36053));

        // blaze3d NativeImage checks that values are != 0 for this function
        manager.redirect("Lorg/lwjgl/system/MemoryUtil;nmemAlloc(J)J", of(1L));
        manager.redirect("Lorg/lwjgl/system/MemoryUtil;nmemCalloc(JJ)J", of(1L));

        // TODO: because MemoryUtil and the Buffers are actually being used,
        //  redirect all methods inside those to return proper Buffers?
        //  - ignore list?
        // I WISH WE COULD SUBCLASS BUFFERS WTF
        manager.redirect("Lorg/lwjgl/system/MemoryUtil;memByteBuffer(JI)" +
                             "Ljava/nio/ByteBuffer;",
                         (obj, desc, type, args) ->
                             ByteBuffer.wrap(new byte[(int) args[1]]));
        manager.redirect("Lorg/lwjgl/system/MemoryUtil;" +
                             "memAlloc(I)Ljava/nio/ByteBuffer;",
                         (obj, desc, type, args) -> ByteBuffer.wrap(
                             new byte[(int) args[0]]));
        manager.redirect("Lorg/lwjgl/system/MemoryStack;" +
                             "mallocInt(I)Ljava/nio/IntBuffer;",
                         (obj, desc, type, args) -> IntBuffer.wrap(
                             new int[(int) args[0]]));
        manager.redirect("Lorg/lwjgl/BufferUtils;createIntBuffer(I)" +
                             "Ljava/nio/IntBuffer;",
                         (obj, desc, type, args) -> IntBuffer.wrap(
                             new int[(int) args[0]]));
        manager.redirect("Lorg/lwjgl/BufferUtils;createFloatBuffer(I)" +
                             "Ljava/nio/FloatBuffer;",
                         (obj, desc, type, args) -> FloatBuffer.wrap(
                             new float[(int) args[0]]));
        manager.redirect("Lorg/lwjgl/system/MemoryUtil;createIntBuffer(I)" +
                             "Ljava/nio/IntBuffer;",
                         (obj, desc, type, args) -> IntBuffer.wrap(
                             new int[(int) args[0]]));
        manager.redirect("Lorg/lwjgl/system/MemoryUtil;" +
                             "memAllocFloat(I)Ljava/nio/FloatBuffer;",
                         (obj, desc, type, args) -> FloatBuffer.wrap(
                             new float[(int) args[0]]));
        manager.redirect("Lorg/lwjgl/BufferUtils;createByteBuffer(I)" +
                             "Ljava/nio/ByteBuffer;",
                         (obj, desc, type, args) -> ByteBuffer.wrap(
                             new byte[(int) args[0]]));
        manager.redirect("Lorg/lwjgl/system/MemoryUtil;memAllocInt(I)" +
                             "Ljava/nio/IntBuffer;",
                         (obj, desc, type, args) -> IntBuffer.wrap(
                             new int[(int) args[0]]));
        manager.redirect("Lorg/lwjgl/system/MemoryUtil;memAllocLong(I)" +
                             "Ljava/nio/LongBuffer;",
                         (obj, desc, type, args) -> LongBuffer.wrap(
                             new long[(int) args[0]]));
        manager.redirect("Lorg/lwjgl/system/MemoryUtil;memAllocDouble(I)" +
                             "Ljava/nio/DoubleBuffer;",
                         (obj, desc, type, args) -> DoubleBuffer.wrap(
                             new double[(int) args[0]]));
        manager.redirect("Lorg/lwjgl/system/MemoryUtil;memAllocShort(I)" +
                             "Ljava/nio/ShortBuffer;",
                         (obj, desc, type, args) -> ShortBuffer.wrap(
                             new short[(int) args[0]]));
        manager.redirect("Lorg/lwjgl/system/MemoryStack;malloc(I)" +
                             "Ljava/nio/ByteBuffer;",
                         (obj, desc, type, args) -> ByteBuffer.wrap(
                             new byte[(int) args[0]]));

        // TODO: this is really bad...
        manager.redirect("Lorg/lwjgl/opengl/GL15;glBufferData(IJI)V",
                         (obj, desc, type, args) -> {
                             CURRENT_BUFFER_SIZE.set((Long) args[1]);
                             return null;
                         });
        manager.redirect("Lorg/lwjgl/opengl/GL15;glMapBuffer(II)" +
                             "Ljava/nio/ByteBuffer;",
                         (obj, desc, type, args) -> ByteBuffer.wrap(
                             new byte[CURRENT_BUFFER_SIZE.get().intValue()]));

        manager.redirect("Lorg/lwjgl/system/MemoryUtil;" +
                             "memAddress(Ljava/nio/ByteBuffer;)J", of(1L));

        if (Boolean.parseBoolean(System.getProperty(LwjglProperties.NO_AWT, "false"))) {
            manager.redirect(STBImage.DESC, STBImageRedirectionNoAWT.INSTANCE);
        } else {
            manager.redirect(STBImage.DESC, STBImageRedirection.INSTANCE);
        }

        manager.redirect(MemASCIIRedirection.DESC,
                         MemASCIIRedirection.INSTANCE);

        // act as if we compiled a shader program (blaze3d program)
        manager.redirect("Lorg/lwjgl/opengl/GL20;glGetShaderi(II)I", of(1));
        manager.redirect("Lorg/lwjgl/opengl/GL20;glCreateProgram()I", of(1));
        manager.redirect("Lorg/lwjgl/opengl/GL20;glGetProgrami(II)I", of(1));

        manager.redirect("Lorg/lwjgl/openal/ALC10;alcOpenDevice(" +
                             "Ljava/lang/CharSequence;)J", of(1L));

        manager.redirect("Lorg/lwjgl/system/MemoryUtil;memSlice(" +
                             "Ljava/nio/ByteBuffer;II)Ljava/nio/ByteBuffer;",
                         (obj, desc, type, args) -> {
                             // TODO: redirected for now to prevent console
                             //  spam, but this should be done properly at some
                             //  point. It looks as if the returned buffer is
                             //  never used except for other redirected lwjgl
                             //  methods, so null is fairly safe rn.
                             /*
                             ByteBuffer buffer = (ByteBuffer) args[0];
                             if (buffer == null) {
                                return null;
                             }

                             int offset = (int) args[1];
                             int capacity = (int) args[2];

                             int position = buffer.position() + offset;
                             if (offset < 0 || buffer.limit() < position) {
                                 throw new IllegalArgumentException();
                             }

                             if (capacity < 0 || buffer.capacity()
                                 - position < capacity) {
                                 throw new IllegalArgumentException();
                             }

                             // TODO: allocate new bytebuffer of size and
                             //       copy bytes to that buffer

                             */

                             return null;
                         });

        // 1.20
        manager.redirect("Lorg/lwjgl/system/MemoryUtil;" +
                             "memIntBuffer(JI)Ljava/nio/IntBuffer;",
                         (obj, desc, type, args) ->
                             IntBuffer.wrap(new int[(int) args[1]])
        );

        CustomBufferRedirection.redirect(manager);

        // 1.20.1
        ForgeDisplayWindowRedirections.redirect(manager);

        // 1.20.4 (3?)
        MemReallocRedirections.redirect(manager);

        // 1.21.5
        manager.redirect(
            "Lorg/lwjgl/opengl/GL30;glMapBufferRange(IJJI)Ljava/nio/ByteBuffer;",
            (obj, desc, type, args) -> ByteBuffer.wrap(new byte[(int) ((long) args[2])])
        );

        manager.redirect(
            "Lorg/lwjgl/system/MemoryUtil;memByteBufferSafe(JI)Ljava/nio/ByteBuffer;",
            (obj, desc, type, args) -> ByteBuffer.wrap(new byte[(int) args[1]])
        );

        // Embeddium
        // https://github.com/3arthqu4ke/headlessmc/issues/208
        // manager.redirect("Lorg/lwjgl/opengl/GL32C;glFenceSync(II)J", of(1L));
        // not enough, game now crashes with SIGSEGV, use xvfb for embeddium
        // v  ~StubRoutines::jlong_disjoint_arraycopy
        //J 10112 c2 jdk.internal.misc.Unsafe.copyMemory(Ljava/lang/Object;JLjava/lang/Object;JJ)V java.base@17.0.12 (33 bytes) @ 0x00007ea004efdf19 [0x00007ea004efdda0+0x0000000000000179]
        //j  jdk.internal.misc.Unsafe.copyMemory(JJJ)V+7 java.base@17.0.12
        //j  sun.misc.Unsafe.copyMemory(JJJ)V+7 jdk.unsupported@17.0.12
        //j  net.caffeinemc.mods.sodium.api.memory.MemoryIntrinsics.copyMemory(JJI)V+8
        //j  net.minecraft.class_287.push(Lorg/lwjgl/system/MemoryStack;JILnet/caffeinemc/mods/sodium/api/vertex/format/VertexFormatDescription;)V+47
        //j  me.jellysquid.mods.sodium.client.render.vertex.buffer.SodiumBufferBuilder.push(Lorg/lwjgl/system/MemoryStack;JILnet/caffeinemc/mods/sodium/api/vertex/format

        // 1.21.6 onwards
        // DynamicUniformStorage.<init>
        // GlDevice this.uniformOffsetAlignment = GL11.glGetInteger(35380);
        // division by zero, because the integer returned is 0
        manager.redirect("Lorg/lwjgl/opengl/GL11;glGetInteger(I)I",
                (obj, desc, type, args) -> {
                    if ((int) args[0] == GL_UNIFORM_BUFFER_OFFSET_ALIGNMENT) {
                        return UNIFORM_OFFSET_ALIGNMENT;
                    }

                    return 0;
                });

        // Forge 1.21.10
        //java.lang.IllegalArgumentException: mipLevels must be at least 1
        //        at TRANSFORMER/minecraft@1.21.10/com.mojang.blaze3d.opengl.GlDevice.createTexture(GlDevice.java:119) ~[forge-1.21.10-60.0.17-client.jar:?]
        manager.redirect("Lorg/lwjgl/opengl/GL11C;glGenTextures()I",
                (obj, desc, type, args) -> 1);
    }

}
