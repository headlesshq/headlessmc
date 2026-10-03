package net.minecraft.launchwrapper;

import java.net.URL;
import java.net.URLClassLoader;

public class LaunchClassLoader extends URLClassLoader {
    public LaunchClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
        throw new RuntimeException("stub");
    }

    public void registerTransformer(String transformerClassName) {
        throw new RuntimeException("stub registerTransformer " + transformerClassName);
    }

}
