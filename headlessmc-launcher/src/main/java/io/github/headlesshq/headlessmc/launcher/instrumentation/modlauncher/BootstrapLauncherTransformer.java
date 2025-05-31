package io.github.headlesshq.headlessmc.launcher.instrumentation.modlauncher;

import io.github.headlesshq.headlessmc.launcher.instrumentation.AbstractClassTransformer;
import io.github.headlesshq.headlessmc.launcher.instrumentation.EntryStream;
import io.github.headlesshq.headlessmc.launcher.instrumentation.Target;
import lombok.CustomLog;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.Locale;

/**
 * Modlauncher, BootstrapLauncher and SecureJarHandler all call ModuleLayer.boot sometimes.
 * Normally this would be fine, but we cannot load forge from the boot layer,
 * because we load it dynamically, at runtime.
 * <p>So first, we replace all calls to ModuleLayer.boot with cpw.mods.cl.ModuleClassLoader.class.getModule().getLayer().
 * <p>The second problem is that the ModuleClassloader delegates classloading of some classes
 * to Classloader.getPlatformClassLoader, which we replace with cpw.mods.cl.ModuleClassLoader.class.getClassloader().
 * <p>The layer and classloader of the ModuleClassLoader should be the one we introduced to load it.
 */
@CustomLog
public class BootstrapLauncherTransformer extends AbstractClassTransformer {
    public BootstrapLauncherTransformer() {
        super("<matches any class by overriding the match method>");
    }

    @Override
    protected boolean matches(EntryStream stream) {
        return stream.getEntry().getName().endsWith(".class")
            && !stream.getEntry().getName().endsWith("module-info.class");
    }

    @Override
    public boolean matches(Target target) {
        return target.getPath().toLowerCase(Locale.ENGLISH).contains("bootstraplauncher")
            || target.getPath().toLowerCase(Locale.ENGLISH).contains("modlauncher")
            || target.getPath().toLowerCase(Locale.ENGLISH).contains("securejarhandler");
    }

    @Override
    public void transform(ClassNode cn) {
        // TOP TEN WORST IDEAS #8 THIS!
        log.debug("Transforming " + cn.name);
        for (MethodNode mn: cn.methods) {
            for (AbstractInsnNode insn : mn.instructions) {
                if (insn instanceof MethodInsnNode) {
                    MethodInsnNode bootCall = (MethodInsnNode) insn;
                    if ("java/lang/ModuleLayer".equals(bootCall.owner)
                        && "boot".equals(bootCall.name)
                        && "()Ljava/lang/ModuleLayer;".equals(bootCall.desc)
                        && bootCall.getOpcode() == Opcodes.INVOKESTATIC) {
                        log.debug("Found ModuleLayer.boot call in " + cn.name + "." + mn.name + mn.desc);
                        InsnList il = new InsnList();
                        il.add(new LdcInsnNode(Type.getType("Lcpw/mods/cl/ModuleClassLoader;")));
                        il.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, Type.getInternalName(Class.class), "getModule", "()Ljava/lang/Module;"));
                        il.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Module", "getLayer", "()Ljava/lang/ModuleLayer;"));
                        mn.instructions.insert(insn, il);
                        mn.instructions.remove(insn);
                    } else if ("java/lang/ClassLoader".equals(bootCall.owner)
                        && "getPlatformClassLoader".equals(bootCall.name)
                        && "()Ljava/lang/ClassLoader;".equals(bootCall.desc)
                        && bootCall.getOpcode() == Opcodes.INVOKESTATIC) {
                        log.debug("Found ClassLoader.getPlatformClassLoader call in " + cn.name + "." + mn.name + mn.desc);
                        InsnList il = new InsnList();
                        il.add(new LdcInsnNode(Type.getType("Lcpw/mods/cl/ModuleClassLoader;")));
                        il.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, Type.getInternalName(Class.class), "getClassLoader", "()Ljava/lang/ClassLoader;"));
                        mn.instructions.insert(insn, il);
                        mn.instructions.remove(insn);
                    }
                }
            }
        }
    }

}
