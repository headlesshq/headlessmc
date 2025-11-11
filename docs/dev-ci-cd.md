# CI/CD

One of the main use-cases for HeadlessMc is launching the Minecraft client inside you CI/CD pipelines.
Projects for using HeadlessMc to run both the production client and server exist.

- [mc-runtime-test](https://github.com/headlesshq/mc-runtime-test)
- [mc-server-test](https://github.com/headlesshq/mc-server-test)

### MC-Runtime-Test
MC-Runtime-Test enables you to run the Minecraft client within your CI/CD pipelines, 
simplifying the testing of runtime bugs in Minecraft mods. 
Manual testing for different Minecraft versions and modloaders can be time-consuming, 
especially when bugs occur only in runtime environments launched via a Minecraft launcher.

```yaml
name: Run Minecraft Client

on:
  workflow_dispatch:

env:
  java_version: 21

jobs:
  run:
    runs-on: ubuntu-latest
    steps:
      - name: Install Java
        uses: actions/setup-java@v4
        with:
          java-version: ${{ env.java_version }}
          distribution: "temurin"

      - name: [Example] Build mod
        run: ./gradlew build

      - name: [Example] Stage mod for test client
        run: |
          mkdir -p run/mods
          cp build/libs/&lt;your-mod&gt;.jar run/mods

      - name: Run MC test client
        uses: headlesshq/mc-runtime-test@3.1.1
        with:
          mc: 1.21.4
          modloader: fabric
          regex: .*fabric.*
          mc-runtime-test: fabric
          java: ${{ env.java_version }}
```

More examples:

- [Fabric Workflow Example](https://github.com/headlesshq/hmc-optimizations/blob/1.20.4/.github/workflows/run-fabric.yml)
- [Matrix Workflow Testing Multiple Versions](https://github.com/headlesshq/hmc-specifics/blob/main/.github/workflows/run-matrix.yml)

---

#### Inputs
The following table summarizes the available inputs for customization:

| Input                 | Description                            | Required | Example                                  |
|-----------------------|----------------------------------------|----------|------------------------------------------|
| `mc`                  | Minecraft version to run               | Yes      | `1.20.4`                                 |
| `modloader`           | Modloader to install                   | Yes      | `forge`, `neoforge`, `fabric`            |
| `regex`               | Regex to match the modloader jar       | Yes      | `.*fabric.*`                             |
| `java`                | Java version to use                    | Yes      | `8`, `16`, `17`, `21`                    |
| `mc-runtime-test`     | MC-Runtime-Test jar to download        | Yes      | `none`, `lexforge`, `neoforge`, `fabric` |
| `dummy-assets`        | Use dummy assets during testing        |          | `true`, `false`                          |
| `xvfb`                | Runs the game with Xvfb                |          | `true`, `false`                          |
| `headlessmc-command`  | Command-line arguments for HeadlessMC  |          | `--jvm "-Djava.awt.headless=true"`       |
| `fabric-api`          | Fabric API version to download or none |          | `0.97.0`, `none`                         |
| `fabric-gametest-api` | Fabric GameTest API version or none    |          | `1.3.5+85d85a934f`, `none`               |
| `download-hmc`        | Download HeadlessMC                    |          | `true`, `false`                          |
| `hmc-version`         | HeadlessMC version                     |          | `2.5.1`, `1.5.0`                         |
| `cache-mc`            | Cache `.minecraft`                     |          | `true`, `false`                          |

---

#### Running Your Own Tests
MC-Runtime-Test supports Minecraft’s [Game-Test Framework](https://www.minecraft.net/en-us/creator/article/get-started-gametest-framework). It executes `/test runall` upon joining a world.

!!! tip "Neoforge/Forge GameTests"

    Currently, Forge and NeoForge GameTest discovery may require additional setup, [hacks](gametest/src/main/java/io/github/headlesshq/clientgametest/mixin/MixinGameTestRegistry.java), or other modifications to register structure templates correctly. We expect to simplify this for future releases.

You can also use the `headlessmc-command` input to specify a JVM argument to enforce the minimum number of GameTests you expect to be executed:

<pre lang="bash">
-DMcRuntimeGameTestMinExpectedGameTests=1
</pre>

---
