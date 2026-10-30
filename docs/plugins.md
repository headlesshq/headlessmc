# Plugins

Plugins add functionality to HeadlessMc, e.g. new commands.
A plugin is a jar file, which you place into the `plugins` directory in the HeadlessMc data directory
(`~/.local/share/headlessmc/plugins` on Linux).
Use `plugin ls` to see the installed plugins, their descriptions and whether they have been loaded,
`plugin ls --verbose` shows the full descriptions and `plugin ls --libraries` lists the libraries they ship.

## How plugins are loaded

HeadlessMc is built with [Quarkus](https://quarkus.io), which resolves everything,
e.g. which classes and CDI beans exist, when HeadlessMc is built.
So the native executables and the `headlessmc.jar` cannot load new jars by themselves.

If plugins are installed, HeadlessMc instead

1. downloads the `headlessmc-mutable.zip` of its release once and verifies its hash.
   It contains everything Quarkus needs to build HeadlessMc again.
2. builds HeadlessMc again with the plugins (re-augmentation).
   This takes a few seconds and only happens when the installed plugins change.
   The result is cached in `<cache>/plugins/builds/<hash>`.
3. runs the build containing the plugins in a new Java process with the same arguments.
   Java 25 is found or installed like for launching the game.

!!! note

    With plugins, HeadlessMc always runs on the JVM, even when started from a native executable.

To keep later starts fast, HeadlessMc remembers the build and the Java installation for the installed plugins.
As long as the size and modification time of the plugin jars do not change, it neither hashes the plugins again,
nor searches for Java installations, which can take a second on machines with many JDKs.
After the first run of a build, HeadlessMc creates an [AOT cache](https://openjdk.org/jeps/483) for the
Java process in the background, which makes every later start about a third faster (~400 ms).
It takes 47 MB per build and is not created if the `CI` environment variable is set,
because there every run usually starts with an empty cache.

| Property                     | Default | Description                                                                                       |
|------------------------------|---------|---------------------------------------------------------------------------------------------------|
| `hmc.plugins.enabled`        | `true`  | Set to `false` to ignore the plugins directory.                                                   |
| `hmc.plugins.fail-on-error`  | `true`  | If a plugin cannot be loaded, exit. With `false`, HeadlessMc starts with the plugins that can be loaded. |
| `hmc.plugins.keep-builds`    | `3`     | How many builds for different sets of plugins to keep in the cache.                               |
| `hmc.plugins.aot-cache`      | `true`, `false` in CI | Speeds up the start of the Java process with an AOT cache, see below.               |
| `hmc.plugins.jvm-args`       |         | Additional JVM arguments, comma separated, for the process running HeadlessMc with plugins.       |
| `hmc.plugins.mutable-jar`    |         | A `quarkus-app` directory or `headlessmc-mutable.zip` to use instead of downloading it.           |

## Writing plugins

Start from the [plugin template](https://github.com/headlesshq/headlessmc-plugin-template).
Plugins are regular CDI beans, they can inject everything HeadlessMc provides and are discovered by Quarkus like the
rest of HeadlessMc, so plugins need a `META-INF/beans.xml`.
To add a command, implement `HeadlessMcSubcommand` on a picocli command bean:

```java
@Dependent
@CommandLine.Command(name = "hello", description = "Says hello.")
public class HelloCommand implements Runnable, HeadlessMcSubcommand {
    private final Console console;

    @Inject
    public HelloCommand(Console console) {
        this.console = console;
    }

    @Override
    public void run() {
        console.write("Hello!");
    }
}
```

### The manifest

Every plugin jar contains `META-INF/headlessmc-plugin.json`, the template generates it:

```json
{
  "schemaVersion": 1,
  "id": "example",
  "version": "1.0.0",
  "name": "Example Plugin",
  "description": "An example HeadlessMc plugin.",
  "authors": ["You"],
  "license": "MIT",
  "homepage": "https://github.com/headlesshq/headlessmc-plugin-template",
  "headlessmc": "[3.0.0-RC9,4)",
  "java": 25,
  "dependencies": [
    {"id": "other-plugin", "versions": "[1.0,2)", "optional": false}
  ],
  "libraries": [
    {
      "group": "com.github.lalyos",
      "artifact": "jfiglet",
      "version": "0.0.9",
      "versions": "[0.0.9,1)",
      "path": "META-INF/jars/jfiglet-0.0.9.jar",
      "sha256": "543ad252f79199445ace26270e33cc1044a9dfe1ad2002de95137ed8fcf7e16f"
    }
  ]
}
```

| Field                    | Description                                                                                   |
|--------------------------|-----------------------------------------------------------------------------------------------|
| `schemaVersion`          | Always `1`.                                                                                   |
| `id`                     | Unique id of the plugin: lowercase letters, digits, `-` and `_`.                              |
| `version`                | Version of the plugin.                                                                        |
| `name`, `description`, `authors`, `license`, `homepage` | Optional information shown to users.                             |
| `headlessmc`             | Versions of HeadlessMc the plugin works with.                                                 |
| `java`                   | Minimum Java version, at least 25.                                                            |
| `dependencies`           | Other plugins this plugin needs, plugins with `optional` dependencies load without them.      |
| `libraries`              | Libraries nested in the plugin jar.                                                           |

Version ranges use the Maven syntax: `[1.0,2.0)` means `1.0 <= version < 2.0`, `[1.0]` exactly `1.0`,
`*` any version. A plain version like `1.2` means `[1.2,2)`, any version up to the next major version.

### Libraries

Plugins ship their libraries as jars nested in the plugin jar (Jar-in-Jar), the template nests every
`include` dependency and its transitive dependencies.
All plugins run on the same classpath as HeadlessMc, so only one version of each library can be loaded:

* Libraries HeadlessMc already contains are never replaced, HeadlessMc's version is used.
  A plugin that cannot work with it (its `versions` range does not contain it) is not loaded.
  The template never nests these libraries and fails the build if the plugin needs a different major version.
* If multiple plugins ship the same library, the highest version that is contained in the `versions` range
  of every plugin is used. By default, a library is compatible with `[version, nextMajor)`,
  so the newest version wins, but two plugins needing different major versions cannot be loaded together.
