package io.github.headlesshq.headlessmc.lwjgl.transformer;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;

public final class MacosMenuTransformer {
    private MacosMenuTransformer() {
    }

    public static byte[] transform(String name, byte[] bytes) {
        if (!"com/mojang/blaze3d/platform/MacosUtil".equals(name)) {
            return null;
        }

        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, 0);
        for (MethodNode method : node.methods) {
            if (method.name.equals("disableCloseWindowMenuItem")
                && method.desc.equals("()V")
                && (method.access & Opcodes.ACC_STATIC) != 0) {
                // SDL creates no Cocoa window or windowsMenu in no-render mode.
                // Transform at class loading: Fabric verifies the original jar,
                // and NeoForge can load its separately patched Minecraft jar.
                method.instructions = new InsnList();
                method.instructions.add(new InsnNode(Opcodes.RETURN));
                method.tryCatchBlocks.clear();
                if (method.localVariables != null) {
                    method.localVariables.clear();
                }
                method.visibleLocalVariableAnnotations = null;
                method.invisibleLocalVariableAnnotations = null;
                method.maxStack = 0;
                method.maxLocals = 0;
                // Other methods retain their frames. Resolving their types here
                // would require accessing the loader's not-yet-ready game layer.
                ClassWriter writer = new ClassWriter(0);
                node.accept(writer);
                return writer.toByteArray();
            }
        }

        return null;
    }
}
