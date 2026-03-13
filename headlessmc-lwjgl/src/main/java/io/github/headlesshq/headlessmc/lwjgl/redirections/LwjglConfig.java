package io.github.headlesshq.headlessmc.lwjgl.redirections;

import io.github.headlesshq.headlessmc.lwjgl.LwjglProperties;
import lombok.experimental.UtilityClass;

@UtilityClass
public class LwjglConfig {

    public static final int GL_TEXTURE_WIDTH = 4096;
    public static final int GL_TEXTURE_INTERNAL_FORMAT_CONST = 4099;
    public static final int GL_UNIFORM_BUFFER_OFFSET_ALIGNMENT = 0x8A34;

    public static final int GL_TEXTURE_INTERNAL_FORMAT = Integer.parseInt(
        System.getProperty(LwjglProperties.GL_TEXTURE_INTERNAL_FORMAT, "32856")); //RGBA8
    public static final int TEXTURE_SIZE = Integer.parseInt(
        System.getProperty(LwjglProperties.TEXTURE_SIZE, "1024"));
    public static final boolean FULLSCREEN = Boolean.parseBoolean(
        System.getProperty(LwjglProperties.FULLSCREEN, "true"));
    public static final int SCREEN_WIDTH = Integer.parseInt(
        System.getProperty(LwjglProperties.SCREEN_WIDTH, "1920"));
    public static final int SCREEN_HEIGHT = Integer.parseInt(
        System.getProperty(LwjglProperties.SCREEN_HEIGHT, "1080"));
    public static final int REFRESH_RATE = Integer.parseInt(
        System.getProperty(LwjglProperties.REFRESH_RATE, "100"));
    public static final int BITS_PER_PIXEL = Integer.parseInt(
        System.getProperty(LwjglProperties.BITS_PER_PIXEL, "32"));
    public static final int JNI_VERSION = Integer.parseInt(
        System.getProperty(LwjglProperties.JNI_VERSION, "24"));
    public static final int UNIFORM_OFFSET_ALIGNMENT = Integer.parseInt(
        System.getProperty(LwjglProperties.UNIFORM_OFFSET_ALIGNMENT, "1"));

}
