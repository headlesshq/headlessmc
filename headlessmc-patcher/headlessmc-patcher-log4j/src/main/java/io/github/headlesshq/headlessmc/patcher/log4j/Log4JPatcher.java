package io.github.headlesshq.headlessmc.patcher.log4j;

import io.github.headlesshq.headlessmc.patcher.PatchContext;
import io.github.headlesshq.headlessmc.patcher.asm.AbstractClassPatcher;
import jakarta.enterprise.context.Dependent;
import lombok.extern.slf4j.Slf4j;
import org.objectweb.asm.tree.ClassNode;

import java.nio.file.Path;
import java.util.Locale;

/**
 * Patches the Log4Shell exploit
 * by removing the exploit class from the classpath.
 */
@Slf4j
@Dependent
public class Log4JPatcher extends AbstractClassPatcher {
    private static final String JNDI = "org/apache/logging/log4j/core/lookup/JndiLookup";

    private boolean ran;

    @Override
    public void patch(PatchContext context) {
        ran = false;
        super.patch(context);
        if (!ran) {
            log.error("Failed to patch Log4Shell exploit");
        }
    }

    @Override
    public void patch(ClassNode classNode) {
        // NOP, we patch by completely skipping the class
    }

    @Override
    public boolean skip(String entry) {
        if (JNDI.equals(classFileAsClassName(entry))) {
            ran = true;
            return true;
        }

        return false;
    }

    @Override
    public boolean matches(String entry) {
        return false;
    }

    @Override
    protected boolean shouldPatch(Path library) {
        return library.toString().toLowerCase(Locale.ENGLISH).contains("log4j");
    }

    @Override
    public String name() {
        return "log4j";
    }

    @Override
    public long version() {
        return 0;
    }

}
