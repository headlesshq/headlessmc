package io.github.headlesshq.headlessmc.version.parser;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.headlesshq.headlessmc.version.Argument;
import io.github.headlesshq.headlessmc.version.Rule;
import jakarta.inject.Inject;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Parses the either the legacy "minecraftArguments" or the new argument format:
 * <pre>
 "minecraftArguments": "--username ${auth_player_name} ...",
 "arguments": {
     "game": [
         "--username",
         "${auth_player_name}",
         {
            "rules": [
                {
                    "action": "allow",
                    "features": {
                        "is_demo_user": true
                    }
                }
            ],
            "value": "--demo"
         },
         ...
     ],
     "jvm": [
        {
            "rules": [
                {
                    "action": "allow",
                    "os": {
                        "name": "osx"
                    }
                }
            ],
            "value": [
                "-XstartOnFirstThread"
            ]
        },
        "-Dminecraft.launcher.version=${launcher_version}",
        "-cp",
        "${classpath}"
     ]
 },</pre>
 */
@CustomLog
@RequiredArgsConstructor(onConstructor_ = {@Inject})
final class ArgumentParser {
    private final RuleParser ruleParser;

    public ArgumentParseResult parse(JsonObject versionJson) {
        JsonElement argumentElement = versionJson.get("arguments");
        if (argumentElement == null) {
            String string = JsonUtil.getString(versionJson, "minecraftArguments");
            if (string == null) {
                return new ArgumentParseResult(null, null);
            }

            return parseLegacyArguments(string);
        }

        return parseArguments(argumentElement);
    }

    public ArgumentParseResult parseArguments(JsonElement arguments) throws VersionParseException {
        List<Argument> gameArgs = new ArrayList<>(parseArgumentsOfType(arguments.getAsJsonObject().get("game")));
        List<Argument> jvmArgs = new ArrayList<>(parseArgumentsOfType(arguments.getAsJsonObject().get("jvm")));
        return new ArgumentParseResult(new ArgumentsImpl(gameArgs, true), new ArgumentsImpl(jvmArgs, true));
    }

    public ArgumentParseResult parseLegacyArguments(String arguments) {
        List<Argument> gameArgs = new ArrayList<>();
        for (String arg : arguments.split(" ")) {
            gameArgs.add(new ArgumentImpl(arg, Rule.ALLOW));
        }

        return new ArgumentParseResult(new ArgumentsImpl(gameArgs, false), null);
    }

    private List<Argument> parseArgumentsOfType(JsonElement arguments) throws VersionParseException {
        List<Argument> args = new ArrayList<>();
        for (JsonElement element : arguments.getAsJsonArray()) {
            if (!element.isJsonObject()) {
                args.add(new ArgumentImpl(element.getAsString(), Rule.ALLOW));
                continue;
            }

            JsonElement value = element.getAsJsonObject().get("value");
            if (value == null) {
                // https://github.com/3arthqu4ke/headlessmc/issues/141#issuecomment-2041048918
                value = element.getAsJsonObject().get("values");
                if (value == null) {
                    throw new VersionParseException("Failed to parse argument: " + element);
                }
            }

            JsonElement rules = element.getAsJsonObject().get("rules");
            Rule rule = ruleParser.parse(rules);
            args.addAll(JsonUtil.toList(JsonUtil.toArray(value), e -> new ArgumentImpl(e.getAsString(), rule)));
        }

        return args;
    }

}
