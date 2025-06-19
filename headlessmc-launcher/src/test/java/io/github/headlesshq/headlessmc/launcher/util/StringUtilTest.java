package io.github.headlesshq.headlessmc.launcher.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class StringUtilTest extends AbstractUtilityTest<StringUtil> {
    @Test
    public void testCutOfEnd() {
        Assertions.assertThrows(IllegalArgumentException.class,
                                () -> StringUtil.cutOfEnd("test", -1));
        Assertions.assertThrows(IllegalArgumentException.class,
                                () -> StringUtil.cutOfEnd("test", 8));
        Assertions.assertEquals("t", StringUtil.cutOfEnd("test", 3));
        Assertions.assertEquals("te", StringUtil.cutOfEnd("test", 2));
        Assertions.assertEquals("tes", StringUtil.cutOfEnd("test", 1));
        Assertions.assertEquals("test", StringUtil.cutOfEnd("test", 0));
    }

}
