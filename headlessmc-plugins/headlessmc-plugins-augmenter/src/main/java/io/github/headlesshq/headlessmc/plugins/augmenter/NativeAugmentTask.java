package io.github.headlesshq.headlessmc.plugins.augmenter;

import io.quarkus.bootstrap.app.AdditionalDependency;
import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.model.MutableJarApplicationModel;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Stream;

/**
 * Like Quarkus' {@code io.quarkus.deployment.mutability.ReaugmentTask}, but generates the sources for a
 * native image ({@code quarkus.native.sources-only}) instead of a jar.
 * Runs in the deployment class loader set up by {@link NativeAugmenterMain}.
 */
public final class NativeAugmentTask {
    private NativeAugmentTask() {
        throw new AssertionError();
    }

    public static void main(Path appRoot) throws Exception {
        Path deploymentLib = appRoot.resolve("lib").resolve("deployment");
        Properties buildSystemProperties = new Properties();
        try (InputStream inputStream = Files.newInputStream(appRoot.resolve("quarkus").resolve("build-system.properties"))) {
            buildSystemProperties.load(inputStream);
        }

        MutableJarApplicationModel mutableModel;
        try (InputStream inputStream = Files.newInputStream(deploymentLib.resolve("appmodel.dat"));
             ObjectInputStream objectInputStream = new ObjectInputStream(inputStream)) {
            mutableModel = (MutableJarApplicationModel) objectInputStream.readObject();
        }

        List<AdditionalDependency> providers = new ArrayList<>();
        String providersDirectory = mutableModel.getUserProvidersDirectory();
        if (providersDirectory != null) {
            System.setProperty("quarkus.package.jar.user-providers-directory", providersDirectory);
            providers.addAll(getProviders(appRoot.resolve(providersDirectory)));
        }

        ApplicationModel model = mutableModel.getAppModel(appRoot);
        // the jar packaging settings of the mutable jar would otherwise be used
        System.setProperty("quarkus.package.jar.type", "mutable-jar");
        System.setProperty("quarkus.native.enabled", "true");
        System.setProperty("quarkus.native.sources-only", "true");
        try (CuratedApplication application = QuarkusBootstrap.builder()
            .setAppArtifact(model.getAppArtifact())
            .setExistingModel(model)
            .setRebuild(true)
            .setBuildSystemProperties(buildSystemProperties)
            .setBaseName(mutableModel.getBaseName())
            .addAdditionalApplicationArchives(providers)
            .setApplicationRoot(model.getAppArtifact().getResolvedPaths().getSinglePath())
            .setTargetDirectory(appRoot.getParent())
            .setBaseClassLoader(NativeAugmentTask.class.getClassLoader())
            .build()
            .bootstrap()) {
            application.createAugmentor().createProductionApplication();
        }

        addProviders(appRoot.resolve(providersDirectory == null ? "providers" : providersDirectory),
            appRoot.getParent().resolve("native-sources"));
    }

    /**
     * Quarkus only places the classes of additional application archives it transformed or generated into the
     * native sources, the jar build reads the others from the providers directory at runtime.
     * So the provider jars are copied into {@code native-sources/lib/providers} and appended to the
     * {@code Class-Path} of the runner jar, after its own entries, so that the transformed classes still win.
     */
    static void addProviders(Path providers, Path nativeSources) throws IOException {
        List<Path> jars = listJars(providers);
        if (jars.isEmpty()) {
            return;
        }

        Path target = nativeSources.resolve("lib").resolve("providers");
        Files.createDirectories(target);
        StringBuilder classPath = new StringBuilder();
        for (Path jar : jars) {
            Files.copy(jar, target.resolve(jar.getFileName()), StandardCopyOption.REPLACE_EXISTING);
            classPath.append(" lib/providers/").append(jar.getFileName());
        }

        Path runner;
        try (Stream<Path> files = Files.list(nativeSources)) {
            runner = files.filter(path -> path.getFileName().toString().endsWith("-runner.jar"))
                .findFirst()
                .orElseThrow(() -> new IOException("No runner jar in " + nativeSources));
        }

        Path temp = runner.resolveSibling(runner.getFileName() + ".tmp");
        try (JarFile jarFile = new JarFile(runner.toFile())) {
            Manifest manifest = new Manifest(jarFile.getManifest());
            Attributes attributes = manifest.getMainAttributes();
            String existing = attributes.getValue(Attributes.Name.CLASS_PATH);
            attributes.put(Attributes.Name.CLASS_PATH, (existing == null ? "" : existing) + classPath);
            try (OutputStream outputStream = Files.newOutputStream(temp);
                 JarOutputStream jarOutputStream = new JarOutputStream(outputStream, manifest)) {
                Enumeration<JarEntry> entries = jarFile.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    if (entry.getName().equalsIgnoreCase(JarFile.MANIFEST_NAME)) {
                        continue;
                    }

                    jarOutputStream.putNextEntry(new JarEntry(entry.getName()));
                    try (InputStream inputStream = jarFile.getInputStream(entry)) {
                        inputStream.transferTo(jarOutputStream);
                    }

                    jarOutputStream.closeEntry();
                }
            }
        }

        Files.move(temp, runner, StandardCopyOption.REPLACE_EXISTING);
    }

    private static List<AdditionalDependency> getProviders(Path directory) throws IOException {
        return listJars(directory).stream().map(path -> new AdditionalDependency(path, false, true)).toList();
    }

    private static List<Path> listJars(Path directory) throws IOException {
        if (!Files.isDirectory(directory)) {
            return List.of();
        }

        try (Stream<Path> files = Files.list(directory)) {
            return files.filter(path -> path.toString().endsWith(".jar")).sorted().toList();
        }
    }

}
