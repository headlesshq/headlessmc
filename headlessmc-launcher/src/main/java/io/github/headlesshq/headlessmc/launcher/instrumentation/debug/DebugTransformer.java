package io.github.headlesshq.headlessmc.launcher.instrumentation.debug;

import io.github.headlesshq.headlessmc.launcher.instrumentation.AbstractClassTransformer;
import io.github.headlesshq.headlessmc.launcher.instrumentation.EntryStream;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.io.PrintStream;
import java.lang.reflect.Modifier;

import static org.objectweb.asm.Opcodes.*;

/**
 * A transformer that prints the name of every method that is called.
 */
public class DebugTransformer extends AbstractClassTransformer {
    private final long debugTime;
    private final boolean enabled;
    private final boolean onlyCurrentCall;

    public DebugTransformer() {
        super("");
        this.debugTime = Long.parseLong(System.getProperty("hmc.debug.transformer.wait.time", "0"));
        this.enabled = Boolean.parseBoolean(System.getProperty("hmc.debug.transformer.enabled", "true"));
        this.onlyCurrentCall = Boolean.parseBoolean(System.getProperty("hmc.debug.transformer.only.current.call", "false"));
    }

    @Override
    protected void transform(ClassNode cn) {
        if (!enabled) {
            return;
        }

        for (MethodNode mn : cn.methods) {
            if (Modifier.isAbstract(mn.access) || Modifier.isNative(mn.access) || "<init>".equals(mn.name) || "<clinit>".equals(mn.name)) {
                continue;
            }

            AbstractInsnNode node = mn.instructions.getFirst();
            AbstractInsnNode last = null;
            if (!onlyCurrentCall) {
                while (node != null) {
                    if (node instanceof MethodInsnNode) {
                        InsnList debug = new InsnList();
                        debug.add(new FieldInsnNode(GETSTATIC, Type.getInternalName(System.class), "out", Type.getDescriptor(PrintStream.class)));
                        debug.add(new LdcInsnNode("Calling: " + ((MethodInsnNode) node).owner + "." + ((MethodInsnNode) node).name + ((MethodInsnNode) node).desc
                                + " from " + cn.name + "." + mn.name + mn.desc));
                        debug.add(new MethodInsnNode(INVOKEVIRTUAL, Type.getInternalName(PrintStream.class), "println", "(Ljava/lang/String;)V", false));
                        if (debugTime != 0L) {
                            debug.add(new LdcInsnNode(debugTime));
                            debug.add(new MethodInsnNode(INVOKESTATIC, Type.getInternalName(Thread.class), "sleep", "(J)V", false));
                        }

                        if (last == null) {
                            mn.instructions.insert(debug);
                        } else {
                            mn.instructions.insert(last, debug);
                        }
                    }

                    last = node;
                    node = node.getNext();
                }
            }

            InsnList debug = new InsnList();
            debug.add(new FieldInsnNode(GETSTATIC, Type.getInternalName(System.class), "out", Type.getDescriptor(PrintStream.class)));
            debug.add(new LdcInsnNode(cn.name + "." + mn.name + mn.desc));
            debug.add(new MethodInsnNode(INVOKEVIRTUAL, Type.getInternalName(PrintStream.class), "println", "(Ljava/lang/String;)V", false));
            if (debugTime != 0L) {
                debug.add(new LdcInsnNode(debugTime));
                debug.add(new MethodInsnNode(INVOKESTATIC, Type.getInternalName(Thread.class), "sleep", "(J)V", false));
            }

            mn.instructions.insert(debug);
            //mn.visitMaxs(0, 0);
        }
    }

    @Override
    protected boolean matches(EntryStream stream) {
        return true;
    }

}
