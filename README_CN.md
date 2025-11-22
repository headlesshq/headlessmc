<!--suppress HtmlDeprecatedAttribute -->
<div align="right">
  <a href="README.md">English</a> | <a href="README_CN.md">中文</a>
</div>

<h1 align="center" style="font-weight: normal;"><b>HeadlessMc</b></h1>
<p align="center">Minecraft Java 版的命令行启动器。</p>
<p align="center"><img src="headlessmc-web/page/logo.png" alt="logo" style="width:200px;"></p>
<p align="center"><a href="https://github.com/headlesshq/mc-runtime-test">Mc-Runtime-Test</a> | HMC | <a href="https://github.com/headlesshq/hmc-specifics">HMC-Specifics</a> | <a href="https://github.com/headlesshq/hmc-optimizations">HMC-Optimizations</a></p>

<div align="center">

[![Codacy Badge](https://app.codacy.com/project/badge/Grade/6a86b3e62d3b47909de670b09737f8fd)](https://app.codacy.com/gh/headlesshq/headlessmc/dashboard?utm_source=gh&utm_medium=referral&utm_content=&utm_campaign=Badge_grade)

[![GitHub All Releases](https://img.shields.io/github/downloads/headlesshq/HeadlessMc/total.svg)](https://github.com/headlesshq/HeadlessMc/releases)

![](https://github.com/headlesshq/HeadlessMc/actions/workflows/gradle-publish.yml/badge.svg)

![GitHub](https://img.shields.io/github/license/headlesshq/HeadlessMc)

[![Docker Image Size](https://img.shields.io/docker/image-size/3arthqu4ke/headlessmc)](https://hub.docker.com/r/3arthqu4ke/headlessmc/)

![Github last-commit](https://img.shields.io/github/last-commit/headlesshq/HeadlessMc)

</div>

> [!NOTE]
> 我们目前正在开发 HeadlessMc 3.0，它将完全重构（使用 Picocli 等）！
> 可以在 [v3](https://github.com/headlesshq/headlessmc/tree/v3) 分支上跟踪进度。

> [!WARNING]
> 非官方 Minecraft 产品。未经 Mojang 或 Microsoft 批准或关联。
> 
> HeadlessMc 不会允许你在未购买 Minecraft 的情况下游玩！
> 账户将始终被验证。
> 离线账户只能用于在 CI/CD 流水线中以无头模式运行游戏。

HeadlessMc (HMC) 允许你从命令行启动 Minecraft Java 版。
它可以管理客户端、服务器和模组。
它可以在无头模式下运行客户端，无需屏幕，由命令行控制。
例如，这可以让你在 CI/CD 流水线中使用 [mc-runtime-test](https://github.com/headlesshq/mc-runtime-test) 测试游戏。

> [!TIP]  
> 阅读我们全新的精美文档 [这里](https://headlesshq.github.io/headlessmc)。

## 快速开始

1. 从发布页面下载 `headlessmc-launcher.jar` 并安装 Java 版本 &geq; 8。
    - 如果你想要额外功能，例如插件和在同一个 JVM 中启动游戏，请使用 `headlessmc-launcher-wrapper.jar`。
    - 如果你下载 GraalVM 可执行文件之一，则无需安装 Java。
2. 在终端中使用 `java -jar headlessmc-launcher.jar` 运行启动器。
    - 或者，例如，如果你使用 GraalVM 可执行文件，使用 `./headlessmc-launcher-linux`。
3. HeadlessMc 通常不允许你在没有账户的情况下启动游戏。
通过执行 `login` 命令登录到你的 Minecraft 账户并按照说明操作。
   - 对于 Microsoft 账户，使用 `login` 命令。
   - 对于第三方认证服务器（Yggdrasil），使用 `yggdrasil login` 命令（见下面的 [Yggdrasil 认证](#yggdrasil-认证)）。
4. 使用 `launch <modloader>:<version>` 启动游戏，例如 `launch fabric:1.21.4 -lwjgl`。
`lwjgl` 标志将使游戏以无头模式运行。

阅读[更多](https://headlesshq.github.io/headlessmc/getting-started/)。

### Yggdrasil 认证

HeadlessMc 支持 Yggdrasil 认证协议，允许你使用第三方认证服务器，例如 [LittleSkin](https://littleskin.cn)。

**功能：**
- 使用用户名和密码登录到 Yggdrasil 兼容服务器
- 支持具有多个角色的账户（交互式角色选择）
- 游戏启动前自动验证 token
- 所有登录操作的详细日志记录
- 日志中安全的 token 打码

**使用方法：**

```bash
# 使用默认 LittleSkin 服务器登录
> yggdrasil login <username> <password>

# 使用自定义 Yggdrasil 服务器登录
> yggdrasil login <username> <password> --server <server_url>

# 交互式密码输入（密码将被隐藏）
> yggdrasil login <username>
Enter your password or type 'abort' to cancel the login process.
```

**多角色示例：**

```
> yggdrasil login myuser mypass
========================================
Starting manual login / 开始手动登录
Login server: https://littleskin.cn/api/yggdrasil / 登录服务器: https://littleskin.cn/api/yggdrasil
Username: myuser / 用户名: myuser
Validating username and password... / 正在验证用户名和密码...
Username and password validated successfully / 用户名密码验证正确
Found 2 profile(s) on external server / 在外置服务器找到 2 个角色:
  1. Character1 (UUID: ...) [Current Default / 当前默认]
  2. Character2 (UUID: ...)
Enter profile number to use (1-2, or press Enter to use current profile, type 'abort' to cancel) / 请输入要使用的角色编号 (1-2, 或按 Enter 使用当前角色, 输入 'abort' 取消):
> 2
Selected profile: Character2 / 玩家已选择: Character2
Received accessToken: abc123def4********************xyz789uvw0 / 请求到的 accessToken: abc123def4********************xyz789uvw0
Login successful! Account: Character2 / 登录成功！账户: Character2
========================================
```

**Token 验证：**

在启动游戏之前，HeadlessMc 会自动验证 Yggdrasil token。如果 token 无效或已过期，系统会提示你选择是继续（可能导致"无效的会话"错误）还是取消并重新登录。

**账户存储：**

Yggdrasil 账户与 Microsoft 账户一起存储在 `HeadlessMC/auth/.accounts.json` 中。Token 被安全存储，并在每次游戏启动前自动验证。

### HeadlessMc-Specifics

[hmc-specifics](https://github.com/headlesshq/hmc-specifics) 是可以放在 .minecraft/mods 文件夹中的模组。
与 HeadlessMc 一起，它们允许你通过命令行控制游戏，例如
通过 `msg "<message>"` 发送聊天消息和命令，
通过 `gui` 可视化 Minecraft 显示的菜单，并通过 `click` 点击菜单。

阅读[更多](https://headlesshq.github.io/headlessmc/specifics/)。

### Docker 

存在一个预配置的 [docker 镜像](https://hub.docker.com/r/3arthqu4ke/headlessmc/)，
它预装了 Java 8、17 和 21。
通过 `docker pull 3arthqu4ke/headlessmc:latest` 拉取它
并使用 `docker run -it 3arthqu4ke/headlessmc:latest` 运行它。
在容器内，你可以在任何地方使用 `hmc` 命令。

### Android

HeadlessMc 可以在 Termux 中运行。
* 从 F-Droid 下载 Termux，不要从 PlayStore 下载。
* 安装 Java：`apt update && apt upgrade $ apt install openjdk-<version>`
* 将 headlessmc-launcher-wrapper.jar 下载到 Termux。
* 禁用 JLine，因为我们目前无法让它在 Termux 上工作，
  通过在 HeadlessMC/config.properties 中添加 `hmc.jline.enabled=false`。
* 现在你可以像在桌面或 Docker 上一样使用 HeadlessMc。

### Web

HeadlessMc 可以在浏览器中运行，某种程度上。
首先，有 CheerpJ，一个 WebAssembly JVM，
但它不支持我们启动游戏所需的所有功能。
可以[在这里](https://headlesshq.github.io/headlessmc/)试用 CheerpJ 实例。
其次，有 [container2wasm](https://github.com/headlesshq/hmc-container2wasm)，
它可以将 HeadlessMc Docker 容器
转换为 WebAssembly 并在浏览器中运行，但这非常慢。

### 服务器

HeadlessMc 还支持 Minecraft 服务器。
它可以安装和运行 Paper、Fabric、Vanilla、Purpur、Forge 和 Neoforge 服务器。
目前不支持服务器的插桩。
使用以下命令：

```
> server add paper 1.21.5
Added paper server: paper-1.21.5-54.

> server list
id   type    version   name
0    paper   1.21.5    paper-1.21.5-54

> server eula paper-1.21.5-54 -accept
...

> server launch paper-1.21.5-54 --jvm "-Xmx10G -XX:+UseG1GC <...>"
...
```

### 测试

HeadlessMc 的主要目标之一是支持生产服务器和客户端的测试。
为此，它在 [mc-runtime-test](https://github.com/headlesshq/mc-runtime-test) 中使用。
它还有一个内置的命令测试框架。
它可以向正在运行的进程发送命令并检查输出。
测试可以用 json 格式指定。
例如，测试**任何** Minecraft 服务器是否成功启动的工作流：

```json
{
  "name": "Server Test",
  "steps": [
    {
      "type": "ENDS_WITH",
      "message": "For help, type \"help\""
    },
    {
      "type": "SEND",
      "message": "stop",
      "timeout": 120
    }
  ]
}
```

它检查以 `For help, type "help"` 结尾的日志消息，
这是所有版本的 Minecraft 服务器在成功启动时输出的内容。
然后它通过向服务器发送 stop 命令来停止服务器。
你可以编写自己的测试，甚至可以针对客户端而不是服务器运行它，
前提是客户端支持命令，例如通过 [hmc-specifics](https://github.com/headlesshq/hmc-specifics)。
只需在配置中使用键 `hmc.test.filename` 指定测试文件的位置。
一个测试 HeadlessMc 是否可以使用 hmc-specifics 启动游戏的示例 CI 工作流
可以在[这里](.github/workflows/hmc-specifics-test.yml)找到。

### 优化 

HeadlessMc 通过修补 LWJGL 库来实现无头模式：
它的每个函数都被重写为不执行任何操作，或返回存根值
（你可以[在这里](headlessmc-lwjgl/README.md)阅读更多相关信息）。
这具有独立于 Minecraft 版本的优点，
但会带来一些开销。
一个依赖于 Minecraft 版本的方法是 [hmc-optimizations](https://github.com/headlesshq/hmc-optimizations)，
另一组修补 Minecraft 本身以跳过所有渲染代码的模组。
此外，HeadlessMc 还带有 `hmc.assets.dummy` 属性，
它用小的虚拟纹理和声音替换所有资源，
这允许更小的内存占用和启动前更少的下载。
你也可以通过使用虚拟帧缓冲区（如 [Xvfb](https://www.x.org/releases/X11R7.6/doc/man/man1/Xvfb.1.xhtml)）运行 headlessmc 来实现无头模式，而无需修补 lwjgl。

### 配置 HeadlessMc

> [!NOTE]  
> 所有配置选项都列在[这里](https://headlesshq.github.io/headlessmc/configuration/)

- HeadlessMc 将其配置存储在 `HeadlessMC/config.properties` 中。
- 在 Windows 和 Linux 上，某些文件夹中的 Java 版本会自动检测
  并且 HeadlessMc 可以下载缺失的 Java 发行版。
  但你也可以指定 HeadlessMc 可以用来运行游戏的 Java 安装。
  打开文件 `HeadlessMC/config.properties` 并添加一个名为 `hmc.java.versions` 的键，
  使用 `;` 分隔的 HeadlessMc 可以使用的 Java 版本列表，如下所示：
    ```properties
    hmc.java.versions=C:/Program Files/Java/jre-<version>/bin/java;C:/Program Files/Java/jdk-<version>/bin/java
    ```
- 重启 HeadlessMc 或使用 `config -refresh` 然后 `java -refresh`，HeadlessMc 现在应该知道要使用哪些 Java 版本。

属性也可以从命令行作为 SystemProperties 传递。
有关可用属性，请参阅 [HmcProperties](headlessmc-api/src/main/java/io/github/headlesshq/headlessmc/api/config/HmcProperties.java)、
[LauncherProperties](headlessmc-launcher/src/main/java/io/github/headlesshq/headlessmc/launcher/LauncherProperties.java)、
[JLineProperties](headlessmc-jline/src/main/java/io/github/headlesshq/headlessmc/jline/JLineProperties.java)、
[LoggingProperties](headlessmc-logging/src/main/java/io/github/headlesshq/headlessmc/logging/LoggingProperties.java)、
[RuntimeProperties](headlessmc-runtime/src/main/java/io/github/headlesshq/headlessmc/runtime/RuntimeProperties.java) 或
[LwjglProperties](headlessmc-lwjgl/src/main/java/io/github/headlesshq/headlessmc/lwjgl/LwjglProperties.java)。

例如，你可以设置 `hmc.gamedir` 以在另一个目录中运行游戏。

### 内存启动和 GraalVM

使用 `-inmemory` 标志，HeadlessMc 甚至可以在运行 HeadlessMc 本身的同一个 JVM 中启动游戏。
这使得 Minecraft 可以在任何可以运行 JVM 的地方真正运行。

这在 GraalVM 上是不可能的。
此外，HeadlessMc 的插件系统和插桩过程
在 GraalVM 中也很难实现。

但我们提供 GraalVM 镜像，它们基本上是 HeadlessMc 本身的启动器：
它们查找/下载合适的 Java 发行版并在其上运行 HeadlessMc，
而用户无需安装 Java。

### 关于命令参数的说明

传递给命令的参数必须使用空格分隔。如果你想传递包含空格的参数
你需要使用引号转义它，如下所示：
`"argument with spaces"`。
引号和反斜杠可以通过使用反斜杠转义。
所以 `msg "A text with spaces"` 将发送聊天消息 `A text with spaces`，
而 `msg "\"A text with spaces\"" additional space`
将发送聊天消息 `"A text with spaces"`，参数 `additional space` 将被丢弃。

## 构建、开发和贡献

只需运行 `./gradlew build` 或将 [build.gradle](build.gradle)
导入到你选择的 IDE，你应该就可以开始了。

为了保持与旧版 Java 和 Minecraft 版本的兼容性
HeadlessMc 使用 Java 语言级别 8。它可以使用任何 JDK &geq; 8 构建，但不能使用语言特性 > 8。
HeadlessMc 使用 [project lombok](https://github.com/projectlombok/lombok)
来消除 Java 样板代码。

（稀疏的）javadoc 可以在[这里](https://headlesshq.github.io/headlessmc/javadoc/)找到。

欢迎贡献！

### 插件

你也可以为 HeadlessMc 编写插件。
插件可以通过 `headlessmc-launcher-wrapper` 运行，
它在另一个类加载器上启动 `headlessmc-launcher`。
你可以在[这里](headlessmc-launcher-wrapper/src/testPlugin)找到一个小示例。

## 许可证和库

我们使用的一些很酷的库：

*   [MinecraftAuth by RaphiMC](https://github.com/RaphiMC/MinecraftAuth)
*   [Deencapsulation by xxDark](https://github.com/xxDark/deencapsulation)
*   [Forge-CLI by TeamKun](https://github.com/TeamKun/ForgeCLI)，我们[定制了它](https://github.com/3arthqu4ke/ForgeCLI)。

HeadlessMc 根据 [MIT 许可证](LICENSE) 许可。

