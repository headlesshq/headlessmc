package io.github.headlesshq.headlessmc.lwjgl.util;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Type;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DescriptionUtilTest extends AbstractUtilityTest<DescriptionUtil> {
    @Test
    public void testGetDescMethod() throws NoSuchMethodException {
        Method method = DescriptionUtilTest.class.getDeclaredMethod(
            "testMethod", boolean.class, byte.class, short.class, int.class,
            long.class, double.class, float.class, char.class, String.class);
        String expected = method.getName() + Type.getMethodDescriptor(method);
        String actual = DescriptionUtil.getDesc(method);
        assertEquals(expected, actual);
    }

    @Test
    public void testGetDescArray() {
        String expected = Type.getDescriptor(DescriptionUtilTest.class);
        String actual = DescriptionUtil.getDesc(DescriptionUtilTest.class);
        assertEquals(expected, actual);
    }

    @Test
    public void testGetDescOfArray() {
        Class<?> s1 = String[].class;
        assertEquals("[Ljava/lang/String;", DescriptionUtil.getDesc(s1));
        Class<?> s2 = String[][].class;
        assertEquals("[[Ljava/lang/String;", DescriptionUtil.getDesc(s2));
        Class<?> s3 = int[][][].class;
        assertEquals("[[[I", DescriptionUtil.getDesc(s3));
    }

    @SuppressWarnings("unused")
    private void testMethod(boolean b, byte by, short s, int i, long l,
                            double d, float f, char ch, String string) {
        throw new UnsupportedOperationException(
            "This method is used by the testGetDesc_Method test.");
    }

}
