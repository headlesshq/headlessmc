package io.github.headlesshq.headlessmc.lwjgl;

import io.github.headlesshq.headlessmc.lwjgl.api.RedirectionApi;
import io.github.headlesshq.headlessmc.lwjgl.api.RedirectionManager;
import io.github.headlesshq.headlessmc.lwjgl.redirections.DefaultRedirections;
import io.github.headlesshq.headlessmc.lwjgl.redirections.ObjectRedirection;
import io.github.headlesshq.headlessmc.lwjgl.transformer.LwjglTransformer;
import io.github.headlesshq.headlessmc.lwjgl.util.DescriptionUtil;
import lombok.SneakyThrows;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.lwjgl.AbstractLwjglClass;
import org.lwjgl.Lwjgl;
import org.lwjgl.LwjglClassLoader;
import org.lwjgl.LwjglInterface;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.*;

public class LwjglInstrumentationTest {
    private static final RedirectionManager MANAGER =
        RedirectionApi.getRedirectionManager();

    @SneakyThrows
    public static void testRedirections(Object object, Class<?> clazz) {
        val called = new boolean[]{false};
        for (val method : clazz.getDeclaredMethods()) {
            if (method.getParameterTypes().length > 0
                // when running with coverage methods get added
                || method.getName().contains("$")) {
                continue;
            }

            val descriptor = DescriptionUtil.getDesc(clazz)
                + DescriptionUtil.getDesc(method);
            if (descriptor.endsWith(";toString()Ljava/lang/String;")) {
                continue;
            }

            if (descriptor.endsWith(";hashCode()I") && clazz.isInterface()) {
                method.setAccessible(true);
                assertNotNull(method.invoke(object));
                continue;
            }

            MANAGER.redirect(descriptor, (obj, desc, type, args) -> {
                called[0] = true;
                if (type.isPrimitive()) {
                    val fb = DefaultRedirections.fallback(
                        type, new ObjectRedirection(MANAGER));
                    assertNotNull(fb);
                    return fb.invoke(obj, desc, type, args);
                }

                return null;
            });

            called[0] = false;
            method.setAccessible(true);
            val result = method.invoke(object);
            if (!method.getReturnType().isPrimitive()) {
                assertNull(result, descriptor);
            }

            assertTrue(called[0], descriptor);
        }
    }

    @Test
    @SneakyThrows
    public void testLwjglInterface() {
        assertNull(LwjglInterface.factoryMethod("test"));
        Method aMethod = LwjglInterface.class.getMethod("abstractMethod");
        assertTrue(Modifier.isAbstract(aMethod.getModifiers()));

        val lwjglInterface = load(LwjglInterface.class);

        aMethod = lwjglInterface.getMethod("abstractMethod");
        assertTrue(
            Modifier.isAbstract(aMethod.getModifiers()),
            "Abstract method in an interface should still be abstract");

        val obj = callFactoryMethod(lwjglInterface);
        assertNotNull(obj);
        testRedirections(obj, lwjglInterface);
    }

    @Test
    @SuppressWarnings({"ResultOfMethodCallIgnored", "Convert2MethodRef"})
    public void testLwjglClass() {
        assertNull(Lwjgl.factoryMethod("test"));
        assertThrows(NoSuchMethodException.class, Lwjgl.class::getConstructor);
        val lwjglClass = load(Lwjgl.class);
        assertDoesNotThrow(() -> lwjglClass.getConstructor(),
                           "A no-args constructor should've been added!");
        val obj = callFactoryMethod(lwjglClass);
        assertNotNull(obj);
        testRedirections(obj, lwjglClass);
    }

    @Test
    @SneakyThrows
    public void testArrayReturnValue() {
        val lwjgl = new Lwjgl("test");
        assertNull(lwjgl.returnsByteArray("test"));
        assertNull(lwjgl.returns2dIntArray("test"));

        val lwjglClass = load(Lwjgl.class);
        val obj = callFactoryMethod(lwjglClass);
        assertNotNull(obj);
        Method method = lwjglClass.getMethod("returnsByteArray", String.class);
        method.setAccessible(true);
        Object result = method.invoke(obj, "test");
        assertNotNull(result);
        assertInstanceOf(byte[].class, result);
        assertArrayEquals(new byte[0], (byte[]) result);

        method = lwjglClass.getMethod("returns2dIntArray", String.class);
        method.setAccessible(true);
        result = method.invoke(obj, "test");
        assertNotNull(result);
        assertInstanceOf(int[][].class, result);
        assertArrayEquals(new int[][]{}, (int[][]) result);
    }

    @Test
    @SneakyThrows
    public void testCastRedirection() {
        val lwjglClass = load(Lwjgl.class);
        val ctr = lwjglClass.getConstructor(Object.class);
        ctr.setAccessible(true);

        // MANAGER.redirect("",  (object, desc, type, args) -> byteBuffer);
        val argument = new Object();
        Object lwjglInstance = ctr.newInstance(argument); // not a String!!!
        val field = lwjglClass.getField("string");
        field.setAccessible(true);
        Object value = field.get(lwjglInstance);
        assertEquals("", value); // DefaultRedirection for String

        val called = new boolean[]{false};
        val expected = "test";
        MANAGER.redirect("<init> <cast> java/lang/String",
                         (object, desc, type, args) -> {
                             called[0] = true;
                             assertEquals(argument, object);
                             return expected;
                         });

        lwjglInstance = ctr.newInstance(argument);
        assertTrue(called[0]);
        called[0] = false;
        value = field.get(lwjglInstance);
        assertEquals(expected, value);

        val nextExpected = "test2";
        val called2 = new boolean[]{false};
        MANAGER.redirect("<cast> java/lang/String",
                         (object, desc, type, args) -> {
                             called2[0] = true;
                             assertEquals(argument, object);
                             return nextExpected;
                         });

        lwjglInstance = ctr.newInstance(argument);
        assertTrue(called2[0]);
        value = field.get(lwjglInstance);
        assertEquals(nextExpected, value);

        val called3 = new boolean[]{false};
        MANAGER.redirect(DescriptionUtil.getDesc(lwjglClass)
                                        .concat("<init>(Ljava/lang/Object;)V"),
                         (object, desc, type, args) -> {
                             called3[0] = true;
                             assertEquals(1, args.length);
                             assertEquals(args[0], argument);
                             return null;
                         });

        ctr.newInstance(argument);
        assertTrue(called3[0]);
    }

    @Test
    @SneakyThrows
    public void testLwjglAbstractClass() {
        assertNull(AbstractLwjglClass.factoryMethod("test"));
        assertTrue(Modifier.isAbstract(
            AbstractLwjglClass.class.getModifiers()));
        Method aMethod = AbstractLwjglClass.class.getMethod("abstractMethod");
        assertTrue(Modifier.isAbstract(aMethod.getModifiers()));

        val abstractClass = load(AbstractLwjglClass.class);

        assertFalse(
            Modifier.isAbstract(abstractClass.getModifiers()));
        aMethod = abstractClass.getMethod("abstractMethod");
        assertFalse(
            Modifier.isAbstract(aMethod.getModifiers()),
            "Abstract method in class should get implemented");

        val obj = callFactoryMethod(abstractClass);
        assertNotNull(obj);
        testRedirections(obj, abstractClass);
    }

    @Test
    @SneakyThrows
    public void testAbstractReturnValue() {
        val abstractClass = load(AbstractLwjglClass.class);
        val obj = callFactoryMethod(abstractClass);

        val returnsAbstractByteBuffer = abstractClass.getMethod(
            "returnsAbstractByteBuffer", String.class);
        returnsAbstractByteBuffer.setAccessible(true);
        Object result = returnsAbstractByteBuffer.invoke(obj, "test");
        assertNull(result);

        val descriptor = DescriptionUtil.getDesc(abstractClass)
            + DescriptionUtil.getDesc(returnsAbstractByteBuffer);
        val byteBuffer = ByteBuffer.wrap(new byte[0]);
        MANAGER.redirect(descriptor, (object, desc, type, args) -> byteBuffer);
        result = returnsAbstractByteBuffer.invoke(obj, "test");
        assertNotNull(result);
        assertEquals(byteBuffer, result);
    }

    @SneakyThrows
    private <T> T callFactoryMethod(Class<T> clazz) {
        val method = clazz.getDeclaredMethod("factoryMethod", String.class);
        method.setAccessible(true);
        return clazz.cast(method.invoke(null, "dummy"));
    }

    @SneakyThrows
    private Class<?> load(Class<?> toLoad) {
        val transformer = new LwjglTransformer();
        val lwjglClassLoader = new LwjglClassLoader(transformer);
        return lwjglClassLoader.loadClass(toLoad.getName());
    }

}
