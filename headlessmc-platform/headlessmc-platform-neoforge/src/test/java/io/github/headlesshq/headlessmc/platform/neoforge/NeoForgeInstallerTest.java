package io.github.headlesshq.headlessmc.platform.neoforge;

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
public class NeoForgeInstallerTest {
    @Inject
    McFiles mcFiles;

    @Inject
    PlatformService platformService;

    @Inject
    @NeoForge
    ClientInstaller forgeInstaller;

    @Test
    @Disabled
    public void testInstall1_20_1() throws HeadlessMcException {
        // TODO: fix VersionMatcher in this case!!! ???!?!?!?!
        VersionArg arg = VersionArg.parse("neoforge", "1.20.1");
        VersionID id = VersionID.resolve(platformService, arg);
        forgeInstaller.installClient(id, mcFiles.getMcDir(), new TypedMapImpl());
    }

    @Test
    @Disabled
    public void testInstall1_20_2() throws HeadlessMcException {
        VersionArg arg = VersionArg.parse("neoforge", "1.20.2");
        VersionID id = VersionID.resolve(platformService, arg);
        forgeInstaller.installClient(id, mcFiles.getMcDir(), new TypedMapImpl());
    }

    @Test
    @Disabled
    public void testInstall26_2() throws HeadlessMcException {
        VersionArg arg = VersionArg.parse("neoforge", "26.2");
        VersionID id = VersionID.resolve(platformService, arg);
        forgeInstaller.installClient(id, mcFiles.getMcDir(), new TypedMapImpl());
    }

}
