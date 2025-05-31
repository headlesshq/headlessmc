package io.github.headlesshq.headlessmc.launcher.launch;

import io.github.headlesshq.headlessmc.launcher.version.Argument;
import io.github.headlesshq.headlessmc.launcher.version.Features;
import io.github.headlesshq.headlessmc.launcher.version.Rule;
import io.github.headlesshq.headlessmc.os.OS;
import lombok.CustomLog;
import lombok.val;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CustomLog
class ArgumentAdapter {
    private final Map<String, String> values = new HashMap<>();
    private final List<Argument> arguments;

    public ArgumentAdapter(List<Argument> arguments) {
        this.arguments = new ArrayList<>(arguments);
    }

    public void add(String arg, String value) {
        if (value != null) {
            values.put(arg, value);
        } else {
            log.debug("Value for " + arg + " was null");
        }
    }

    public void remove(String argument) {
        arguments.removeIf(a -> argument.equalsIgnoreCase(a.getValue()));
    }

    public List<String> build(OS os, Features features, String type) {
        val result = new ArrayList<String>();
        for (val arg : arguments) {
            if (arg.getType().equals(type)
                && arg.getRule().apply(os, features) == Rule.Action.ALLOW) {
                val inputs = arg.getInputs();
                String current = arg.getValue();
                for (val input : inputs) {
                    String value = values.get(input);
                    if (value == null) {
                        log.warning("No value found for " + input);
                    } else {
                        // TODO: check if we need to escape input?
                        current = current.replace(input, value);
                    }
                }

                result.add(current);
            }
        }

        return result;
    }

}
