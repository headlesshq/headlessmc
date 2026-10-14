package io.github.headlesshq.headlessmc.patcher.paulscode;

import io.github.headlesshq.headlessmc.patcher.PatchResult;
import io.github.headlesshq.headlessmc.patcher.asm.FakePatchContext;
import io.github.headlesshq.headlessmc.patcher.asm.TestClasses;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;

import static org.junit.jupiter.api.Assertions.*;

class PaulsCodePatcherTest {
    private static final String LIBRARY = "paulscode/sound/Library";
    private static final List<String> CLEARED = List.of(
        "message(Ljava/lang/String;)V",
        "importantMessage(Ljava/lang/String;)V",
        "errorCheck(ZLjava/lang/String;)V",
        "errorMessage(Ljava/lang/String;)V",
        "printStackTrace(Ljava/lang/Exception;)V"
    );

    @TempDir
    Path root;

    /** Creates a class with the given methods, each printing something to System.out. */
    private static byte[] classWithMethods(String internalName, List<String> methods) {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        writer.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, internalName, null, "java/lang/Object", null);
        for (String method : methods) {
            int descStart = method.indexOf('(');
            MethodVisitor mv = writer.visitMethod(
                Opcodes.ACC_PUBLIC, method.substring(0, descStart), method.substring(descStart), null, null
            );
            mv.visitCode();
            mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
            mv.visitLdcInsn(method);
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
            mv.visitInsn(Opcodes.RETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        writer.visitEnd();
        return writer.toByteArray();
    }

    private static ClassNode read(byte[] bytes) {
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, 0);
        return node;
    }

    private static ClassNode read(JarFile jar, String entry) throws IOException {
        try (InputStream in = jar.getInputStream(jar.getEntry(entry))) {
            return read(in.readAllBytes());
        }
    }

    private static MethodNode method(ClassNode node, String nameAndDesc) {
        return node.methods.stream()
            .filter(m -> (m.name + m.desc).equals(nameAndDesc))
            .findFirst()
            .orElseThrow();
    }

    private static boolean onlyReturns(MethodNode method) {
        AbstractInsnNode[] instructions = method.instructions.toArray();
        return instructions.length == 1 && instructions[0].getOpcode() == Opcodes.RETURN;
    }

    @Test
    void messageMethodsAreCleared() {
        List<String> methods = new java.util.ArrayList<>(CLEARED);
        methods.add("keep(Ljava/lang/String;)V");
        // same name, different descriptor
        methods.add("message(I)V");
        ClassNode node = read(classWithMethods(LIBRARY, methods));

        new PaulsCodePatcher().patch(node);

        for (String cleared : CLEARED) {
            assertTrue(onlyReturns(method(node, cleared)), cleared);
        }

        assertFalse(onlyReturns(method(node, "keep(Ljava/lang/String;)V")));
        assertFalse(onlyReturns(method(node, "message(I)V")));
    }

    @Test
    void onlyTheSoundSystemLibraryClassMatches() {
        PaulsCodePatcher patcher = new PaulsCodePatcher();

        assertTrue(patcher.matches("paulscode/sound/Library.class"));
        assertFalse(patcher.matches("paulscode/sound/SoundSystem.class"));
        assertFalse(patcher.matches("paulscode/sound/libraries/LibraryLWJGLOpenAL.class"));
        assertFalse(patcher.matches("Library.class"));
    }

    @Test
    void onlySoundSystemLibrariesArePatched() {
        PaulsCodePatcher patcher = new PaulsCodePatcher();

        assertTrue(patcher.shouldPatch(Path.of("libraries/com/paulscode/soundsystem/20120107/soundsystem-20120107.jar")));
        assertTrue(patcher.shouldPatch(Path.of("libraries/com/PaulsCode/SoundSystem/1/SoundSystem-1.jar")));
        assertFalse(patcher.shouldPatch(Path.of("libraries/com/paulscode/codecjorbis/20101023/codecjorbis-20101023.jar")));
        assertFalse(patcher.shouldPatch(Path.of("libraries/org/lwjgl/lwjgl/3.3.3/lwjgl-3.3.3.jar")));
    }

    @Test
    void nameAndVersion() {
        PaulsCodePatcher patcher = new PaulsCodePatcher();

        assertEquals("paulscode", patcher.name());
        assertEquals(0L, patcher.version());
    }

    @Test
    void patchesTheSoundSystemJar() throws IOException {
        Path jar = TestClasses.writeJar(
            root.resolve("libraries/com/paulscode/soundsystem/20120107/soundsystem-20120107.jar"),
            Map.of(
                LIBRARY + ".class", classWithMethods(LIBRARY, CLEARED),
                "paulscode/sound/SoundSystem.class", classWithMethods("paulscode/sound/SoundSystem", CLEARED)
            )
        );
        Path other = TestClasses.writeJar(root.resolve("libraries/other.jar"), Map.of(
            LIBRARY + ".class", classWithMethods(LIBRARY, CLEARED)
        ));
        PatchResult patchResult = new PatchResult(new LinkedHashSet<>(List.of(jar, other)), new LinkedHashSet<>());
        FakePatchContext context = new FakePatchContext(root.resolve("patched"), patchResult, List.of());

        new PaulsCodePatcher().patch(context);

        Path patched = root.resolve("patched/paulscode/soundsystem-20120107.jar");
        assertEquals(List.of(other, patched), List.copyOf(context.getCurrentPatchResult().files()));
        try (JarFile result = new JarFile(patched.toFile())) {
            ClassNode library = read(result, LIBRARY + ".class");
            for (String cleared : CLEARED) {
                assertTrue(onlyReturns(method(library, cleared)), cleared);
            }

            ClassNode soundSystem = read(result, "paulscode/sound/SoundSystem.class");
            for (String method : CLEARED) {
                assertFalse(onlyReturns(method(soundSystem, method)), method);
            }
        }
    }

}
