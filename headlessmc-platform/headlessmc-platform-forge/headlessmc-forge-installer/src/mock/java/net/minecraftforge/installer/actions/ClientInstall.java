package net.minecraftforge.installer.actions;

import net.minecraftforge.installer.json.Install;
import net.minecraftforge.installer.json.InstallV1;

import java.io.File;
import java.util.function.Predicate;

@SuppressWarnings("unused")
public class ClientInstall {
    // 2.1.4
    public ClientInstall(InstallV1 profile, ProgressCallback monitor) {
        throw new UnsupportedOperationException("stub");
    }

    // 2.0.24
    public ClientInstall(Install profile, ProgressCallback monitor) {
        throw new UnsupportedOperationException("stub");
    }

    // super legacy: run(File target, com.google.common.base.Predicate<String> optionals) {

    // 2.0.24
    public boolean run(File target, Predicate<String> optionals) {
        throw new UnsupportedOperationException("stub");
    }

    // 2.1.4
    public boolean run(File target, Predicate<String> optionals, File installer) {
        throw new UnsupportedOperationException("stub");
    }

    // 2.2.15
    public boolean run(File target, File installer) {
        throw new UnsupportedOperationException("stub");
    }

}
