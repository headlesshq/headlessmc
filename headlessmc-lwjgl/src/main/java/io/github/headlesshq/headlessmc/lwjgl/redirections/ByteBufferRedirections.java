package io.github.headlesshq.headlessmc.lwjgl.redirections;

import io.github.headlesshq.headlessmc.lwjgl.api.RedirectionManager;

import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class ByteBufferRedirections {
    private static final ByteOrder NATIVE_ORDER = ByteOrder.nativeOrder();

    public static void redirect(RedirectionManager manager) {
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
            (obj, desc, type, args) -> ByteBuffer.allocate((int) args[0]).order(NATIVE_ORDER));
        manager.redirect("Lorg/lwjgl/system/MemoryUtil;" +
            "memUTF8(Ljava/lang/CharSequence;Z)Ljava/nio/ByteBuffer;",
            (obj, desc, type, args) -> utf8(Objects.requireNonNull(args[0]), (boolean) args[1]));
        manager.redirect("Lorg/lwjgl/system/MemoryUtil;" +
            "memUTF8(Ljava/lang/CharSequence;)Ljava/nio/ByteBuffer;",
            (obj, desc, type, args) -> utf8(Objects.requireNonNull(args[0]), true));

        manager.redirect("Lorg/lwjgl/system/MemoryUtil;" +
            "memUTF8Safe(Ljava/lang/CharSequence;Z)Ljava/nio/ByteBuffer;",
            (obj, desc, type, args) -> utf8(args[0], (boolean) args[1]));
        manager.redirect("Lorg/lwjgl/system/MemoryUtil;" +
            "memUTF8Safe(Ljava/lang/CharSequence;)Ljava/nio/ByteBuffer;",
            (obj, desc, type, args) -> utf8(args[0], true));

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

    private static ByteBuffer utf8(Object input, boolean terminated) {
        if (input == null) {
            return null;
        }

        byte[] bytes = input.toString().getBytes(StandardCharsets.UTF_8);
        ByteBuffer result = ByteBuffer.allocate(
            bytes.length + (terminated ? 1 : 0)).order(NATIVE_ORDER);
        result.put(bytes);
        result.flip();
        if (terminated) {
            result.limit(result.capacity());
        }
        return result;
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
