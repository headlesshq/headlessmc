package io.github.headlesshq.headlessmc.launcher.util;

import lombok.val;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UuidUtilTest extends AbstractUtilityTest<UuidUtil> {
    @Test
    public void testIsUuid() {
        for (int i = 0; i < 10; i++) {
            val uuid = UUID.randomUUID();
            assertTrue(UuidUtil.isUuid(uuid.toString()),
                       uuid + " should be recognized as UUID");
        }

        assertFalse(UuidUtil.isUuid("definitely-not-a-uuid"));
        assertFalse(UuidUtil.isUuid("test"));
    }

}
