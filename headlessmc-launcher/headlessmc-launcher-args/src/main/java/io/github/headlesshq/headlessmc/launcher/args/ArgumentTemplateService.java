package io.github.headlesshq.headlessmc.launcher.args;

import io.github.headlesshq.headlessmc.version.util.TemplateStrings;
import jakarta.enterprise.context.ApplicationScoped;
import org.jspecify.annotations.Nullable;

import java.util.Map;

@ApplicationScoped
public class ArgumentTemplateService {
    public Arguments process(Arguments arguments, TemplateStrings templates) {
        Arguments result = Arguments.mutable();
        for (Map.Entry<String, @Nullable String> systemProperty : arguments.systemProperties().entrySet()) {
            String key = templates.process(systemProperty.getKey());
            String value = systemProperty.getValue() == null ? null : templates.process(systemProperty.getValue());
            result.systemProperties().put(key, value);
        }

        for (String vmArg : arguments.vmArgs()) {
            result.vmArgs().add(templates.process(vmArg));
        }

        for (String gameArg : arguments.gameArgs()) {
            result.gameArgs().add(templates.process(gameArg));
        }

        return result;
    }

}
