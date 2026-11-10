package io.github.headlesshq.headlessmc.console;

/**
 * Marks commands that exit the program after they have run.
 */
public interface FinalCommand {
    /**
     * Decides, after the command has been parsed, whether running it should exit the program.
     *
     * @return {@code true} if the program should exit after this command has run.
     */
    default boolean isFinal() {
        return true;
    }

}
