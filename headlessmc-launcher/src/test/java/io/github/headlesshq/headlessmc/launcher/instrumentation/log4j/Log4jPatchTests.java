package io.github.headlesshq.headlessmc.launcher.instrumentation.log4j;

import io.github.headlesshq.headlessmc.launcher.instrumentation.Target;
import lombok.SneakyThrows;
import lombok.val;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.lookup.Interpolator;
import org.apache.logging.log4j.core.lookup.JndiLookup;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Field;

import static io.github.headlesshq.headlessmc.launcher.instrumentation.InstrumentationTestUtils.instrument;
import static org.junit.jupiter.api.Assertions.*;

public class Log4jPatchTests extends AbstractUtilityTest<Patchers> {
    @Test
    @SneakyThrows
    public void testInstrumentInterpolator() {
        val transformer = Patchers.LOOKUP;
        assertFalse(transformer.matches(new Target(false, "test")));
        assertTrue(transformer.matches(new Target(false, "log4j")));
        Class<?> clazz = instrument(Interpolator.class, transformer);
        testClass(clazz, false);
    }

    @Test
    @SneakyThrows
    public void testInstrumentJndi() {
        val transformer = Patchers.JNDI;
        assertFalse(transformer.matches(new Target(false, "test")));
        assertTrue(transformer.matches(new Target(false, "log4j")));
        Class<?> clazz =  instrument(JndiLookup.class, transformer);
        // unsafe is necessary because the jndi constructor throws an Exception.
        testClass(clazz, true);
    }

    @SneakyThrows
    private void testClass(Class<?> clazz, boolean unsafe) {
        val obj = unsafe
            ? initWithUnsafe(clazz)
            : clazz.getConstructor().newInstance();
        val result = clazz.getMethod("lookup", LogEvent.class, String.class)
                          .invoke(obj, null, "t");
        assertEquals("HeadlessMc prevented Log4j lookup!", result);
    }

    @SneakyThrows
    private Object initWithUnsafe(Class<?> clazz) {
        Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
        theUnsafe.setAccessible(true);
        Unsafe unsafe = (Unsafe) theUnsafe.get(null);
        return unsafe.allocateInstance(clazz);
    }

}