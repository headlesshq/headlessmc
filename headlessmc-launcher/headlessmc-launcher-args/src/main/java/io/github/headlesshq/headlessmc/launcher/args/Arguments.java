package io.github.headlesshq.headlessmc.launcher.args;

import io.github.headlesshq.headlessmc.java.args.ArgPair;
import io.github.headlesshq.headlessmc.java.args.SystemPropertyUtil;
import lombok.With;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.SequencedMap;

@With
public record Arguments(
    SequencedMap<String, @Nullable String> systemProperties,
    List<String> vmArgs,
    List<String> gameArgs // eventually we might want to parse these into ArgPairs?
) {
    public static Arguments mutable() {
        return new Arguments(new LinkedHashMap<>(), new ArrayList<>(), new ArrayList<>());
    }

    public void addVmArg(String arg) {
        if (SystemPropertyUtil.isSystemProperty(arg)) {
            ArgPair pair = SystemPropertyUtil.parseSystemProperty(arg);
            systemProperties.put(pair.arg(), pair.value());
        } else {
            vmArgs.add(arg);
        }
    }
    
}
