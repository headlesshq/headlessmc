# headlessmc

<img src="https://raw.githubusercontent.com/headlesshq/headlessmc/main/logo.svg" alt="HeadlessMc logo" width="160">

[HeadlessMc](https://github.com/headlesshq/headlessmc) is a command line launcher for Minecraft: Java Edition.
It can run the Minecraft client in headless mode, e.g. on CI/CD runners, handles mod loaders, modpacks, mods,
resourcepacks, shaderpacks and datapacks, and manages servers.

```shell
npm install -g headlessmc
headlessmc login
headlessmc launch fabric 26.3
```

Or run it without installing:

```shell
npx headlessmc launch fabric 26.3 -lwjgl
```

This package starts the native HeadlessMc executable for your platform (Windows x64, Linux x64/arm64 with glibc,
macOS x64/arm64), which npm installs as an optional dependency, so no Java installation is needed for HeadlessMc
itself. On other platforms, run the `headlessmc.jar` from the
[GitHub releases](https://github.com/headlesshq/headlessmc/releases) with Java 25 or newer.

See the [documentation](https://headlesshq.github.io/headlessmc/getting-started/) to get started.
