package io.github.headlesshq.headlessmc.api.command.picocli;

import io.github.headlesshq.headlessmc.api.command.Splitter;

import java.util.ArrayList;
import java.util.List;

public class LegacySplitter implements Splitter {
    private final char delimiter;

    public LegacySplitter(char delimiter) {
        if (delimiter == '"' || delimiter == '\\') {
            throw new IllegalArgumentException("Illegal delimiter '" + delimiter + "'");
        }

        this.delimiter = delimiter;
    }

    @Override
    public String[] split(String string) {
        List<String> result = new ArrayList<>();
        boolean quoted = false;
        boolean escaped = false;
        StringBuilder currentArg = new StringBuilder();
        for (int i = 0; i < string.length(); i++) {
            char ch = string.charAt(i);
            if (escaped) {
                currentArg.append(ch);
                escaped = false;
            } else if (ch == '\\') {
                escaped = true;
            } else if (ch == '"') {
                if (quoted) {
                    result.add(currentArg.toString());
                    currentArg = new StringBuilder();
                }

                quoted = !quoted;
            } else if (ch == delimiter) {
                if (quoted) {
                    currentArg.append(ch);
                } else {
                    if (currentArg.length() != 0) {
                        result.add(currentArg.toString());
                        currentArg = new StringBuilder();
                    }
                }
            } else {
                currentArg.append(ch);
            }
        }

        if (currentArg.length() > 0) {
            result.add(currentArg.toString());
        }

        return result.toArray(new String[0]);
    }

}
