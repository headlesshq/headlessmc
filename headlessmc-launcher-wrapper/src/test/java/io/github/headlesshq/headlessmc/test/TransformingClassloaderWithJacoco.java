package io.github.headlesshq.headlessmc.test;

import io.github.headlesshq.headlessmc.wrapper.plugin.TransformerPlugin;
import io.github.headlesshq.headlessmc.wrapper.plugin.TransformingClassloader;

import java.util.List;

public class TransformingClassloaderWithJacoco extends TransformingClassloader {
    public TransformingClassloaderWithJacoco(URL[] urls, ClassLoader parent, URLClassLoader transformerClassloader, List<TransformerPlugin> plugins) {
        super(urls, parent, transformerClassloader, plugins);
    }

    @Override
    protected byte[] instrument(String name, byte[] classBytes) throws ClassNotFoundException {
        byte[] result = super.instrument(name, classBytes);
        return result;
    }

}
