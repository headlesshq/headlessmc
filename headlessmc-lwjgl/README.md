# HeadlessMc-LWJGL

This module is responsible for instrumenting the LWJGL library
and thus making the Minecraft client "headless".
It consists of two parts: A transformer that transforms class
files and the so called RedirectionAPI.The transformer is usually
called from HeadlessMc's instrumentation,
to transform LWJGL before running the game.
But it can also run at runtime as a Java agent
or as a LaunchWrapper Tweaker.
If you want to do that you also need to add the system property
`-Djoml.nounsafe=true` to your game, and, if you are
running on fabric, the path to the headlessmc-lwjgl
agent jar to the system property `fabric.systemLibraries`.  

The transformer will transform every `org.lwjgl`class in the following way:
Every method body will be replaced with a call to the RedirectionAPI:

```java
public <type> method(<arg>... args) {
    return (<type>) RedirectionApi.invoke(this, "<owner>;method(<arg>)<type>", <type>.class, args);
}
```

The RedirectionApi can return a default value for all
datatypes except abstract classes (interfaces will be implemented
using `java.lang.reflect.Proxy`), 
we can also redirect a call manually like this:

```java
RedirectionApi.getRedirectionManager().redirect("<owner>;method(<arg.type>)<type>", <Redirection>);
```

These custom redirections are needed in some
cases to ensure that the game does not crash.
E.g. for all methods returning Buffers, 
as those classes cannot be instantiated easily.
All redirections can be found in the
[redirections package](src/main/java/io/github/headlesshq/headlessmc/lwjgl/redirections).

Minecraft 26.3 uses SDL for windowing and RenderPearl for rendering. The headless
redirections use OpenGL and report Vulkan unavailable. Shaderc results and SPVC
reflection are stubbed along with drawing; these are not native shader modules.
Shader syntax and compilation errors are not checked in this mode. A successful
headless launch must not be treated as shader validation. The client logs this
limitation once when shader bytes are first requested.
The SDL swap call uses the existing `hmc.lwjgl.update_sleep` setting. Display
enumeration reports the same display ID as the primary-display query. Buffer
allocations and graphics-provider proxies are shared redirections rather than
SDL-specific behavior.

For macOS clients launched in a separate JVM, a small Java agent skips
Minecraft's native window-menu setup in no-render mode. SDL has not created a
Cocoa window or menu in that mode. The hook runs as the class loads, keeping
signed game files intact and covering NeoForge's separate game jar. The
ordinary rendering path keeps the menu setup. The experimental `-inmemory`
launcher receives the same hook when started through the executable launcher
wrapper on Java 9 or later. Its agent checks that the game class loader can see
the SDL redirections before skipping the menu setup; graphical clients retain
the original method. Custom same-process hosts must install the menu agent at
JVM startup themselves.

On Linux with Java 25, Fabric, NeoForge and Forge have passed the
`mc-runtime-test` fresh-world smoke test: create a world, load the player and
chunks, wait 100 player ticks, then save and exit. This does not validate native
rendering, screenshots, gameplay mods or other operating systems. Separate
Linux runs without LWJGL redirection also completed a GameTest on all three
loaders using SDL offscreen and Mesa llvmpipe OpenGL, exercising the ordinary
shader and rendering path.

An example:

```java
manager.redirect("Lorg/lwjgl/BufferUtils;createFloatBuffer(I)"Ljava/nio/FloatBuffer;",
                         (obj, desc, type, args) -> FloatBuffer.wrap(
                             new float[(int) args[0]]));
```
