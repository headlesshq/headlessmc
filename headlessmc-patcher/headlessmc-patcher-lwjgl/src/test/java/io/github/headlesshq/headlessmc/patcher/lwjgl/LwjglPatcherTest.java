package io.github.headlesshq.headlessmc.patcher.lwjgl;

// TODO: port HmcLwjglTransformerTest
public class LwjglPatcherTest {
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
