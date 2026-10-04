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
An example:

```java
manager.redirect("Lorg/lwjgl/BufferUtils;createFloatBuffer(I)"Ljava/nio/FloatBuffer;",
                         (obj, desc, type, args) -> FloatBuffer.wrap(
                             new float[(int) args[0]]));
```
