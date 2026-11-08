# HeadlessMc Plugin Template

A template for [HeadlessMc](https://github.com/headlesshq/headlessmc) plugins.
It contains an example plugin which adds the `hello` command, `hello --banner` uses a nested library.
Click **Use this template** to start your own plugin.

## Building and running

HeadlessMc 3.0.0-RC9 and the HeadlessMc Gradle plugin are not released yet, they are git submodules in `deps/`,
so clone with `--recursive` or run `git submodule update --init` after cloning.

```shell
./gradlew build                              # build/libs/headlessmc-example-plugin-<version>.jar
./gradlew hello                              # runs hello --banner HeadlessMc
./gradlew headlessmc --args="hello you"      # runs any HeadlessMc command
./gradlew headlessmc --args="plugin ls"
```

Every HeadlessMc task runs the HeadlessMc in `run/headlessmc` with the freshly built plugin installed,
so your own HeadlessMc installation is never touched. HeadlessMc needs Java 25, Gradle finds or
[downloads](https://github.com/gradle/foojay-toolchains) a Java 25 toolchain.
`./gradlew installPlugin` only installs it into `run/headlessmc/launcher/plugins`.

To use the plugin with your own HeadlessMc, copy the jar into the `plugins` directory in the HeadlessMc data
directory (`~/.local/share/headlessmc/plugins` on Linux) and run `headlessmc plugin ls`.

## Configuration

Everything is configured in [build.gradle](build.gradle) with the
[HeadlessMc Gradle plugin](https://github.com/headlesshq/headlessmc-gradle-plugin#writing-headlessmc-plugins)
`io.github.headlesshq.hmc-plugin-gradle-plugin`:

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

### Which HeadlessMc

Plugins need HeadlessMc 3.0.0-RC9, which is not released yet. Until then, the template includes the
`feat/plugins` branch of HeadlessMc (`deps/headlessmc`) and the `feat/headlessmc-3` branch of the
[HeadlessMc Gradle plugin](https://github.com/headlesshq/headlessmc-gradle-plugin) (`deps/headlessmc-gradle-plugin`)
as git submodules and [included builds](https://docs.gradle.org/current/userguide/composite_builds.html),
see [settings.gradle](settings.gradle). The included HeadlessMc reports itself as 3.0.0-RC9-SNAPSHOT,
the plugin is compatible with `[3.0.0-RC9,4)`. The first build compiles HeadlessMc, which takes a few minutes.

`git submodule update --remote` updates both to the newest commit of their branch.
Once HeadlessMc 3.0.0-RC9 is released, the submodules will be replaced by the released versions.

## Releasing

Publish a GitHub release with a tag like `1.0.0`. The [release workflow](.github/workflows/release.yml)
builds the plugin with that version, attaches the jar to the release and shows the entry for the
[HeadlessMc plugin index](https://github.com/headlesshq/headlessmc-plugins) in its summary.
Open a pull request there to make your plugin available with `headlessmc plugin search` and
`headlessmc plugin download`.

See the [HeadlessMc documentation](https://headlesshq.github.io/headlessmc/plugins/) for more.
