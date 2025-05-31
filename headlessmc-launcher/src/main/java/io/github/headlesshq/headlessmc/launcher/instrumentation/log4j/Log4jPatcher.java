package io.github.headlesshq.headlessmc.launcher.instrumentation.log4j;

import io.github.headlesshq.headlessmc.launcher.instrumentation.AbstractClassTransformer;
import io.github.headlesshq.headlessmc.launcher.instrumentation.Target;
import lombok.CustomLog;
import org.objectweb.asm.tree.*;

import java.util.ArrayList;
import java.util.Locale;
import java.util.stream.Collectors;

import static org.objectweb.asm.Opcodes.ARETURN;

@CustomLog
public class Log4jPatcher extends AbstractClassTransformer {
    public Log4jPatcher(String className) {
        super(className);
    }

    @Override
    public boolean matches(Target target) {
        return target.getPath().toLowerCase(Locale.ENGLISH).contains("log4j");
    }

    @Override
    protected void transform(ClassNode cn) {
        // TODO: check desc of method for ARETURN compatible type
        for (MethodNode mn : cn.methods.stream()
                                       .filter(m -> "lookup".equals(m.name))
                                       .collect(Collectors.toList())) {
            log.info("Patching lookup in " + cn.name);
            InsnList insns = new InsnList();
            insns.add(new LdcInsnNode("HeadlessMc prevented Log4j lookup!"));
            insns.add(new InsnNode(ARETURN));
            mn.instructions = insns;
            mn.tryCatchBlocks = new ArrayList<>();
            mn.localVariables = new ArrayList<>();
            mn.parameters = new ArrayList<>();
            mn.visitMaxs(0, 0);
        }
    }

}
