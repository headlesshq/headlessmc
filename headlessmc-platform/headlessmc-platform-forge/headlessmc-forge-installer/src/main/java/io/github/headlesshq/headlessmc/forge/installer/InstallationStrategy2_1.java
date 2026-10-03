package io.github.headlesshq.headlessmc.forge.installer;

import net.minecraftforge.installer.SimpleInstaller;
import net.minecraftforge.installer.actions.ClientInstall;
import net.minecraftforge.installer.actions.ProgressCallback;
import net.minecraftforge.installer.json.InstallV1;
import net.minecraftforge.installer.json.Util;

import java.io.File;
import java.util.function.Predicate;

/**
 * Installation strategy for the 2.1 versions of the forge installer.
 */
public class InstallationStrategy2_1 implements InstallationStrategy {
    @Override
    public void install(File target) throws Exception {
        SimpleInstaller.headless = true;
        InstallV1 install = Util.loadInstallProfile();
        ProgressCallback monitor = ProgressCallback.withOutputs(System.out);
        ClientInstall2_1 installer = new ClientInstall2_1(install, monitor);
        File jar = new File(SimpleInstaller.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        installer.run(target, ignored -> true, jar);
    }

    @Override
    public boolean isUsable() {
        try {
            ClientInstall.class.getMethod("run", File.class, Predicate.class, File.class);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static final class ClientInstall2_1 extends ClientInstall {
        public ClientInstall2_1(InstallV1 profile, ProgressCallback monitor) {
            super(profile, monitor);
        }

        @Override
        public boolean run(File target, Predicate<String> optionals, File installer) {
            return super.run(target, optionals, installer);
        }
    }

}
