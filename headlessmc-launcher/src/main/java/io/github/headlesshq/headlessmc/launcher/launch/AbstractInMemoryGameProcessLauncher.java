package io.github.headlesshq.headlessmc.launcher.launch;

import io.github.headlesshq.headlessmc.java.Java;
import io.github.headlesshq.headlessmc.launcher.LauncherProperties;
import io.github.headlesshq.headlessmc.launcher.auth.AuthException;
import io.github.headlesshq.headlessmc.launcher.util.PathUtil;
import lombok.CustomLog;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

// TOP 10 worst ideas #9 this
@Getter
@CustomLog
@RequiredArgsConstructor
public abstract class AbstractInMemoryGameProcessLauncher extends SimpleInMemoryLauncher {
    protected final LaunchOptions options;
    protected final @Nullable Java java;

    protected abstract String getMainClassName() throws IOException;

    protected abstract List<String> getClasspath();

    protected abstract List<String> buildCommand() throws LaunchException, AuthException, IOException;

    protected abstract boolean needsJava9(Java java) throws IOException;

    public Java getJava() {
        Java java = this.java;
        if (java == null) {
            java = new Java("unknown", options.getLauncher().getConfig().get(LauncherProperties.ASSUMED_JAVA_VERSION, 8L).intValue());
        }

        return java;
    }

    public void launch() throws IOException, LaunchException, AuthException {
        log.warning("The In-Memory Launcher is a BETA feature and has not been thoroughly tested yet!");
        if (options.isForceBoot() && options.isForceSimple()) {
            throw new LaunchException("Both -forceSimple and -forceBoot specified!");
        }

        Java java = getJava();
        boolean java9 = needsJava9(java);

        String mainClass = getMainClassName();
        URL[] classpathUrls = new URL[getClasspath().size()];
        for (int i = 0; i < getClasspath().size(); i++) {
            log.info(getClasspath().get(i));
            classpathUrls[i] = Paths.get(getClasspath().get(i)).toUri().toURL();
        }

        List<String> actualCommand = buildCommand();
        List<String> gameArgs = new ArrayList<>();
        boolean hasPassedMainClass = false;
        for (String arg : actualCommand) {
            if (SystemPropertyHelper.isSystemProperty(arg)) {
                String[] nameValue = SystemPropertyHelper.splitSystemProperty(arg);
                log.info("SystemProperty: " + nameValue[0] + " : " + nameValue[1]);
                if ("java.library.path".equals(nameValue[0])) {
                    for (String library : nameValue[1].split(File.pathSeparator)) {
                        addLibraryPath(PathUtil.stripQuotes(library));
                    }
                } else {
                    System.setProperty(nameValue[0], nameValue[1]);
                }
            }

            if (hasPassedMainClass) {
                gameArgs.add(arg);
            }

            if (arg.equals(mainClass)) {
                hasPassedMainClass = true;
            }
        }

        System.setProperty("legacyClassPath", String.join(File.pathSeparator, getClasspath()));
        System.setProperty("java.class.path", String.join(File.pathSeparator, getClasspath()));

        if (java9) {
            log.info("Launching with Java-9 in-memory launcher, main: " + mainClass);
            java9Launch(classpathUrls, mainClass, gameArgs);
        } else {
            log.info("Launching with simple in-memory launcher, main: " + mainClass);
            simpleLaunch(classpathUrls, mainClass, gameArgs);
        }
    }

    protected void java9Launch(URL[] classpathUrls, String mainClass, List<String> gameArgs) {
        ClassLoader contextClassloader = Thread.currentThread().getContextClassLoader();
        try {
            Class<?> bootstrapLauncherClass = Class.forName("io.github.headlesshq.headlessmc.modlauncher.LayeredBootstrapLauncher");
            Constructor<?> constructor = bootstrapLauncherClass.getConstructor(List.class, URL[].class, String.class);
            Object bootstrapLauncher = constructor.newInstance(getClasspath().stream().map(Paths::get).collect(Collectors.toList()), classpathUrls, mainClass);
            Method launch = bootstrapLauncherClass.getMethod("launch", String[].class);
            launch.invoke(bootstrapLauncher, (Object) gameArgs.toArray(new String[0]));
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        } finally {
            Thread.currentThread().setContextClassLoader(contextClassloader);
        }
    }

    @SuppressWarnings({"JavaReflectionMemberAccess", "RedundantSuppression"})
    protected void addLibraryPath(Path libraryPath) {
        try {
            // https://stackoverflow.com/questions/15409223/adding-new-paths-for-native-libraries-at-runtime-in-java
            if (getJava().getVersion() <= 8) {
                String libraryPathToAdd = libraryPath.toString();

                Field usrPathsField = ClassLoader.class.getDeclaredField("usr_paths");
                usrPathsField.setAccessible(true);

                String[] paths = (String[]) usrPathsField.get(null);
                for (String path : paths) {
                    if (path.equals(libraryPathToAdd)) {
                        return;
                    }
                }

                String[] newPaths = Arrays.copyOf(paths, paths.length + 1);
                newPaths[newPaths.length - 1] = libraryPathToAdd;
                usrPathsField.set(null, newPaths);
            }
        } catch (Exception e) {
            log.error("Failed to add " + libraryPath + " to library path", e);
        }
    }

}
