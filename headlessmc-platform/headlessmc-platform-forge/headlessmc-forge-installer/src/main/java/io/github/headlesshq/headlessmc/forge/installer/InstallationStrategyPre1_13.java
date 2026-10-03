package io.github.headlesshq.headlessmc.forge.installer;

import net.minecraftforge.installer.ServerInstall;

import java.io.File;
import java.lang.reflect.Method;

/**
 * Installation strategy for very old versions of the launcher that are pre 1.13
 */
public class InstallationStrategyPre1_13 implements InstallationStrategy {
    private static final String CLASS_NAME = "net.minecraftforge.installer.ClientInstall";

    @Override
    public void install(File target) throws ReflectiveOperationException {
        ServerInstall.headless = true;
        Class<?> clientInstall = Class.forName(CLASS_NAME);
        Object instance = clientInstall.getConstructor().newInstance();
        install(clientInstall, instance, target);
    }

    @Override
    public boolean isUsable() {
        try {
            Class.forName(CLASS_NAME);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private void install(Class<?> clientInstall, Object instance, File target)
        throws ReflectiveOperationException {
        Method install;
        Object predicate;
        try {
            Class<?> predicateClass = Predicates.getPredicateClass();
            install = clientInstall.getMethod("run", File.class, predicateClass);
            predicate = Predicates.isGooglePredicate(predicateClass)
                ? Predicates.getGooglePredicate()
                : Predicates.getJavaPredicate();
        } catch (Throwable t) {
            install = clientInstall.getMethod("run", File.class, Predicates.getJavaPredicateClass());
            predicate = Predicates.getJavaPredicate();
        }

        install.invoke(instance, target, predicate);
    }

}
