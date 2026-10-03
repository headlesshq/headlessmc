package io.github.headlesshq.headlessmc.lwjgl.agent;

import io.github.headlesshq.headlessmc.lwjgl.launchwrapper.LaunchWrapperTarget;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.lwjgl.AbstractLwjglClass;

import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.assertFalse;

public class AgentTest {
    @Test
    public void testWithAgent() {
        Assumptions.assumeTrue(Boolean.parseBoolean(System.getProperty("hmc.lwjgl.agenttest")));

        LaunchWrapperTarget.testLwjglClasses();
        assertFalse(Modifier.isAbstract(AbstractLwjglClass.class.getModifiers()));
    }

}
