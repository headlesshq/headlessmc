package io.github.headlesshq.headlessmc.lwjgl.transformer;

import io.github.headlesshq.headlessmc.lwjgl.api.Redirection;
import io.github.headlesshq.headlessmc.lwjgl.api.RedirectionApi;
import io.github.headlesshq.headlessmc.lwjgl.api.Transformer;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Locale;

import static org.objectweb.asm.Opcodes.*;

/**
 * A given {@link ClassNode} will be transformed in the following ways:
 * <p>-if it's a module a {@code requires headlessmc.lwjgl} will be added.
 * <p>-the no-args constructor will be made public or created if necessary
 * <p>-Every method body will have its code removed and will call
 * {@link RedirectionApi#invoke(Object, String, Class, Object...)}.
 * <p>-the constructor will keep its body, a {@link RedirectionApi#invoke(Object,
 * String, Class, Object...)} will be added before all RETURN instructions in
 * the constructor. Class casts in constructors will be redirected.
 * <p>-if the class is abstract and not an interface the abstract modifier
 * will be removed.
 * <p>-All abstract and native methods will be turned into normal methods, with
 * their body transformed as described above.
 */
public class LwjglTransformer implements Transformer {
    /**
     * {@code org.lwjgl.system.Configuration} is a pure configuration registry:
     * its {@code public static final Configuration<?>} option fields are
     * assigned in the static initializer and it makes no native calls. If we
     * transform it like every other lwjgl class its static initializer gets
     * gutted, leaving those fields {@code null}. Minecraft 26.2's {@code
     * com.mojang.blaze3d.platform.NativeLibrariesBootstrap} reads {@code
     * Configuration.SHARED_LIBRARY_EXTRACT_PATH} while loading native libraries,
     * which then throws a {@link NullPointerException} and crashes the game
     * before it starts. Leaving the class untouched lets it behave exactly as it
     * does without the agent (i.e. as it already does in a real, non-headless
     * launch), while every actual native/GL class stays stubbed.
     */
    private static final String CONFIGURATION = "org/lwjgl/system/Configuration";

    @Override
    public void transform(ClassNode cn) {
        if (CONFIGURATION.equals(cn.name)) {
            return;
        }

        try {
            transformModule(cn);
        } catch (NoSuchFieldError ignored) {
            // If we run this Transformer via the LaunchWrapper we could be on
            // an older ASM version which doesnt have the module field yet.
        }

        if ((cn.access & ACC_MODULE) == 0) {
            patchClass(cn, (cn.access & ACC_INTERFACE) != 0);
            // make all non-static fields non-final
            for (FieldNode fn : cn.fields) {
                if ((fn.access & ACC_STATIC) == 0) {
                    fn.access &= ~ACC_FINAL;
                }
            }
        }
    }

    private void transformModule(ClassNode cn) {
        if (cn.module != null) {
            cn.module.visitRequire("headlessmc.lwjgl", ACC_MANDATED, null);
            cn.module.access |= ACC_OPEN;
            cn.module.opens = null; // Forge: InvalidModuleDescriptorException: The opens table for an open module must be 0 length
        }
    }

    private Type injectRedirection(ClassNode cn, MethodNode mn, InsnList il) {
        boolean isStatic = Modifier.isStatic(mn.access);
        if (isStatic) {
            il.add(new LdcInsnNode(Type.getType("L" + cn.name + ";")));
        } else {
            il.add(new VarInsnNode(ALOAD, 0));
        }

        il.add(new LdcInsnNode("L" + cn.name + ";" + mn.name + mn.desc));
        Type returnType = Type.getReturnType(mn.desc);
        il.add(InstructionUtil.loadType(returnType));
        loadArgArray(mn.desc, il, isStatic);

        il.add(new MethodInsnNode(
            INVOKESTATIC, Type.getInternalName(RedirectionApi.class),
            Redirection.METHOD_NAME, Redirection.METHOD_DESC));

        il.add(InstructionUtil.unbox(returnType));
        return returnType;
    }

    private void patchClass(ClassNode cn, boolean isInterface) {
        boolean shouldAddNoArgsCtr = true;
        // TODO: while we can implement all abstract methods which are directly
        //  present in the class, methods inherited from an interface or another
        //  abstract class may pose a problem.
        //  We could at least warn every time a formerly abstract class is being
        //  instantiated?
        if (!isInterface) {
            cn.access &= ~ACC_ABSTRACT;
        }

        for (MethodNode mn : cn.methods) {
            if (mn.name.equals("<init>") && mn.desc.equals("()V")) {
                // TODO: check this
                mn.access |= ACC_PUBLIC;
                mn.access &= ~(ACC_PROTECTED | ACC_PRIVATE);
                shouldAddNoArgsCtr = false;
            }

            if (!isInterface
                || (mn.access & ACC_STATIC) != 0
                || (mn.access & ACC_ABSTRACT) == 0) {
                redirect(mn, cn);
            }
        }

        // TODO: super class containing lwjgl is no guarantee for it to
        //  have a default constructor!
        if (shouldAddNoArgsCtr
            && !isInterface
            && (cn.superName == null
            || cn.superName.toLowerCase(Locale.ENGLISH).contains("lwjgl")
            || cn.superName.equals(Type.getInternalName(Object.class)))) {
            // Add NoArgs Constructor for the ObjectRedirection
            MethodNode mn = new MethodNode(ACC_PUBLIC, "<init>", "()V",
                                           null, new String[0]);
            InsnList il = new InsnList();
            mn.instructions = il;
            il.add(new VarInsnNode(ALOAD, 0));
            il.add(new MethodInsnNode(INVOKESPECIAL, cn.superName, "<init>",
                                      "()V", false));
            injectRedirection(cn, mn, il);
            il.add(new InsnNode(RETURN));
            cn.methods.add(mn);
            mn.visitMaxs(0, 0);
        }
    }

    private void redirect(MethodNode mn, ClassNode cn) {
        if ("<init>".equals(mn.name) && mn.desc.endsWith(")V")) {
            // There's not really a way to determine when this() or super()
            // is called in a constructor, because of that we'll just inject
            // the RedirectionManager at every RETURN instruction.
            // This hasn't become a problem, yet...
            AbstractInsnNode insnNode = mn.instructions.getFirst();
            while (insnNode != null) {
                if (insnNode instanceof TypeInsnNode
                    && insnNode.getOpcode() == CHECKCAST) {
                    mn.instructions.insertBefore(
                        insnNode, redirectCast((TypeInsnNode) insnNode));
                } else if (insnNode.getOpcode() == RETURN) {
                    InsnList il = new InsnList();
                    injectRedirection(cn, mn, il);
                    mn.instructions.insertBefore(insnNode, il);
                }

                insnNode = insnNode.getNext();
            }
        } else {
            InsnList il = new InsnList();
            il.add(InstructionUtil.makeReturn(injectRedirection(cn, mn, il)));
            mn.instructions = il;
        }

        mn.tryCatchBlocks = new ArrayList<>();
        mn.localVariables = new ArrayList<>();
        mn.parameters = new ArrayList<>();
        mn.access = mn.access & ~ACC_NATIVE & ~ACC_ABSTRACT;
    }

    private InsnList redirectCast(TypeInsnNode cast) {
        InsnList il = new InsnList();
        il.add(new LdcInsnNode(Redirection.CAST_PREFIX + cast.desc));
        // TODO: does this really cover all cases?
        if (cast.desc.startsWith("[")) {
            il.add(new LdcInsnNode(Type.getType(cast.desc)));
        } else {
            il.add(new LdcInsnNode(Type.getType("L" + cast.desc + ";")));
        }

        loadArgArray("()V", il, false); // create empty array
        il.add(new MethodInsnNode(
            INVOKESTATIC, Type.getInternalName(RedirectionApi.class),
            Redirection.METHOD_NAME, Redirection.METHOD_DESC));
        return il;
    }

    private void loadArgArray(String desc, InsnList il, boolean isStatic) {
        Type[] args = Type.getArgumentTypes(desc);
        il.add(new LdcInsnNode(args.length));
        il.add(new TypeInsnNode(ANEWARRAY, Type.getInternalName(Object.class)));
        for (int i = 0, v = isStatic ? 0 : 1; i < args.length; i++, v++) {
            il.add(new InsnNode(DUP));

            Type type = args[i];
            il.add(new LdcInsnNode(i));
            il.add(InstructionUtil.loadParam(type, v));
            if (type.getSort() == Type.DOUBLE || type.getSort() == Type.LONG) {
                // double and long take up two registers, so we skip one
                v++;
            }

            MethodInsnNode boxing = InstructionUtil.box(type);
            if (boxing != null) {
                il.add(boxing);
            }

            il.add(new InsnNode(AASTORE));
        }
    }
}
