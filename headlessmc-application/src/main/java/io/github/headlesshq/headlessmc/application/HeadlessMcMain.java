package io.github.headlesshq.headlessmc.application;

import io.github.headlesshq.headlessmc.java.args.ArgPair;
import io.github.headlesshq.headlessmc.java.args.SystemPropertyUtil;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.annotations.QuarkusMain;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.ArrayList;
import java.util.List;

/**
 * The main entrypoint of HeadlessMc.
 * Runs {@link Quarkus#run(Class, String...)}.
 */
@QuarkusMain
public class HeadlessMcMain {
    @SuppressWarnings("UnnecessaryModifier")
    public static void main(String[] args) {
        String[] actualArgs = parseSystemProperties(args);
        Quarkus.run(HeadlessMcApplication.class, actualArgs);
        // TODO: handle McLaunched
    }

    @VisibleForTesting
    static String[] parseSystemProperties(String[] args) {
        // https://github.com/headlesshq/headlessmc/issues/420
        // By Default GraalVM parses SystemProperties.
        // We disabled this behavior as GraalVM parses them from anywhere in the args:
        // headlessmc -Dkey=value launch --jvm -Dsome=value 26.1
        // would result in -Dsome=value also being parsed and removed from the args.
        // So we disabled GraalVM parsing with -H:-ParseRuntimeOptions in scripts/build-native
        // and do it ourselves, parsing SystemProperties only until the actual arguments start:
        boolean argsStarted = false;
        List<String> actualArgs = new ArrayList<>(args.length);
        for (String arg : args) {
            if (!argsStarted && SystemPropertyUtil.isSystemProperty(arg)) {
                ArgPair pair = SystemPropertyUtil.parseSystemProperty(arg);
                System.setProperty(pair.arg(), pair.value() == null ? "" : pair.value());
                continue;
            }

            argsStarted = true;
            actualArgs.add(arg);
        }

        return actualArgs.toArray(new String[0]);
    }

}
