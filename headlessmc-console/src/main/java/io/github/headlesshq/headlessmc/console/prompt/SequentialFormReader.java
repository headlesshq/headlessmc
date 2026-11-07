package io.github.headlesshq.headlessmc.console.prompt;

import io.github.headlesshq.headlessmc.console.Console;
import io.github.headlesshq.headlessmc.console.ConsoleException;
import io.github.headlesshq.headlessmc.console.Password;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Locale;

/**
 * Asks the {@link Prompt}s of a {@link Form} one after another
 * with {@link Console#read(String)} and {@link Console#readPassword(String)}.
 * This is the default implementation of {@link Console#ask(Form)}.
 * <p>
 * Required prompts, choices and confirmations are asked again if the answer is empty or invalid,
 * up to {@link #MAX_ATTEMPTS} times, so that a closed or scripted input cannot loop forever.
 */
@RequiredArgsConstructor
public final class SequentialFormReader {
    public static final int MAX_ATTEMPTS = 3;

    private final Console console;

    public Answers ask(Form form) throws ConsoleException {
        Answers answers = new Answers();
        try {
            if (form.getPrompts().size() > 1) {
                console.write(form.getTitle() + " (Ctrl+C to abort)");
            }

            for (Prompt prompt : form.getPrompts()) {
                answers.put(prompt.getId(), ask(prompt));
            }

            return answers;
        } catch (RuntimeException | Error e) {
            answers.close();
            throw e;
        }
    }

    private Object ask(Prompt prompt) {
        if (prompt.getType() == Prompt.Type.CHOICE) {
            List<String> choices = prompt.getChoices();
            for (int i = 0; i < choices.size(); i++) {
                console.write("  " + (i + 1) + ". " + choices.get(i));
            }
        }

        String question = question(prompt);
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            Object answer = prompt.getType() == Prompt.Type.SECRET
                ? readSecret(prompt, question)
                : parse(prompt, console.read(question));
            if (answer != null) {
                return answer;
            }
        }

        throw new ConsoleException("No valid answer for \"" + prompt.getMessage() + "\" after " + MAX_ATTEMPTS + " attempts.");
    }

    private @Nullable Password readSecret(Prompt prompt, String question) {
        Password password = console.readPassword(question);
        if (prompt.isRequired() && password.get().length == 0) {
            password.close();
            console.write("A value is required.");
            return null;
        }

        return password;
    }

    private @Nullable Object parse(Prompt prompt, String input) {
        String trimmed = input.trim();
        switch (prompt.getType()) {
            case CONFIRM:
                if (trimmed.isEmpty()) {
                    return Boolean.parseBoolean(prompt.getDefaultValue());
                }

                String lower = trimmed.toLowerCase(Locale.ENGLISH);
                if (lower.equals("y") || lower.equals("yes")) {
                    return Boolean.TRUE;
                } else if (lower.equals("n") || lower.equals("no")) {
                    return Boolean.FALSE;
                }

                console.write("Please answer y or n.");
                return null;
            case CHOICE:
                String choice = choice(prompt.getChoices(), trimmed);
                if (choice == null) {
                    console.write("Please enter a number from 1 to " + prompt.getChoices().size() + ".");
                }

                return choice;
            default:
                if (!trimmed.isEmpty()) {
                    return input;
                }

                if (prompt.isRequired()) {
                    console.write("A value is required.");
                    return null;
                }

                return prompt.getDefaultValue() == null ? "" : prompt.getDefaultValue();
        }
    }

    private static @Nullable String choice(List<String> choices, String input) {
        try {
            int index = Integer.parseInt(input);
            if (index >= 1 && index <= choices.size()) {
                return choices.get(index - 1);
            }
        } catch (NumberFormatException ignored) {
            // maybe the choice itself has been entered
        }

        for (String choice : choices) {
            if (choice.equalsIgnoreCase(input)) {
                return choice;
            }
        }

        return null;
    }

    static String question(Prompt prompt) {
        StringBuilder question = new StringBuilder(prompt.getMessage());
        switch (prompt.getType()) {
            case CONFIRM:
                question.append(Boolean.parseBoolean(prompt.getDefaultValue()) ? " [Y/n]" : " [y/N]");
                break;
            case CHOICE:
                question.append(" (1-").append(prompt.getChoices().size()).append(')');
                break;
            case SECRET:
                question.append(prompt.isRequired() ? " (hidden)" : " (optional, hidden)");
                break;
            default:
                if (prompt.getDefaultValue() != null && !prompt.getDefaultValue().isEmpty()) {
                    question.append(" (default: ").append(prompt.getDefaultValue()).append(')');
                } else if (!prompt.isRequired()) {
                    question.append(" (optional)");
                }
        }

        return question.append(": ").toString();
    }

}
