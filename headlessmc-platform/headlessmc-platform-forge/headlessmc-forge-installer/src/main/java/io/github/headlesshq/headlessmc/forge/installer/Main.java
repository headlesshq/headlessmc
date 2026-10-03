package io.github.headlesshq.headlessmc.forge.installer;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        if (args.length < 1) {
            throw new IllegalArgumentException("Invalid arguments: use java -jar file.jar <install-location>");
        } else if (args.length > 1) {
            throw new IllegalArgumentException("Too many arguments: use java -jar file.jar <install-location>, not " + Arrays.toString(args));
        }

        Path target = Paths.get(args[0]).toAbsolutePath();
        System.setProperty("java.awt.headless", "true");
        if (System.getProperty("java.net.preferIPv4Stack") == null) {
            System.setProperty("java.net.preferIPv4Stack", "true");
        }

        List<InstallationStrategy> strategies = new ArrayList<>(Arrays.asList(
            new InstallationStrategyPre1_13(),
            new InstallationStrategyPost1_13(),
            new InstallationStrategy2_1(),
            new InstallationStrategy2_2()
        ));

        RuntimeException exceptions = new RuntimeException("Failed to install Forge");
        for (InstallationStrategy strategy : strategies) {
            try {
                if (strategy.isUsable()) {
                    strategy.install(target.toFile());
                    return;
                } else {
                    exceptions.addSuppressed(new IOException("Strategy is not usable: " + strategy));
                }
            } catch (Throwable throwable) {
                exceptions.addSuppressed(throwable);
            }
        }

        for (Throwable exception : exceptions.getSuppressed()) {
            exception.printStackTrace(System.out);
        }

        throw exceptions;
    }

}
