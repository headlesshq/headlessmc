package io.github.headlesshq.headlessmc.patcher.lwjgl;

import io.github.headlesshq.headlessmc.lwjgl.transformer.LwjglTransformer;
import io.github.headlesshq.headlessmc.os.CPU;
import io.github.headlesshq.headlessmc.os.OS;
import io.github.headlesshq.headlessmc.os.OSConfigurator;
import io.github.headlesshq.headlessmc.os.OSService;
import io.github.headlesshq.headlessmc.patcher.HelperService;
import io.github.headlesshq.headlessmc.patcher.PatchCache;
import io.github.headlesshq.headlessmc.patcher.PatchContext;
import io.github.headlesshq.headlessmc.patcher.PatchResult;
import io.github.headlesshq.headlessmc.patcher.Patcher;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

// TODO: port HmcLwjglTransformerTest
public class LwjglPatcherTest {
    @Test
    public void jomlUnsafeIsDisabled(@TempDir Path root) {
        TestContext context = new TestContext(root);

        new LwjglPatcher(new LwjglTransformer(), osService()).patch(context);

        assertEquals("true", context.result.systemProperties().get(LwjglPatcher.JOML_NO_UNSAFE));
        assertEquals(
            List.of(root.resolve("headlessmc-lwjgl.jar")),
            List.copyOf(context.result.files()),
            "the headlessmc-lwjgl jar should be added to the classpath"
        );
    }

    private static OSService osService() {
        return new OSService() {
            @Override
            public OS getOS() {
                return new OS("Linux", OS.Type.LINUX, "6.0");
            }

            @Override
            public CPU getCPU() {
                throw new UnsupportedOperationException();
            }

            @Override
            public List<OSConfigurator> getConfigurators() {
                return List.of();
            }
        };
    }

    /** A {@link PatchContext} with an empty classpath, which records what the patcher adds. */
    private static final class TestContext implements PatchContext {
        private final Path root;
        private PatchResult result = new PatchResult(new LinkedHashSet<>(), new LinkedHashSet<>());

        private TestContext(Path root) {
            this.root = root;
        }

        @Override
        public PatchResult getInitialPatchResult() {
            return new PatchResult(new LinkedHashSet<>(), new LinkedHashSet<>());
        }

        @Override
        public PatchResult getCurrentPatchResult() {
            return result;
        }

        @Override
        public OutputStream add(String library, Patcher patcher) throws IOException {
            Path file = root.resolve(library + ".jar");
            result = result.withFile(file);
            return Files.newOutputStream(file);
        }

        @Override
        public OutputStream addAgent(String library, Patcher patcher) throws IOException {
            Path file = root.resolve(library + ".jar");
            result = result.withAgent(file);
            return Files.newOutputStream(file);
        }

        @Override
        public void patch(Path library, Patcher patcher, PatchAction action) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void addSystemProperty(String key, @Nullable String value) {
            result = result.withSystemProperty(key, value);
        }

        @Override
        public List<Patcher> getPatchers() {
            return List.of();
        }

        @Override
        public int getJavaVersion() {
            return 21;
        }

        @Override
        public <C extends HelperService> Stream<C> services(Class<C> type) {
            return Stream.empty();
        }

        @Override
        public PatchCache.Key getCacheKey() {
            throw new UnsupportedOperationException();
        }
    }

    /*
    @Test
    public void testJava27MultiReleaseClass() throws IOException {
        // LWJGL 3.4.3 has Java 27 entries even when Minecraft uses Java 25.
        assertNativeMethodTransformed(71, "META-INF/versions/27/");
    }

    @Test
    public void testJava8Class() throws IOException {
        assertNativeMethodTransformed(V1_8, "");
    }

    private void assertNativeMethodTransformed(int version, String prefix)
        throws IOException {
        String name = "org/lwjgl/system/TestMemoryBackend";
        ClassWriter writer = new ClassWriter(0);
        writer.visit(version, ACC_PUBLIC, name, null, "java/lang/Object", null);
        writer.visitMethod(ACC_PUBLIC | ACC_STATIC | ACC_NATIVE,
                           "address", "()J", null, null).visitEnd();
        writer.visitEnd();

        EntryStream entry = new EntryStream(
            new ByteArrayInputStream(writer.toByteArray()),
            Collections.emptyList(), () -> prefix + name + ".class");
        byte[] transformed = new HmcLwjglTransformer().maybeTransform(entry);
        assertNotNull(transformed);
        ClassNode result = AsmUtil.read(transformed);
        assertEquals(version, result.version);
        assertEquals(name, result.name);
        MethodNode method = result.methods.stream()
            .filter(candidate -> candidate.name.equals("address"))
            .findFirst().orElseThrow(AssertionError::new);
        assertEquals(0, method.access & ACC_NATIVE);
        boolean redirects = false;
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof MethodInsnNode) {
                MethodInsnNode call = (MethodInsnNode) instruction;
                redirects |= call.owner.equals(
                    "io/github/headlesshq/headlessmc/lwjgl/api/RedirectionApi")
                    && call.name.equals("invoke");
            }
        }

        assertTrue(redirects,
                   "Native calls must be redirected for headless execution");
    }
     */
}
