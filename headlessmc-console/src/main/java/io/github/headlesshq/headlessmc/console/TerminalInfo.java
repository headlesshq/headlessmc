package io.github.headlesshq.headlessmc.console;

import lombok.Value;
import lombok.experimental.Accessors;

/**
 * Size and capabilities of a terminal.
 */
@Value
@Accessors(fluent = true)
public class TerminalInfo {
    public static final TerminalInfo UNKNOWN = new TerminalInfo(0, false);

    /**
     * The number of columns of the terminal, {@code 0} if unknown, e.g. for dumb terminals.
     */
    int width;
    /**
     * {@code true} if the terminal can move the cursor up and down, e.g. to redraw multiple progress bars at once.
     */
    boolean cursorMovement;

    public TerminalInfo(int width, boolean cursorMovement) {
        this.width = Math.max(0, width);
        this.cursorMovement = cursorMovement;
    }

}
