package io.github.headlesshq.headlessmc.lwjgl.launchwrapper;

import io.github.headlesshq.headlessmc.lwjgl.api.Transformer;
import io.github.headlesshq.headlessmc.lwjgl.transformer.AsmUtil;
import io.github.headlesshq.headlessmc.lwjgl.transformer.LwjglTransformer;
import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import java.util.Locale;

/**
 * Added to {@link net.minecraft.launchwrapper.LaunchClassLoader}
 * in {@link LwjglTweaker}.
 */
@SuppressWarnings("unused")
public class LaunchWrapperLwjglTransformer implements IClassTransformer {
    private final Transformer transformer = new LwjglTransformer();

    @Override
    public byte[] transform(String name, String transformed, byte[] clazz) {
        if (name != null && name.toLowerCase(Locale.ENGLISH).startsWith("org.lwjgl")) {
            ClassNode node = AsmUtil.read(clazz);
            transformer.transform(node);
            return AsmUtil.write(node, ClassWriter.COMPUTE_FRAMES);
        }

        return clazz;
    }

}
