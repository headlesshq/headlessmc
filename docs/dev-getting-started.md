# Getting started

HeadlessMc can also be used as a library.

#### Installation

HeadlessMc is published on Maven Central, so no additional repositories are needed.
Depending on your build tool, add the following settings.
`headlessmc-bom` pulls in the core modules and all platforms (vanilla, fabric, forge, neoforge, ...),
`headlessmc-launcher-client` contains the `ClientLauncher`.

=== "Maven"

    ``` xml
    <dependency>
        <groupId>io.github.headlesshq</groupId>
        <artifactId>headlessmc-bom</artifactId>
        <version>$VERSION</version>
    </dependency>
    <dependency>
        <groupId>io.github.headlesshq</groupId>
        <artifactId>headlessmc-launcher-client</artifactId>
        <version>$VERSION</version>
    </dependency>
    ```

=== "Gradle"

    ``` groovy
    dependencies {
        implementation 'io.github.headlesshq:headlessmc-bom:$VERSION'
        implementation 'io.github.headlesshq:headlessmc-launcher-client:$VERSION'
    }
    ```

#### Installing and launching the game

HeadlessMc is built with [Quarkus](https://quarkus.io/),
all of its services are CDI beans which you can inject into your own application.
The following example installs Fabric for Minecraft 26.3
and launches it headlessly with an offline account:

``` java
import io.github.headlesshq.headlessmc.auth.Account;
import io.github.headlesshq.headlessmc.files.McFilesService;
import io.github.headlesshq.headlessmc.java.launcher.ProcessHandler;
import io.github.headlesshq.headlessmc.launcher.client.ClientLauncher;
import io.github.headlesshq.headlessmc.launcher.process.McProcess;
import io.github.headlesshq.headlessmc.launcher.profile.Profile;
import io.github.headlesshq.headlessmc.launcher.profile.ProfileService;
import io.github.headlesshq.headlessmc.platform.PlatformService;
import io.github.headlesshq.headlessmc.platform.VersionID;
import io.github.headlesshq.headlessmc.platform.client.ClientSupport;
import io.github.headlesshq.headlessmc.util.typemap.TypedMapImpl;
import io.github.headlesshq.headlessmc.version.arg.VersionArg;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import jakarta.inject.Inject;

import java.util.List;

@QuarkusMain
public class FabricLauncher implements QuarkusApplication {
    @Inject PlatformService platformService;
    @Inject ProfileService profileService;
    @Inject ClientLauncher clientLauncher;
    @Inject McFilesService mcFilesService;

    public static void main(String[] args) {
        Quarkus.run(FabricLauncher.class, args);
    }

    @Override
    public int run(String... args) {
        VersionID id = VersionID.resolve(platformService, VersionArg.parse("fabric", "26.3"));

        // install fabric 26.3 into the primary .minecraft directory
        id.getPlatform()
            .getClientSupport()
            .map(ClientSupport::installer)
            .orElseThrow()
            .installClient(id, mcFilesService.getPrimaryMcFiles().getMcDir(), new TypedMapImpl());

        // launch with the patchers used by "launch --headless"
        Profile profile = profileService.getProfile(id.asArg())
            .withPatchers(List.of("log4j", "lwjgl", "paulscode"));
        McProcess process = clientLauncher.launcher(profile, Account.defaultOfflineAccount())
            .launch(false);

        return process.waitFor(ProcessHandler.defaultHandler());
    }
}
```

Installing explicitly is optional,
the `ClientLauncher` installs missing versions automatically,
unless `hmc.launcher.auto-install` is set to `false`.
