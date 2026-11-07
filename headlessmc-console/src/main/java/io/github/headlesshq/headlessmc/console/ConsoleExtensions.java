package io.github.headlesshq.headlessmc.console;

import lombok.Data;

public interface ConsoleExtensions {
    String edit(String initialString);

    // calls to Console are expensive because we need to open a new Terminal everytime
    // by also returning the width we save a call to getWidth
    String read(String prompt, Completions completions);

    int getWidth();

    /**
     * Returns the size and capabilities of the terminal without blocking,
     * even while another thread is waiting for input.
     * This is cheap enough to be called repeatedly, e.g. on every render of a progress bar,
     * so the result might be slightly outdated.
     *
     * @return the size and capabilities of the terminal, {@link TerminalInfo#UNKNOWN} if they are not known.
     */
    default TerminalInfo terminalInfo() {
        return TerminalInfo.UNKNOWN;
    }

}
