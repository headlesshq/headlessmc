package io.github.headlesshq.headlessmc.api.command;

import io.github.headlesshq.headlessmc.api.command.picocli.CommandLineParser;

public interface Splitter {
    String[] split(String string);

    static Splitter defaultSplitter() {
        return string -> new CommandLineParser().parse(string);
    }

}
