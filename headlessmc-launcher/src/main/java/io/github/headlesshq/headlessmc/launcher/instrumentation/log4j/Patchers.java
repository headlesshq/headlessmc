package io.github.headlesshq.headlessmc.launcher.instrumentation.log4j;

import io.github.headlesshq.headlessmc.launcher.instrumentation.Transformer;
import lombok.experimental.UtilityClass;

@UtilityClass
public class Patchers {
    public static final Transformer JNDI = new Log4jPatcher(
        "org/apache/logging/log4j/core/lookup/JndiLookup");
    public static final Transformer LOOKUP = new Log4jPatcher(
        "org/apache/logging/log4j/core/lookup/Interpolator");

}
