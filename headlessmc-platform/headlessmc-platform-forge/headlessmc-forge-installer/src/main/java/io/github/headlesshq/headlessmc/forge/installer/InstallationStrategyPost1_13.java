package io.github.headlesshq.headlessmc.forge.installer;

import net.minecraftforge.installer.SimpleInstaller;
import net.minecraftforge.installer.actions.ClientInstall;
import net.minecraftforge.installer.actions.ProgressCallback;
import net.minecraftforge.installer.json.Install;
import net.minecraftforge.installer.json.Util;

import java.io.File;
import java.lang.reflect.Method;
import java.util.function.Predicate;

/**
 * Installation strategy for older versions of the launcher that are post 1.13
 */
public class InstallationStrategyPost1_13 implements InstallationStrategy {
    @Override
    public void install(File target) throws ReflectiveOperationException {
        SimpleInstaller.headless = true;
        // need to reflectionally call this, Util.loadInstallProfile has different signatures on different versions
        // on older versions it returns Install, on newer InstallV1
        Method installProfile = Util.class.getMethod("loadInstallProfile");
        Install install = (Install) installProfile.invoke(null);
        ProgressCallback monitor = ProgressCallback.withOutputs(System.out);
        ClientInstallPost1_13 installer = new ClientInstallPost1_13(install, monitor);
        installer.run(target, ignored -> true);
    }

    @Override
    public boolean isUsable() {
        try {
            ClientInstall.class.getMethod("run", File.class, Predicate.class);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static final class ClientInstallPost1_13 extends ClientInstall {
        public ClientInstallPost1_13(Install profile, ProgressCallback monitor) {
            super(profile, monitor);
        }

        @Override
        public boolean run(File target, Predicate<String> optionals) {
            return super.run(target, optionals);
        }
    }

}
