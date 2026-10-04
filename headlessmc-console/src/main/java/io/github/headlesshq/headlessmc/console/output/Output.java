package io.github.headlesshq.headlessmc.console.output;

public interface Output {
    void write(String message);

    void write(String message, String nextLine);

}
