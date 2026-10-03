package io.github.headlesshq.headlessmc.commands.server;

import io.github.headlesshq.headlessmc.console.ArgSplitter;
import io.github.headlesshq.headlessmc.java.args.ArgPair;
import io.github.headlesshq.headlessmc.java.args.SystemPropertyUtil;
import lombok.experimental.UtilityClass;
import org.jspecify.annotations.Nullable;

import java.util.*;

/**
 * Helps with parsing jvm args.
 */
@UtilityClass
public class VmArgsUtil {
    public static SequencedMap<String, @Nullable String> removeSystemProperties(Iterator<String> jvmArgs) {
        SequencedMap<String, @Nullable String> result = new LinkedHashMap<>();
        while (jvmArgs.hasNext()) {
            String next = jvmArgs.next();
            if (SystemPropertyUtil.isSystemProperty(next)) {
                ArgPair pair = SystemPropertyUtil.parseSystemProperty(next);
                result.put(pair.arg(), pair.value());
                jvmArgs.remove();
            }
        }

        return result;
    }

    public static List<String> parseArgs(ArgSplitter splitter, @Nullable String args) {
        if (args == null) {
            return List.of();
        }

        return Arrays.asList(splitter.split(args));
    }

}
