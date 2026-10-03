package io.github.headlesshq.headlessmc.version.util;

import lombok.Getter;

import java.util.*;
import java.util.regex.Matcher;

@Getter
public class TemplateStrings {
    private final Map<String, TemplateString> lookup = new HashMap<>();
    private final Map<TemplateString, String> values = new HashMap<>();
    private final Set<TemplateString> processed = new HashSet<>();
    private final Set<String> failed = new HashSet<>();

    public boolean hasProcessed(TemplateString templateString) {
        return processed.contains(templateString);
    }

    public void add(TemplateString templateString, String value) {
        lookup.put(templateString.getTemplateString(), templateString);
        values.put(templateString, value);
    }

    public String process(String string) {
        Matcher matcher = TemplateString.REGEX.matcher(string);
        List<String> matches = new ArrayList<>();
        while (matcher.find()) {
            matches.add(matcher.group());
        }

        String result = string;
        for (String match : matches) {
            TemplateString templateString = lookup.get(match);
            if (templateString == null) {
                failed.add(match);
                result = result.replace(match, "");
            } else {
                String value = values.get(templateString);
                if (value == null) {
                    throw new IllegalStateException(templateString + " was registered without value");
                } else {
                    processed.add(templateString);
                    result = result.replace(match, value);
                }
            }
        }

        return result;
    }

    public Set<TemplateString> getTemplates() {
        return values.keySet();
    }

}
