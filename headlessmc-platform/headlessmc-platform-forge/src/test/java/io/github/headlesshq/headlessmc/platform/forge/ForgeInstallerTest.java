package io.github.headlesshq.headlessmc.platform.forge;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.files.McFiles;
import io.github.headlesshq.headlessmc.platform.client.ClientInstaller;
import io.github.headlesshq.headlessmc.platform.PlatformService;
import io.github.headlesshq.headlessmc.platform.VersionID;
import io.github.headlesshq.headlessmc.util.typemap.TypedMapImpl;
import io.github.headlesshq.headlessmc.version.arg.VersionArg;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class ForgeInstallerTest {
    @Inject
    McFiles mcFiles;

    @Inject
    PlatformService platformService;

    @Forge
    @Inject
    ClientInstaller forgeInstaller;

    @Test
    @Disabled
    public void testInstall1_8_9() throws HeadlessMcException {
        VersionArg arg = VersionArg.parse("forge", "1.8.9");
        VersionID id = VersionID.resolve(platformService, arg);
        forgeInstaller.installClient(id, mcFiles.getMcDir(), new TypedMapImpl());
    }

    @Test
    @Disabled
    public void testInstall1_12_2() throws HeadlessMcException {
        VersionArg arg = VersionArg.parse("forge", "1.12.2");
        VersionID id = VersionID.resolve(platformService, arg);
        forgeInstaller.installClient(id, mcFiles.getMcDir(), new TypedMapImpl());
    }

    @Test
    @Disabled
    public void testInstall1_21_1() throws HeadlessMcException {
        VersionArg arg = VersionArg.parse("forge", "1.21.1");
        VersionID id = VersionID.resolve(platformService, arg);
        forgeInstaller.installClient(id, mcFiles.getMcDir(), new TypedMapImpl());
    }

    @Test
    @Disabled
    public void testInstall26_2() throws HeadlessMcException {
        VersionArg arg = VersionArg.parse("forge", "26.2");
        VersionID id = VersionID.resolve(platformService, arg);
        forgeInstaller.installClient(id, mcFiles.getMcDir(), new TypedMapImpl());
    }

}
