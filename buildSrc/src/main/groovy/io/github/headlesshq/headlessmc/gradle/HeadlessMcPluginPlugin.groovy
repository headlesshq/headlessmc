package io.github.headlesshq.headlessmc.gradle

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.api.attributes.Bundling
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.LibraryElements
import org.gradle.api.attributes.Usage
import org.gradle.api.plugins.JavaLibraryPlugin
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.tasks.Jar

/**
 * Builds HeadlessMc plugins.
 * <ul>
 *     <li>{@code headlessmc}: the HeadlessMc runtime, added to compileOnly. Never packaged.</li>
 *     <li>{@code include}: libraries nested into the plugin jar (Jar-in-Jar) under META-INF/jars, with
 *     their transitive dependencies, except for the ones HeadlessMc already contains.</li>
 *     <li>{@code generatePluginManifest}: writes META-INF/headlessmc-plugin.json.</li>
 *     <li>{@code installPlugin}: copies the plugin jar into {@code -PheadlessmcPluginsDir=<dir>}.</li>
 * </ul>
 */
class HeadlessMcPluginPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        project.pluginManager.apply(JavaLibraryPlugin)
        def extension = project.extensions.create('headlessmcPlugin', HeadlessMcPluginExtension)
        extension.java.convention(25)
        extension.headlessmc.convention(extension.headlessmcVersion)
        extension.name.convention(extension.id)

        Configuration headlessmcRuntime = project.configurations.create('headlessmc') {
            canBeConsumed = false
            canBeResolved = true
            description = 'The HeadlessMc runtime the plugin is compiled against, never packaged.'
            runtimeAttributes(project, it)
        }
        project.dependencies.addProvider('headlessmc', extension.headlessmcVersion.map {
            "io.github.headlesshq:headlessmc-application:$it".toString()
        })
        // compile against the complete runtime classpath of HeadlessMc, it is exactly what the plugin runs with,
        // the api scope of the HeadlessMc modules misses e.g. the CDI annotations
        project.dependencies.add('compileOnly', project.files(headlessmcRuntime))
        project.dependencies.add('testImplementation', project.files(headlessmcRuntime))

        Configuration include = project.configurations.create('include') {
            canBeConsumed = false
            canBeResolved = false
            description = 'Libraries to nest into the plugin jar (Jar-in-Jar).'
        }
        project.configurations.named('implementation') { extendsFrom(include) }
        Configuration includeClasspath = project.configurations.create('includeClasspath') {
            canBeConsumed = false
            canBeResolved = true
            extendsFrom(include)
            runtimeAttributes(project, it)
        }

        // group:artifact -> version of the libraries HeadlessMc contains
        Provider<Map<String, String>> coreModules = headlessmcRuntime.incoming.artifacts.resolvedArtifacts.map { artifacts ->
            modules(artifacts).collectEntries { [(it.key): it.value.version] }
        }
        Provider<List<ResolvedArtifactResult>> nested = includeClasspath.incoming.artifacts.resolvedArtifacts.map { artifacts ->
            Map<String, String> core = coreModules.get()
            artifacts.findAll { artifact ->
                def id = artifact.id.componentIdentifier
                if (!(id instanceof ModuleComponentIdentifier)) {
                    throw new GradleException("Only external modules can be included, not ${id.displayName}")
                }

                String module = "${id.group}:${id.module}"
                String coreVersion = core[module]
                if (coreVersion == null) {
                    return true
                }

                checkCoreVersion(project, module, id.version, coreVersion)
                return false
            }
        }

        def manifestTask = project.tasks.register('generatePluginManifest', GeneratePluginManifest) {
            group = 'build'
            description = 'Generates META-INF/headlessmc-plugin.json.'
            pluginId.set(extension.id)
            pluginVersion.set(project.provider { project.version.toString() })
            pluginName.set(extension.name)
            pluginDescription.set(extension.description)
            authors.set(extension.authors)
            license.set(extension.license)
            homepage.set(extension.homepage)
            headlessmc.set(extension.headlessmc)
            java.set(extension.java)
            dependencies.set(extension.dependencies)
            libraryVersions.set(extension.libraryVersions)
            libraries.set(nested.map { artifacts ->
                artifacts.collect { artifact ->
                    def id = artifact.id.componentIdentifier as ModuleComponentIdentifier
                    "${id.group}:${id.module}:${id.version}:${artifact.file.name}".toString()
                }
            })
            libraryFiles.from(nested.map { artifacts -> artifacts*.file })
            output.set(project.layout.buildDirectory.file('generated/headlessmc/headlessmc-plugin.json'))
        }

        project.tasks.named('jar', Jar) {
            from(manifestTask) {
                into 'META-INF'
            }
            from(nested.map { artifacts -> artifacts*.file }) {
                into GeneratePluginManifest.JARS_DIR
            }
        }

        project.tasks.withType(JavaCompile).configureEach {
            options.encoding = 'UTF-8'
            options.release.convention(extension.java)
        }

        project.tasks.register('installPlugin', Copy) {
            group = 'headlessmc'
            description = 'Copies the plugin jar into -PheadlessmcPluginsDir=<HeadlessMc plugins directory>.'
            from(project.tasks.named('jar'))
            into(project.providers.gradleProperty('headlessmcPluginsDir').orElse(project.provider {
                throw new GradleException('Set -PheadlessmcPluginsDir=<dir>, e.g. ~/.local/share/headlessmc/plugins')
            }))
        }
    }

    private static Map<String, ModuleComponentIdentifier> modules(Set<ResolvedArtifactResult> artifacts) {
        artifacts.collect { it.id.componentIdentifier }
            .findAll { it instanceof ModuleComponentIdentifier }
            .collectEntries { ModuleComponentIdentifier id -> [("${id.group}:${id.module}".toString()): id] }
    }

    /**
     * HeadlessMc always uses its own version of the libraries it contains.
     * Fails if the plugin needs a different major version, warns if it needs a newer one.
     */
    private static void checkCoreVersion(Project project, String module, String version, String coreVersion) {
        if (version == coreVersion) {
            return
        }

        if (major(version) != major(coreVersion)) {
            throw new GradleException("The plugin needs $module:$version, but HeadlessMc ships $coreVersion"
                + " and plugins cannot replace libraries of HeadlessMc.")
        }

        if (compare(version, coreVersion) > 0) {
            project.logger.warn("The plugin needs $module:$version, but HeadlessMc ships the older $coreVersion,"
                + " which will be used at runtime.")
        }
    }

    private static String major(String version) {
        version.split(/[.\-]/)[0]
    }

    private static int compare(String first, String second) {
        List<String> a = first.split(/[.\-]/) as List
        List<String> b = second.split(/[.\-]/) as List
        for (int i = 0; i < Math.min(a.size(), b.size()); i++) {
            if (a[i].isInteger() && b[i].isInteger()) {
                int result = a[i].toInteger() <=> b[i].toInteger()
                if (result != 0) {
                    return result
                }
            } else if (a[i] != b[i]) {
                return a[i] <=> b[i]
            }
        }

        a.size() <=> b.size()
    }

    private static void runtimeAttributes(Project project, Configuration configuration) {
        configuration.attributes {
            attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(Usage, Usage.JAVA_RUNTIME))
            attribute(Category.CATEGORY_ATTRIBUTE, project.objects.named(Category, Category.LIBRARY))
            attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, project.objects.named(LibraryElements, LibraryElements.JAR))
            attribute(Bundling.BUNDLING_ATTRIBUTE, project.objects.named(Bundling, Bundling.EXTERNAL))
        }
    }

}
