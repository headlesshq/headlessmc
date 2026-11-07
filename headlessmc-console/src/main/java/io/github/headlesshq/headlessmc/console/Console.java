package io.github.headlesshq.headlessmc.console;

import io.github.headlesshq.headlessmc.console.cache.CachedConsole;
import io.github.headlesshq.headlessmc.console.input.Input;
import io.github.headlesshq.headlessmc.console.output.Output;
import io.github.headlesshq.headlessmc.console.prompt.Answers;
import io.github.headlesshq.headlessmc.console.prompt.Form;
import io.github.headlesshq.headlessmc.console.prompt.SequentialFormReader;

import java.util.Optional;

public interface Console extends Input, Output {
    int SORT_JLINE = 100;
    int SORT_CONSOLE = 200;
    int SORT_STD = 300;
    int SORT_FILE_DESCRIPTOR = 400;

    void write(String message);

    void write(String message, String nextLine);

    String read();

    Password readPassword();

    default String read(String message) {
        write(message, "");
        return read();
    }

    default Password readPassword(String message) {
        write(message, "");
        return readPassword();
    }

    /**
     * Asks the user everything declared by the given {@link Form}.
     * Prefer this over multiple calls to {@link #read(String)},
     * as it tells the console about all questions up front,
     * e.g. an MCP server can ask them all at once.
     * By default, the prompts are asked one after another, see {@link SequentialFormReader}.
     *
     * @param form the prompts to ask.
     * @return the answers, close them to clear secrets.
     * @throws ConsoleException.Interrupted if the user aborted, e.g. by pressing Ctrl+C.
     * @throws ConsoleException if the form could not be asked.
     */
    default Answers ask(Form form) throws ConsoleException {
        return new SequentialFormReader(this).ask(form);
    }

    /**
     * Asks the user a yes/no question.
     *
     * @param message      the question.
     * @param defaultValue the answer if the user just presses enter.
     * @return whether the user confirmed.
     * @throws ConsoleException.Interrupted if the user aborted, e.g. by pressing Ctrl+C.
     */
    default boolean confirm(String message, boolean defaultValue) throws ConsoleException {
        try (Answers answers = ask(Form.builder(message).confirm("confirm", message, defaultValue).build())) {
            return answers.getBoolean("confirm");
        }
    }

    default Optional<ConsoleExtensions> extensions() {
        return Optional.empty();
    }

    // should not be called directly, rather use ConsoleCachingService
    default CachedConsole cache() throws ConsoleException {
        return new CachedConsole() {
            @Override
            public Console get() {
                return Console.this;
            }

            @Override
            public void close() {

            }
        };
    }

}
