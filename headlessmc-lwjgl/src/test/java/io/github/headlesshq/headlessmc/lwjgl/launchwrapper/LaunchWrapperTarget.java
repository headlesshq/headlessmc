package io.github.headlesshq.headlessmc.lwjgl.launchwrapper;

import io.github.headlesshq.headlessmc.lwjgl.LwjglInstrumentationTest;
import org.lwjgl.AbstractLwjglClass;
import org.lwjgl.Lwjgl;
import org.lwjgl.LwjglInterface;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class LaunchWrapperTarget {
    public static void testLwjglClasses() {
        Lwjgl lwjgl = Lwjgl.factoryMethod("test");
        assertNotNull(lwjgl);
        LwjglInstrumentationTest.testRedirections(lwjgl, lwjgl.getClass());

        //noinspection ConstantConditions
        assertNull(AbstractLwjglClass.returnsAbstractByteBuffer("test"));
        AbstractLwjglClass aLwjgl = AbstractLwjglClass.factoryMethod("test");
        assertNotNull(aLwjgl);
        LwjglInstrumentationTest.testRedirections(aLwjgl, aLwjgl.getClass());

        LwjglInterface iLwjgl = LwjglInterface.factoryMethod("test");
        assertNotNull(iLwjgl);
        LwjglInstrumentationTest.testRedirections(iLwjgl, LwjglInterface.class);
    }

}
