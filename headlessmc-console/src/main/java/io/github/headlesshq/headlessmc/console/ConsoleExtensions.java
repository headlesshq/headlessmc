package io.github.headlesshq.headlessmc.console;

import lombok.Data;

public interface ConsoleExtensions {
    String edit(String initialString);

    // calls to Console are expensive because we need to open a new Terminal everytime
    // by also returning the width we save a call to getWidth
    String read(String prompt, Completions completions);

    int getWidth();

}
