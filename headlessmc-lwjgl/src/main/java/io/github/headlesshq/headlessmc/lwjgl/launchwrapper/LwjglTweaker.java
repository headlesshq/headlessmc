package io.github.headlesshq.headlessmc.lwjgl.launchwrapper;

import lombok.SneakyThrows;
import io.github.headlesshq.headlessmc.lwjgl.LwjglProperties;
import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.LaunchClassLoader;

import java.io.File;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;

@SuppressWarnings("unused")
public class LwjglTweaker implements ITweaker {
    @Override
    public void acceptOptions(List<String> args, File gameDir, File assetsDir, String profile) {
        // NOP
    }

    @Override
    @SneakyThrows
    public void injectIntoClassLoader(LaunchClassLoader cl) {
        Field exceptions = cl.getClass().getDeclaredField("classLoaderExceptions");
        exceptions.setAccessible(true);
        ((Set<?>) exceptions.get(cl)).remove("org.lwjgl.");
        cl.registerTransformer("io.github.headlesshq.headlessmc.lwjgl.launchwrapper.LaunchWrapperLwjglTransformer");
    }

    @Override
    public String getLaunchTarget() {
        return System.getProperty(LwjglProperties.TWEAKER_MAIN_CLASS, "net.minecraft.client.main.Main");
    }

    @Override
    public String[] getLaunchArguments() {
        return new String[0];
    }

}
