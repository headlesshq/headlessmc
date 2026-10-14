package io.github.headlesshq.headlessmc.lwjgl.agent;

import lombok.Generated;
import io.github.headlesshq.headlessmc.lwjgl.api.Transformer;
import io.github.headlesshq.headlessmc.lwjgl.transformer.AsmUtil;
import io.github.headlesshq.headlessmc.lwjgl.transformer.LwjglTransformer;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

/**
 * A JavaAgent calling the {@link LwjglTransformer}.
 */
@Generated // exclude from Jacoco TestCoverage, we cannot get coverage for a javaagent
public class LwjglAgent implements ClassFileTransformer {
    private final Transformer transformer = new LwjglTransformer();

    public static void premain(String args, Instrumentation instrumentation) {
        instrumentation.addTransformer(new LwjglAgent());
    }

    public static void agentmain(String args, Instrumentation instrumentation) {
        instrumentation.addTransformer(new LwjglAgent());
    }

    @Override
    public byte[] transform(ClassLoader loader, String className,
                            Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain,
                            byte[] classfileBuffer)
        throws IllegalClassFormatException {
        if (className != null && className.startsWith("org/lwjgl")) {
            try {
                ClassNode node = AsmUtil.read(classfileBuffer);
                transformer.transform(node);
                // TODO: make writer.getClassLoader() return the given loader?
                return AsmUtil.write(node, ClassWriter.COMPUTE_FRAMES);
            } catch (Throwable t) {
                // The JVM silently discards anything thrown by a ClassFileTransformer and loads
                // the original class, which then fails somewhere far away from the actual cause.
                handleTransformFailure(className, t);
            }
        }

        return classfileBuffer;
    }

    private static void handleTransformFailure(String className, Throwable t) {
        System.err.println("[headlessmc-lwjgl] Failed to transform " + className
            + ", the class will be loaded unpatched! ASM " + getAsmVersion()
            + " is used, make sure that the ASM on the classpath supports Java "
            + System.getProperty("java.specification.version")
            + " and that no older ASM version comes before it.");
        //noinspection CallToPrintStackTrace
        t.printStackTrace();
        if (t instanceof Error && !(t instanceof LinkageError)) {
            throw (Error) t;
        }
    }

    private static String getAsmVersion() {
        Package asmPackage = ClassWriter.class.getPackage();
        String version = asmPackage == null ? null : asmPackage.getImplementationVersion();
        java.security.CodeSource codeSource = ClassWriter.class.getProtectionDomain().getCodeSource();
        return (version == null ? "unknown version" : version)
            + (codeSource == null ? "" : " (" + codeSource.getLocation() + ")");
    }

}
