package io.github.headlesshq.headlessmc.gradle

import groovy.json.JsonOutput
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

import java.security.MessageDigest

/**
 * Generates META-INF/headlessmc-plugin.json, the manifest HeadlessMc reads from every plugin jar.
 */
@CacheableTask
abstract class GeneratePluginManifest extends DefaultTask {
    static final int SCHEMA_VERSION = 1
    static final String JARS_DIR = 'META-INF/jars'

    @Input
    abstract Property<String> getPluginId()

    @Input
    abstract Property<String> getPluginVersion()

    @Input
    @Optional
    abstract Property<String> getPluginName()

    @Input
    @Optional
    abstract Property<String> getPluginDescription()

    @Input
    abstract ListProperty<String> getAuthors()

    @Input
    @Optional
    abstract Property<String> getLicense()

    @Input
    @Optional
    abstract Property<String> getHomepage()

    @Input
    abstract Property<String> getHeadlessmc()

    @Input
    abstract Property<Integer> getJava()

    @Input
    abstract MapProperty<String, Map<String, Object>> getDependencies()

    @Input
    abstract MapProperty<String, String> getLibraryVersions()

    /**
     * group:artifact:version:fileName of every nested library.
     */
    @Input
    abstract ListProperty<String> getLibraries()

    /**
     * The nested library jars, to compute their hashes.
     */
    @InputFiles
    @PathSensitive(PathSensitivity.NAME_ONLY)
    abstract ConfigurableFileCollection getLibraryFiles()

    @OutputFile
    abstract RegularFileProperty getOutput()

    @TaskAction
    void generate() {
        String id = pluginId.get()
        if (!(id ==~ /[a-z0-9][a-z0-9_-]*/)) {
            throw new GradleException("Invalid plugin id '$id', use lowercase letters, digits, - and _")
        }

        if (java.get() < 25) {
            throw new GradleException("HeadlessMc requires at least Java 25, but java is set to ${java.get()}")
        }

        Map<String, File> filesByName = [:]
        libraryFiles.files.each { filesByName[it.name] = it }

        Set<String> paths = []
        List<Map<String, Object>> libraries = this.libraries.get().collect { String coordinates ->
            def (group, artifact, version, fileName) = coordinates.split(':', 4) as List<String>
            String path = "$JARS_DIR/$fileName"
            if (!paths.add(path)) {
                throw new GradleException("Two included libraries are named $fileName")
            }

            File file = filesByName[fileName]
            if (file == null) {
                throw new GradleException("Could not find the jar of included library $coordinates")
            }

            String versions = libraryVersions.get()["$group:$artifact".toString()]
            withoutNulls([
                group   : group,
                artifact: artifact,
                version : version,
                versions: versions,
                path    : path,
                sha256  : sha256(file),
            ])
        }

        List<Map<String, Object>> dependencies = this.dependencies.get().collect { String dependencyId, Map<String, Object> dependency ->
            [id: dependencyId, versions: dependency.versions ?: '*', optional: dependency.optional ?: false] as Map<String, Object>
        }

        Map<String, Object> manifest = withoutNulls([
            schemaVersion: SCHEMA_VERSION,
            id           : id,
            version      : pluginVersion.get(),
            name         : pluginName.getOrNull(),
            description  : pluginDescription.getOrNull(),
            authors      : authors.get(),
            license      : license.getOrNull(),
            homepage     : homepage.getOrNull(),
            headlessmc   : headlessmc.get(),
            java         : java.get(),
            dependencies : dependencies,
            libraries    : libraries,
        ])

        output.get().asFile.text = JsonOutput.prettyPrint(JsonOutput.toJson(manifest)) + '\n'
    }

    static Map<String, Object> withoutNulls(Map<String, Object> map) {
        map.findAll { it.value != null } as LinkedHashMap<String, Object>
    }

    static String sha256(File file) {
        MessageDigest digest = MessageDigest.getInstance('SHA-256')
        file.withInputStream { stream ->
            byte[] buffer = new byte[8192]
            int read
            while ((read = stream.read(buffer)) != -1) {
                digest.update(buffer, 0, read)
            }
        }

        digest.digest().encodeHex().toString()
    }

}
