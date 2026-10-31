package io.github.headlesshq.headlessmc.plugins.gradle;

import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.component.ComponentIdentifier;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.artifacts.result.ResolvedArtifactResult;
import org.gradle.api.attributes.AttributeContainer;
import org.gradle.api.attributes.Bundling;
import org.gradle.api.attributes.Category;
import org.gradle.api.attributes.LibraryElements;
import org.gradle.api.attributes.Usage;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.plugins.JavaLibraryPlugin;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Copy;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.jvm.tasks.Jar;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Builds HeadlessMc plugins.
 * <ul>
 *     <li>{@code headlessmcPlugin}: the {@link HmcPluginExtension}, describes the plugin.</li>
 *     <li>{@code headlessmc}: the HeadlessMc runtime, the plugin is compiled and tested against it. Never packaged.</li>
 *     <li>{@code include}: libraries nested into the plugin jar (Jar-in-Jar) under META-INF/jars, with
 *     their transitive dependencies, except for the ones HeadlessMc already contains.</li>
 *     <li>{@code generatePluginManifest}: writes META-INF/headlessmc-plugin.json.</li>
 *     <li>{@code installPlugin}: copies the plugin jar into {@code -PheadlessmcPluginsDir=<dir>}.</li>
 * </ul>
 */
public class HmcPluginGradlePlugin implements Plugin<Project> {
    public static final String EXTENSION = "headlessmcPlugin";
    public static final String HEADLESSMC_CONFIGURATION = "headlessmc";
    public static final String INCLUDE_CONFIGURATION = "include";
    public static final String HEADLESSMC_MODULE = "io.github.headlesshq:headlessmc-application";
    private static final Logger LOGGER = Logging.getLogger(HmcPluginGradlePlugin.class);

    @Override
    public void apply(Project project) {
        project.getPluginManager().apply(JavaLibraryPlugin.class);
        HmcPluginExtension extension = project.getExtensions().create(EXTENSION, HmcPluginExtension.class);
        extension.getHeadlessmcVersion().convention(getVersion());
        extension.getHeadlessmc().convention(extension.getHeadlessmcVersion());
        extension.getName().convention(extension.getId());
        extension.getJava().convention(GeneratePluginManifest.MIN_JAVA);

        ObjectFactory objects = project.getObjects();
        Configuration headlessmc = project.getConfigurations().create(HEADLESSMC_CONFIGURATION, configuration -> {
            configuration.setCanBeConsumed(false);
            configuration.setCanBeResolved(true);
            configuration.setDescription("The HeadlessMc runtime the plugin is compiled against, never packaged.");
            configuration.attributes(attributes -> runtimeAttributes(objects, attributes));
        });
        project.getDependencies().addProvider(HEADLESSMC_CONFIGURATION,
            extension.getHeadlessmcVersion().map(version -> HEADLESSMC_MODULE + ":" + version));
        // compile against the complete runtime classpath of HeadlessMc, it is exactly what the plugin runs with,
        // the api scope of the HeadlessMc modules misses e.g. the CDI annotations
        project.getDependencies().add("compileOnly", project.files(headlessmc));
        project.getDependencies().add("testImplementation", project.files(headlessmc));

        Configuration include = project.getConfigurations().create(INCLUDE_CONFIGURATION, configuration -> {
            configuration.setCanBeConsumed(false);
            configuration.setCanBeResolved(false);
            configuration.setDescription("Libraries to nest into the plugin jar (Jar-in-Jar).");
        });
        project.getConfigurations().named("implementation", configuration -> configuration.extendsFrom(include));
        Configuration includeClasspath = project.getConfigurations().create("includeClasspath", configuration -> {
            configuration.setCanBeConsumed(false);
            configuration.setCanBeResolved(true);
            configuration.extendsFrom(include);
            configuration.attributes(attributes -> runtimeAttributes(objects, attributes));
        });

        // group:artifact -> version of the libraries HeadlessMc contains
        Provider<Map<String, String>> coreModules = headlessmc.getIncoming().getArtifacts().getResolvedArtifacts()
            .map(HmcPluginGradlePlugin::modules);
        Provider<List<ResolvedArtifactResult>> nested = includeClasspath.getIncoming().getArtifacts().getResolvedArtifacts()
            .zip(coreModules, HmcPluginGradlePlugin::withoutCoreModules);
        Provider<List<File>> nestedFiles = nested.map(artifacts -> artifacts.stream().map(ResolvedArtifactResult::getFile).toList());

        TaskProvider<GeneratePluginManifest> manifest = project.getTasks().register("generatePluginManifest", GeneratePluginManifest.class, task -> {
            task.setGroup("build");
            task.setDescription("Generates META-INF/" + GeneratePluginManifest.FILE_NAME + ".");
            task.getPluginId().set(extension.getId());
            task.getPluginVersion().set(project.provider(() -> project.getVersion().toString()));
            task.getPluginName().set(extension.getName());
            task.getPluginDescription().set(extension.getDescription());
            task.getAuthors().set(extension.getAuthors());
            task.getLicense().set(extension.getLicense());
            task.getHomepage().set(extension.getHomepage());
            task.getHeadlessmc().set(extension.getHeadlessmc());
            task.getJava().set(extension.getJava());
            task.getDependencies().set(extension.getDependencies());
            task.getOptionalDependencies().set(extension.getOptionalDependencies());
            task.getLibraryVersions().set(extension.getLibraryVersions());
            task.getLibraries().set(nested.map(artifacts -> artifacts.stream().map(artifact -> {
                ModuleComponentIdentifier id = (ModuleComponentIdentifier) artifact.getId().getComponentIdentifier();
                return id.getGroup() + ":" + id.getModule() + ":" + id.getVersion() + ":" + artifact.getFile().getName();
            }).toList()));
            task.getLibraryFiles().from(nestedFiles);
            task.getOutput().set(project.getLayout().getBuildDirectory().file("generated/headlessmc/" + GeneratePluginManifest.FILE_NAME));
        });

        project.getTasks().named("jar", Jar.class, jar -> {
            jar.from(manifest, spec -> spec.into("META-INF"));
            jar.from(nestedFiles, spec -> spec.into(GeneratePluginManifest.JARS_DIR));
        });

        project.getTasks().withType(JavaCompile.class).configureEach(task -> {
            task.getOptions().setEncoding("UTF-8");
            task.getOptions().getRelease().convention(extension.getJava());
        });

        project.getTasks().register("installPlugin", Copy.class, task -> {
            task.setGroup("headlessmc");
            task.setDescription("Copies the plugin jar into -PheadlessmcPluginsDir=<HeadlessMc plugins directory>.");
            task.from(project.getTasks().named("jar"));
            task.into(project.getProviders().gradleProperty("headlessmcPluginsDir").orElse(project.provider(() -> {
                throw new GradleException("Set -PheadlessmcPluginsDir=<dir>, e.g. ~/.local/share/headlessmc/plugins");
            })));
        });
    }

    /**
     * @return the version of this Gradle plugin, which is the version of HeadlessMc it was released with.
     */
    static String getVersion() {
        try (InputStream inputStream = HmcPluginGradlePlugin.class.getResourceAsStream("version.properties")) {
            if (inputStream == null) {
                throw new IllegalStateException("version.properties is missing");
            }

            Properties properties = new Properties();
            properties.load(inputStream);
            return properties.getProperty("version");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Map<String, String> modules(Collection<ResolvedArtifactResult> artifacts) {
        Map<String, String> modules = new HashMap<>();
        for (ResolvedArtifactResult artifact : artifacts) {
            if (artifact.getId().getComponentIdentifier() instanceof ModuleComponentIdentifier id) {
                modules.put(id.getGroup() + ":" + id.getModule(), id.getVersion());
            }
        }

        return modules;
    }

    private static List<ResolvedArtifactResult> withoutCoreModules(Collection<ResolvedArtifactResult> artifacts,
                                                                   Map<String, String> coreModules) {
        List<ResolvedArtifactResult> result = new ArrayList<>(artifacts.size());
        for (ResolvedArtifactResult artifact : artifacts) {
            ComponentIdentifier componentId = artifact.getId().getComponentIdentifier();
            if (!(componentId instanceof ModuleComponentIdentifier id)) {
                throw new GradleException("Only external modules can be included, not " + componentId.getDisplayName());
            }

            String module = id.getGroup() + ":" + id.getModule();
            String coreVersion = coreModules.get(module);
            if (coreVersion == null) {
                result.add(artifact);
            } else {
                checkCoreVersion(module, id.getVersion(), coreVersion);
            }
        }

        return result;
    }

    /**
     * HeadlessMc always uses its own version of the libraries it contains.
     * Fails if the plugin needs a different major version, warns if it needs a newer one.
     */
    static void checkCoreVersion(String module, String version, String coreVersion) {
        if (version.equals(coreVersion)) {
            return;
        }

        if (!Versions.major(version).equals(Versions.major(coreVersion))) {
            throw new GradleException("The plugin needs " + module + ":" + version + ", but HeadlessMc ships "
                + coreVersion + " and plugins cannot replace libraries of HeadlessMc.");
        }

        if (Versions.compare(version, coreVersion) > 0) {
            LOGGER.warn("The plugin needs {}:{}, but HeadlessMc ships the older {}, which will be used at runtime.",
                module, version, coreVersion);
        }
    }

    private static void runtimeAttributes(ObjectFactory objects, AttributeContainer attributes) {
        attributes.attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.class, Usage.JAVA_RUNTIME));
        attributes.attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.class, Category.LIBRARY));
        attributes.attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.class, LibraryElements.JAR));
        attributes.attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.class, Bundling.EXTERNAL));
    }

}
