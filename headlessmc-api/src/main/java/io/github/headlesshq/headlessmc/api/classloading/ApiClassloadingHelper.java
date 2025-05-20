package io.github.headlesshq.headlessmc.api.classloading;

import io.github.headlesshq.headlessmc.api.HeadlessMc;
import io.github.headlesshq.headlessmc.api.HeadlessMcApi;
import io.github.headlesshq.headlessmc.api.command.line.CommandLineManager;
import lombok.CustomLog;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.*;

/**
 * It's possible that multiple classes named {@link HeadlessMcApi} exist on multiple Classloaders.
 * E.g. when running the Runtime and HMC-Specifics together:
 * The Runtime instance might be loaded through the system classloader,
 * while the HMC-Specifics instance will be loaded through the modloaders classloader.
 * This class allows you to find HeadlessMcApi classes on other classloaders.
 * (restricted to parent classloaders only).
 */
@CustomLog
@UtilityClass
public class ApiClassloadingHelper {
    /**
     * Looks for {@link HeadlessMcApi} classes in all parent classloaders of the current one that has loaded this class.
     * If it finds an instance that has not been loaded by this classloader it will check if the {@link CommandLineManager}
     * provided by that instance is listening and if it does it will install a {@link ClAgnosticCommandContext} on that instance.
     *
     * @param headlessMc the HeadlessMc whose CommandLine to use for the ClAgnosticCommandContext.
     * @return the instance of the {@link CommandLineManager} on a remote instance that is already listening, or {@code null}.
     */
    public static @Nullable Object installOnOtherInstances(HeadlessMc headlessMc) {
        Object result = null;
        for (Class<?> apiClass : getApiClasses(ApiClassloadingHelper.class.getClassLoader())) {
            if (apiClass.equals(HeadlessMcApi.class)) {
                continue;
            }

            // might be on another module, requires us to deencapsulate
            headlessMc.getDeencapsulator().deencapsulate(apiClass);
            try {
                Method supportingClassloadingAgnosticContexts = apiClass.getMethod("isSupportingClassloadingAgnosticContexts");
                // HeadlessMcAPI.isSupportingClassloadingAgnosticContexts()
                if (!(Boolean) supportingClassloadingAgnosticContexts.invoke(null)) {
                    log.info("Found a HeadlessMcAPI that does not support cl agnostic contexts. " + apiClass + " on classloader " + apiClass.getClassLoader());
                    continue;
                }

                Method getInstance = apiClass.getMethod("getInstance");
                // HeadlessMc instance = HeadlessMcAPI.getInstance();
                Object instance = getInstance.invoke(null);
                if (instance != null) {
                    headlessMc.getDeencapsulator().deencapsulate(instance.getClass());
                    Method getCommandLine = instance.getClass().getMethod("getCommandLine");
                    // CommandLine commandLine = instance.getCommandLine();
                    Object commandLine = getCommandLine.invoke(instance);
                    headlessMc.getDeencapsulator().deencapsulate(commandLine.getClass());
                    Method listening = commandLine.getClass().getMethod("isListening");
                    // commandLine.isListening();
                    if (!(Boolean) listening.invoke(commandLine)) {
                        log.info("Found a non-listening API instance: " + apiClass + " on classloader " + apiClass.getClassLoader());
                        continue;
                    }

                    if (result != null) {
                        log.warn("Found another listening API instance? " + apiClass + " on classloader " + apiClass.getClassLoader());
                    }

                    // TODO: !!!!!!!!!!!!!!!!!!!!! How do we work around exposing Picoli?
                    /*Class<?> clAgnosticCommandContextClass = Class.forName(ClAgnosticCommandContext.class.getName(), true, apiClass.getClassLoader());
                    headlessMc.getDeencapsulator().deencapsulate(clAgnosticCommandContextClass);
                    // ClAgnosticCommandContext clAgnosticCommandContext = new ClAgnosticCommandContext(headlessMc.getCommandLine());
                    Object clAgnosticCommandContext = clAgnosticCommandContextClass.getConstructor(Object.class).newInstance(headlessMc.getCommandLine());
                    Class<?> commandContext = Class.forName(CommandContext.class.getName(), true, apiClass.getClassLoader());
                    headlessMc.getDeencapsulator().deencapsulate(commandContext);
                    Method setCommandContext = commandLine.getClass().getMethod("setCommandContext", commandContext);
                    // commandLine.setCommandContext(clAgnosticCommandContext);
                    setCommandContext.invoke(commandLine, clAgnosticCommandContext);*/
                    result = commandLine;
                    log.info("Installed ClAgnosticCommandContext on instance " + apiClass + " on classloader " + apiClass.getClassLoader());
                } else {
                    log.warn("Found an API without instance? " + apiClass + " on classloader " + apiClass.getClassLoader());
                }
            } catch (ReflectiveOperationException | NoClassDefFoundError e) {
                throw new IllegalStateException("Failed to call getInstance on " + apiClass + " on Classloader " + apiClass.getClassLoader(), e);
            }
        }

        return result;
    }

    private static Set<Class<?>> getApiClasses(ClassLoader currentClassLoader) {
        List<ClassLoader> classLoaders = new ArrayList<>();
        collectParentClassloaders(currentClassLoader, classLoaders);

        if (!classLoaders.contains(ClassLoader.getSystemClassLoader())) {
            classLoaders.add(ClassLoader.getSystemClassLoader());
        }

        Collections.reverse(classLoaders); // start with the parent classloaders
        Set<Class<?>> result = new LinkedHashSet<>();
        for (ClassLoader classLoader : classLoaders) {
            try {
                Class<?> api = Class.forName(HeadlessMcApi.class.getName(), true, classLoader);
                result.add(api);
            } catch (ClassNotFoundException | NoClassDefFoundError ignored) { }
        }

        return result;
    }

    private void collectParentClassloaders(@Nullable ClassLoader classLoader, List<ClassLoader> classLoaders) {
        ClassLoader cl = classLoader;
        while (cl != null) {
            if (!classLoaders.contains(cl)) {
                classLoaders.add(cl);
            }

            cl = cl.getParent();
        }
    }

}
