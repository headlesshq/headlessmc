package io.github.headlesshq.headlessmc.patcher.paulscode;

import io.github.headlesshq.headlessmc.patcher.asm.AbstractClassPatcher;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Mcs SoundManager spams us with some errorMessages if we use the -lwjgl
 * flag. This Transformer removes those messages.
 */
@Slf4j
@Dependent
public class PaulsCodePatcher extends AbstractClassPatcher {
    private final Set<String> methods = new HashSet<>();

    @Inject
    public PaulsCodePatcher() {
        methods.add("message(Ljava/lang/String;)V");
        methods.add("importantMessage(Ljava/lang/String;)V");
        methods.add("errorCheck(ZLjava/lang/String;)V");
        methods.add("errorMessage(Ljava/lang/String;)V");
        methods.add("printStackTrace(Ljava/lang/Exception;)V");
    }

    @Override
    public void patch(ClassNode classNode) {
        for (MethodNode method : classNode.methods) {
            String desc = method.name + method.desc;
            if (methods.contains(desc)) {
                log.debug("Clearing {}", desc);
                method.instructions = new InsnList();
                method.instructions.add(new InsnNode(Opcodes.RETURN));
            }
        }
    }

    @Override
    public boolean matches(String entry) {
        return "paulscode/sound/Library".equals(classFileAsClassName(entry));
    }

    @Override
    protected boolean shouldPatch(Path library) {
        String path = library.toString().toLowerCase(Locale.ENGLISH);
        return path.contains("paulscode") && path.contains("soundsystem");
    }

    @Override
    public String name() {
        return "paulscode";
    }

    @Override
    public long version() {
        return 0;
    }

}
