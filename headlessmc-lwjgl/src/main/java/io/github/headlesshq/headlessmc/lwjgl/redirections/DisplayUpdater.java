package io.github.headlesshq.headlessmc.lwjgl.redirections;

import lombok.RequiredArgsConstructor;
import io.github.headlesshq.headlessmc.lwjgl.LwjglProperties;
import io.github.headlesshq.headlessmc.lwjgl.api.Redirection;

/**
 * Since Minecrafts Gameloop is just a while(True) loop which calls {@code
 * org.lwjgl.opengl.Display.update()} and {@link Thread#yield()}. Once we
 * redirect the {@code update()} call the loop just runs and runs and puts some
 * heavy load on the CPU. This {@link Redirection} fixes that by sleeping for a
 * set amount of time, configurable by the SystemProperty {@link
 * LwjglProperties#DISPLAY_UPDATE}.
 */
@RequiredArgsConstructor
public class DisplayUpdater implements Redirection {
    public static final String DESC = "Lorg/lwjgl/opengl/Display;update()V";

    private final long time;

    public DisplayUpdater() {
        this(getTime());
    }

    private static long getTime() {
        try {
            return Long.parseLong(
                System.getProperty(LwjglProperties.DISPLAY_UPDATE, "10"));
        } catch (NumberFormatException nfe) {
            return 10L;
        }
    }

    @Override
    public Object invoke(Object obj, String desc, Class<?> type, Object... args)
        throws Throwable {
        // we could scale this with the refresh rate?
        Thread.sleep(time);
        return null;
    }

}
