package io.github.headlesshq.headlessmc.api.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ReflectionUtilTest {
    @Test
    public void testReflectionUtil_iterate() {
        Class<?> clazz = ReflectionUtil.iterate(SubClass.class, c -> c);
        Assertions.assertEquals(SubClass.class, clazz);

        clazz = ReflectionUtil.iterate(SubClass.class, c -> null);
        Assertions.assertNull(clazz);

        boolean[] found = new boolean[]{false};
        clazz = ReflectionUtil.iterate(SubClass.class, c -> {
            if (found[0]) {
                return c;
            }
            found[0] = true;
            return null;
        });
        Assertions.assertEquals(SuperClass.class, clazz);
    }

    private static class SuperClass {
    }

    private static final class SubClass extends SuperClass {
    }

}
