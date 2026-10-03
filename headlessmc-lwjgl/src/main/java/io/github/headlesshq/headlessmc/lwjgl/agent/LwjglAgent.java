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
            ClassNode node = AsmUtil.read(classfileBuffer);
            transformer.transform(node);
            // TODO: make writer.getClassLoader() return the given loader?
            return AsmUtil.write(node, ClassWriter.COMPUTE_FRAMES);
        }

        return classfileBuffer;
    }

}
