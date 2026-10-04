package io.github.headlesshq.headlessmc.console.output;

import io.github.headlesshq.headlessmc.console.Console;

import java.io.PrintStream;

public class StdOutOutput implements OutputProvider {
    @Override
    public Output get() {
        return new Output() {
            @Override
            public void write(String message) {
                getStdOut().println(message);
            }

            @Override
            public void write(String message, String nextLine) {
                getStdOut().print(message + nextLine);
            }
        };
    }

    @Override
    public int sort() {
        return Console.SORT_STD;
    }

    PrintStream getStdOut() {
        return System.out;
    }

}
