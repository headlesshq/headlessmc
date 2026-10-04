open module io.github.headlesshq.headlessmc.lwjgl {
    exports io.github.headlesshq.headlessmc.lwjgl;
    exports io.github.headlesshq.headlessmc.lwjgl.api;
    exports io.github.headlesshq.headlessmc.lwjgl.transformer;
    exports io.github.headlesshq.headlessmc.lwjgl.redirections;
    exports io.github.headlesshq.headlessmc.lwjgl.redirections.stb;
    exports io.github.headlesshq.headlessmc.lwjgl.util;
    exports io.github.headlesshq.headlessmc.lwjgl.agent;

    requires java.desktop;
    requires static java.instrument;
    requires static org.objectweb.asm.tree;
    requires static org.objectweb.asm;
}
