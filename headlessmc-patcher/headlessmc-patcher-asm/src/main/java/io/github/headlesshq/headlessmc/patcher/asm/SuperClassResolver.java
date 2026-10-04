package io.github.headlesshq.headlessmc.patcher.asm;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;

/**
 * {@code ClassWriter.getCommonSuperClass(String, String)}
 * comes with some caveats.
 * This class helps with implementing that method.
 *
 * @see SuperClassStrategy
 * @see ClassWriter
 */
public interface SuperClassResolver extends AutoCloseable {
    /**
     * Attempts to return the common super type of the two given types.
     * See {@code ClassWriter.getCommonSuperClass(String, String)}.
     *
     * @param type1 the internal name of a class (see {@link Type#getInternalName()}).
     * @param type2 the internal name of another class (see {@link Type#getInternalName()}).
     * @return the internal name of the common super class of the two given classes (see {@link
     * Type#getInternalName()}), or an empty Optional if the common super class could not be found.
     * @throws HeadlessMcException if something goes wrong.
     * @see ClassWriter
     */
    String getCommonSuperClass(String type1, String type2) throws HeadlessMcException;

    @Override
    void close() throws HeadlessMcException;

}
