# HeadlessMc Plugin Template

A template for [HeadlessMc](https://github.com/headlesshq/headlessmc) plugins.
It contains an example plugin which adds the `hello` command, `hello --banner` uses a nested library.

## Building

```shell
./gradlew build
```

The plugin jar is `build/libs/headlessmc-example-plugin-<version>.jar`.
Copy it into the `plugins` directory in the HeadlessMc data directory, e.g. with
`./gradlew installPlugin -PheadlessmcPluginsDir=$HOME/.local/share/headlessmc/plugins`, then run `headlessmc plugin ls`.

## Configuration

Everything is configured in [build.gradle](build.gradle), the HeadlessMc version in [gradle.properties](gradle.properties).
The [HeadlessMc Gradle plugin](https://headlesshq.github.io/headlessmc/plugins/#the-gradle-plugin)
`io.github.headlesshq.hmc-plugin-gradle-plugin` does the packaging. It is released with HeadlessMc, under the same version,
so `headlessmc_version` sets both the version of the Gradle plugin and the HeadlessMc version the plugin is built against:

* `headlessmcPlugin { ... }` describes the plugin. It is written into `META-INF/headlessmc-plugin.json`,
  the manifest HeadlessMc reads: id, version, name, description, authors, license, homepage,
  compatible HeadlessMc versions, minimum Java version, dependencies on other plugins and the nested libraries
  with their hashes.
* The `headlessmc` configuration contains the HeadlessMc runtime, the plugin is compiled against it,
  but it is never packaged.
* `include` dependencies are nested into the plugin jar under `META-INF/jars` (Jar-in-Jar), together with their
  transitive dependencies, except for the libraries HeadlessMc already contains.
  HeadlessMc always uses its own version of those, the build fails if the plugin needs another major version of one.

If multiple plugins include the same library, HeadlessMc loads the highest version contained in the
compatible version ranges of all plugins. By default, a library is compatible with all versions of its major version,
override it with `libraryVersions 'group:artifact', '[1.2,3)'`.

## Native executables

`headlessmc plugin compile` compiles HeadlessMc with the installed plugins into a native executable.
For a plugin to work in it, resources it reads at runtime have to be registered with
`@RegisterResources(globs = "...")` and classes accessed by reflection (e.g. records parsed from JSON) with
`@RegisterForReflection`, see `HelloCommand`. CDI beans and picocli commands work without further configuration.

See the [HeadlessMc documentation](https://headlesshq.github.io/headlessmc/plugins/) for more.

## Developing against a local HeadlessMc

```shell
# in the headlessmc repository, publishes HeadlessMc and the Gradle plugin
./gradlew publishToMavenLocal -Pversion=3.0.0-RC9-SNAPSHOT
# in this repository
./gradlew build -Pheadlessmc_version=3.0.0-RC9-SNAPSHOT
```

To run the plugin with a local HeadlessMc build, build its mutable jar with
`./gradlew :headlessmc-application:mutableJarZip -PmutableJar` and start HeadlessMc with
`-Dhmc.plugins.mutable-jar=<headlessmc>/headlessmc-application/build/headlessmc-mutable.zip`.
