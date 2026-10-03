package io.github.headlesshq.headlessmc.patcher.asm;

import io.github.headlesshq.headlessmc.patcher.PatchContext;
import io.github.headlesshq.headlessmc.patcher.Patcher;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.jar.JarEntry;

@Slf4j
public abstract class AbstractClassPatcher implements Patcher, ClassPatcher {
    @Override
    public abstract void patch(ClassNode classNode);

    @Override
    public boolean matches(String entry) {
        return entry.endsWith(".class") && !entry.endsWith("module-info.class");
    }

    @Override
    public void patch(PatchContext context) {
        for (Path library : context.getCurrentClasspath().files()) {
            if (shouldPatch(library)) {
                log.info("{}: patching library {}", name(), library);
                patch(context, library);
            }
        }
    }

    protected void patch(PatchContext context, Path library) {
        context.patch(library, this, (source, destination) -> {
            boolean changed = false;
            for (Enumeration<JarEntry> e = source.entries(); e.hasMoreElements(); ) {
                JarEntry entry = e.nextElement();
                String name = entry.getName();
                if (skip(name)) {
                    changed = true;
                    continue;
                }

                try (InputStream inputStream = source.getInputStream(entry)) {
                    InputStream streamToCopy = patchEntry(context, inputStream, name);
                    if (streamToCopy == null) {
                        streamToCopy = inputStream;
                    } else {
                        changed = true;
                    }

                    destination.putNextEntry(new JarEntry(name));
                    streamToCopy.transferTo(destination);
                    destination.flush();
                    destination.closeEntry();
                }
            }

            return changed;
        });
    }

    protected @Nullable InputStream patchEntry(PatchContext context, InputStream inputStream, String name)
        throws IOException {
        if (matches(name)) {
            ClassReader reader = new ClassReader(inputStream);
            ClassNode classNode = new ClassNode();
            reader.accept(classNode, 0);
            patch(classNode);
            EntryClassWriter classWriter = new EntryClassWriter(context);
            classNode.accept(classWriter);
            return new ByteArrayInputStream(classWriter.toByteArray());
        }

        return null;
    }

    protected boolean shouldPatch(Path library) {
        return false;
    }

    protected String classFileAsClassName(String classFileName) {
        if (classFileName.endsWith(".class")) {
            return classFileName.substring(0, classFileName.length() - 6);
        }

        return classFileName;
    }

}
