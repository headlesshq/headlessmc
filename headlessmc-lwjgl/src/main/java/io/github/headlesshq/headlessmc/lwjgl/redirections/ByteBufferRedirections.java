package io.github.headlesshq.headlessmc.lwjgl.redirections;

import io.github.headlesshq.headlessmc.lwjgl.api.RedirectionManager;

import java.nio.*;

public class ByteBufferRedirections {
    private static final ByteOrder NATIVE_ORDER = ByteOrder.nativeOrder();

    public static void redirect(RedirectionManager manager) {
        // this is not that great, but the entire idea of LWJGL redirection is not, so whatever

        // 1.21.10
        // Lorg/lwjgl/system/MemoryUtil;memByteBuffer(Ljava/nio/IntBuffer;)Ljava/nio/ByteBuffer;
        manager.redirect("Lorg/lwjgl/system/MemoryUtil;memByteBuffer(Ljava/nio/IntBuffer;)Ljava/nio/ByteBuffer;",
                (obj, desc, type, args) -> memByteBuffer((IntBuffer) args[0]));

        manager.redirect("Lorg/lwjgl/system/MemoryUtil;memByteBuffer(Ljava/nio/ShortBuffer;)Ljava/nio/ByteBuffer;",
                (obj, desc, type, args) -> memByteBuffer((ShortBuffer) args[0]));

        manager.redirect("Lorg/lwjgl/system/MemoryUtil;memByteBuffer(Ljava/nio/CharBuffer;)Ljava/nio/ByteBuffer;",
                (obj, desc, type, args) -> memByteBuffer((CharBuffer) args[0]));

        manager.redirect("Lorg/lwjgl/system/MemoryUtil;memByteBuffer(Ljava/nio/LongBuffer;)Ljava/nio/ByteBuffer;",
                (obj, desc, type, args) -> memByteBuffer((LongBuffer) args[0]));

        manager.redirect("Lorg/lwjgl/system/MemoryUtil;memByteBuffer(Ljava/nio/FloatBuffer;)Ljava/nio/ByteBuffer;",
                (obj, desc, type, args) -> memByteBuffer((FloatBuffer) args[0]));

        manager.redirect("Lorg/lwjgl/system/MemoryUtil;memByteBuffer(Ljava/nio/DoubleBuffer;)Ljava/nio/ByteBuffer;",
                (obj, desc, type, args) -> memByteBuffer((DoubleBuffer) args[0]));
    }

    private static ByteBuffer memByteBuffer(IntBuffer buffer) {
        IntBuffer slice = buffer.slice();
        ByteBuffer bb = ByteBuffer.allocate(slice.remaining() << 2).order(NATIVE_ORDER);
        bb.asIntBuffer().put(slice);
        return bb;
    }

    private static ByteBuffer memByteBuffer(ShortBuffer buffer) {
        ShortBuffer slice = buffer.slice();
        ByteBuffer bb = ByteBuffer.allocate(slice.remaining() << 1).order(NATIVE_ORDER);
        bb.asShortBuffer().put(slice);
        return bb;
    }

    private static ByteBuffer memByteBuffer(CharBuffer buffer) {
        CharBuffer slice = buffer.slice();
        ByteBuffer bb = ByteBuffer.allocate(slice.remaining() << 1).order(NATIVE_ORDER);
        bb.asCharBuffer().put(slice);
        return bb;
    }

    private static ByteBuffer memByteBuffer(LongBuffer buffer) {
        LongBuffer slice = buffer.slice();
        ByteBuffer bb = ByteBuffer.allocate(slice.remaining() << 3).order(NATIVE_ORDER);
        bb.asLongBuffer().put(slice);
        return bb;
    }

    private static ByteBuffer memByteBuffer(FloatBuffer buffer) {
        FloatBuffer slice = buffer.slice();
        ByteBuffer bb = ByteBuffer.allocate(slice.remaining() << 2).order(NATIVE_ORDER);
        bb.asFloatBuffer().put(slice);
        return bb;
    }

    private static ByteBuffer memByteBuffer(DoubleBuffer buffer) {
        DoubleBuffer slice = buffer.slice();
        ByteBuffer bb = ByteBuffer.allocate(slice.remaining() << 3).order(NATIVE_ORDER);
        bb.asDoubleBuffer().put(slice);
        return bb;
    }

}
