# HeadlessMc Quilt

A [HeadlessMc](https://github.com/headlesshq/headlessmc) plugin that adds the [Quilt](https://quiltmc.org) platform.

* `launch quilt/<mc-version>[/<loader-version>]` installs and launches Quilt clients,
  without a loader version the newest stable Quilt loader is used.
* `server add quilt/<mc-version>[/<loader-version>]` installs Quilt servers.
* Mods are read from `quilt.mod.json`, and from `fabric.mod.json`, because Quilt loads Fabric mods, too.

The client and server are installed with the official
[Quilt installer](https://github.com/QuiltMC/quilt-installer), which HeadlessMc downloads from the Quilt maven
and verifies against the hash pinned at build time (see `quiltInstaller` in [build.gradle](build.gradle)).
Loader versions come from [meta.quiltmc.org](https://meta.quiltmc.org/v3/versions/loader).

## Installation

Copy `headlessmc-quilt-<version>.jar` into the `plugins` directory in the HeadlessMc data directory,
e.g. `~/.local/share/headlessmc/plugins`, and check that it has been loaded with `headlessmc plugin ls`.

## Building

```shell
./gradlew build
```

This project was created from the
[HeadlessMc plugin template](https://github.com/headlesshq/headlessmc-plugin-template),
see its README for how the plugin is packaged.
