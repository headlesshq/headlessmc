package io.github.headlesshq.headlessmc.launcher.version;

import com.google.gson.JsonElement;
import lombok.RequiredArgsConstructor;
import lombok.val;
import io.github.headlesshq.headlessmc.launcher.util.CollectionUtil;
import io.github.headlesshq.headlessmc.launcher.util.JsonUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@RequiredArgsConstructor
class ArgumentFactory {
    private final RuleFactory ruleFactory;

    // TODO: the Consumer here is kinda awful
    public List<Argument> parse(JsonElement arguments, Consumer<Boolean> format)
        throws VersionParseException {
        if (arguments == null) {
            return null;
        }

        if (!arguments.isJsonObject()) {
            format.accept(false);
            return Arrays.stream(arguments.getAsString().split(" "))
                         .map(arg -> (Argument) () -> arg)
                         .collect(Collectors.toList());
        }

        format.accept(true);
        val result = new ArrayList<Argument>();
        for (val type : arguments.getAsJsonObject().entrySet()) {
            if (!type.getValue().isJsonArray()) {
                continue;
            }

            for (val element : type.getValue().getAsJsonArray()) {
                result.addAll(parseElement(element, type.getKey()));
            }
        }

        return result;
    }

    private Collection<Argument> parseElement(JsonElement element, String type)
        throws VersionParseException {
        if (!element.isJsonObject()) {
            String arg = element.getAsString();
            return CollectionUtil.listOf(
                new ArgumentImpl(arg, type, Rule.ALLOW));
        }

        JsonElement value = element.getAsJsonObject().get("value");
        if (value == null) {
            // https://github.com/headlesshq/headlessmc/issues/141#issuecomment-2041048918
            value = element.getAsJsonObject().get("values");
            if (value == null) {
                throw new VersionParseException("Failed to parse value(s) in argument of type " + type + ", element: " + element);
            }
        }

        val rules = element.getAsJsonObject().get("rules");
        val rule = ruleFactory.parse(rules);
        return JsonUtil.toList(JsonUtil.toArray(value), e ->
            new ArgumentImpl(e.getAsString(), type, rule));
    }

}
