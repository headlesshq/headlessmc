package io.github.headlesshq.headlessmc.launcher.args;

public class VmArgs {
    // Default args of official launcher for e.g. 1.6.4:
    //-Xmx2G
    //-XX:+UnlockExperimentalVMOptions
    //-XX:+UseG1GC
    //-XX:G1NewSizePercent=20
    //-XX:G1ReservePercent=20
    //-XX:MaxGCPauseMillis=50
    //-XX:G1HeapRegionSize=32M

    // TODO: system property added:
    // -Dminecraft.client.jar=/.minecraft/versions/1.4.5/1.4.5.jar
    // TODO: native lib path, how is hash calculated?
    // -Djava.library.path=/.minecraft/bin/d5972e8f89c74287ae5d407aedc944bfed695ffc

}
