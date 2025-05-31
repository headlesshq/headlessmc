package io.github.headlesshq.headlessmc.launcher.instrumentation;

import io.github.headlesshq.headlessmc.launcher.instrumentation.log4j.Patchers;
import io.github.headlesshq.headlessmc.launcher.instrumentation.lwjgl.HmcLwjglTransformer;
import io.github.headlesshq.headlessmc.launcher.instrumentation.modlauncher.BootstrapLauncherTransformer;
import io.github.headlesshq.headlessmc.launcher.instrumentation.paulscode.PaulscodeTransformer;
import io.github.headlesshq.headlessmc.launcher.launch.LaunchOptions;
import lombok.CustomLog;
import lombok.experimental.UtilityClass;
import lombok.val;

import java.util.ArrayList;

@CustomLog
@UtilityClass
public class InstrumentationHelper {
    public static final String LWJGL_JAR = "headlessmc-lwjgl.jar";

    public static Instrumentation create(LaunchOptions options) {
        val transformers = new ArrayList<Transformer>(7);
        if (options.isLwjgl()) {
            transformers.add(new HmcLwjglTransformer());
            transformers.add(new ResourceExtractor(options.getFiles(), LWJGL_JAR));
        }

        if (options.isPaulscode()) {
            transformers.add(new PaulscodeTransformer());
        }

        if (options.isJndi()) {
            transformers.add(Patchers.JNDI);
        }

        if (options.isLookup()) {
            transformers.add(Patchers.LOOKUP);
        }

        if (options.isInMemory()) {
            transformers.add(new BootstrapLauncherTransformer());
        }

        return new Instrumentation(transformers, options.getFiles().getBase());
    }

}
