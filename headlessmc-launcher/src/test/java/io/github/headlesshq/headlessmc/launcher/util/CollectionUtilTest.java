package io.github.headlesshq.headlessmc.launcher.util;

import io.github.headlesshq.headlessmc.api.util.AbstractUtilityTest;
import lombok.val;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CollectionUtilTest extends AbstractUtilityTest<CollectionUtil> {
    @Test
    public void testListOf() {
        val first = "first";
        val second = "second";
        val list = CollectionUtil.listOf(first, second);
        Assertions.assertEquals(first, list.get(0));
        Assertions.assertEquals(second, list.get(1));
    }

}
