package io.github.headlesshq.headlessmc.forge.installer;

import net.minecraftforge.installer.SimpleInstaller;
import net.minecraftforge.installer.actions.ClientInstall;
import net.minecraftforge.installer.actions.ProgressCallback;
import net.minecraftforge.installer.json.InstallV1;
import net.minecraftforge.installer.json.Util;

import java.io.File;

/**
 * Installation strategy for the 2.2 versions of the forge installer.
 */
public class InstallationStrategy2_2 implements InstallationStrategy {
    @Override
    public void install(File target) throws Exception {
        SimpleInstaller.headless = true;
        InstallV1 install = Util.loadInstallProfile();
        ProgressCallback monitor = ProgressCallback.withOutputs(System.out);
        ClientInstall2_2 installer = new ClientInstall2_2(install, monitor);
        File jar = new File(SimpleInstaller.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        installer.run(target, jar);
    }

    @Override
    public boolean isUsable() {
        try {
            ClientInstall.class.getMethod("run", File.class, File.class);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static final class ClientInstall2_2 extends ClientInstall {
        public ClientInstall2_2(InstallV1 profile, ProgressCallback monitor) {
            super(profile, monitor);
        }

        @Override
        public boolean run(File target, File installer) {
            return super.run(target, installer);
        }
    }

}
