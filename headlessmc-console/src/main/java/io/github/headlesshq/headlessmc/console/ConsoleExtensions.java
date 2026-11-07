package io.github.headlesshq.headlessmc.console;

import lombok.Data;

public interface ConsoleExtensions {
    String edit(String initialString);

    // calls to Console are expensive because we need to open a new Terminal everytime
    // by also returning the width we save a call to getWidth
    String read(String prompt, Completions completions);

    int getWidth();

    /**
     * @return {@code true} if the terminal can move the cursor up and down,
     * e.g. to redraw multiple progress bars at once.
     */
    default boolean supportsCursorMovement() {
        return false;
    }

}
