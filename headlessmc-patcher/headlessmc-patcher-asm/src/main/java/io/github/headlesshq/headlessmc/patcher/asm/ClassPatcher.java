package io.github.headlesshq.headlessmc.patcher.asm;

import io.github.headlesshq.headlessmc.patcher.Patcher;
import org.objectweb.asm.tree.ClassNode;

/**
 * Represents a transformer that modifies the bytecode of classes with ASM.
 * {@link ClassPatcher}s can either run in the context of {@link Patcher},
 * or from a transforming ClassLoader/java agent that modifies classes
 * at runtime, when they are loaded.
 */
public interface ClassPatcher {
    /**
     * Modifies the bytecode of the given class object.
     *
     * @param classNode the byte code to modify in ASM tree representation.
     */
    void patch(ClassNode classNode);

    /**
     * Checks if {@link #patch(ClassNode)} should be called for a given class.
     *
     * @param classFileName the name of the class file,
     *                      e.g. {@code "com/example/Class.class"},
     *                      note the appended {@code ".class"}.
     *                      Can also be {@code "module-info.class"}.
     * @return {@code true} of this {@link ClassPatcher} can patch the
     * byte code of the class file with the given name.
     */
    boolean matches(String classFileName);

    /**
     * Decides if the given class file should be skipped.
     * That means it is either excluded from the patched Jar,
     * when running via {@link Patcher}, or a {@link ClassNotFoundException}
     * is thrown from the ClassLoader that uses this {@link ClassPatcher}
     * to modify bytecode at runtime.
     *
     * @param classFileName the name of the class file,
     *                      e.g. {@code "com/example/Class.class"},
     *                      note the appended {@code ".class"}.
     *                      Can also be {@code "module-info.class"}.
     * @return {@code true} if the class should be skipped.
     */
    default boolean skip(String classFileName) {
        return false;
    }

}
