package io.github.headlesshq.headlessmc.launcher.command;

import lombok.Data;
import io.github.headlesshq.headlessmc.api.traits.HasName;
import io.github.headlesshq.headlessmc.launcher.LauncherMock;
import io.github.headlesshq.headlessmc.launcher.command.download.VersionArgument;
import io.github.headlesshq.headlessmc.launcher.modlauncher.Modlauncher;
import io.github.headlesshq.headlessmc.launcher.version.Version;
import io.github.headlesshq.headlessmc.launcher.version.family.HasParent;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class AbstractVersionCommandTest {
    @Test
    public void testFindVersionArgument() {
        TestVersion vanilla = new TestVersion("1.12.2");

        TestVersion forge = new TestVersion("forge-1.12.2-26");
        forge.setParent(vanilla);

        List<TestVersion> testVersions = Arrays.asList(vanilla, forge);
        AbstractVersionCommand abstractVersionCommand = new AbstractVersionCommand(LauncherMock.INSTANCE, "test", "test") {
            @Override
            public void execute(Version obj, String... args) {
                throw new UnsupportedOperationException();
            }
        };

        assertEquals(vanilla, abstractVersionCommand.find(new VersionArgument(null, null, "1.12.2"), testVersions));
        assertEquals(forge, abstractVersionCommand.find(new VersionArgument(Modlauncher.LEXFORGE, null, "1.12.2"), testVersions));
        assertEquals(forge, abstractVersionCommand.find(new VersionArgument(Modlauncher.LEXFORGE, null, "forge-1.12.2-26"), testVersions));
        assertEquals(forge, abstractVersionCommand.find(new VersionArgument(Modlauncher.LEXFORGE, "26", "forge-1.12.2-26"), testVersions));

        assertNull(abstractVersionCommand.find(new VersionArgument(null, "26", "forge-1.12.2-26"), testVersions)); // not possible
        assertNull(abstractVersionCommand.find(new VersionArgument(Modlauncher.LEXFORGE, "27", "1.12.2"), testVersions));
        assertNull(abstractVersionCommand.find(new VersionArgument(Modlauncher.NEOFORGE, null, "1.12.2"), testVersions));
        assertNull(abstractVersionCommand.find(new VersionArgument(Modlauncher.FABRIC, null, "1.12.2"), testVersions));
    }

    @Data
    private static class TestVersion implements HasName, HasParent<TestVersion> {
        private final String name;
        private @Nullable TestVersion parent;
    }

}
