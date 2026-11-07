package io.github.headlesshq.headlessmc.console.prompt;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * A single question of a {@link Form}.
 * Prompts are declared before anything is asked,
 * so that a console can decide how to ask all of them,
 * e.g. as a single form in an MCP client.
 */
@Getter
@ToString
@EqualsAndHashCode
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public final class Prompt {
    /**
     * The key of the answer in the {@link Answers}.
     */
    private final String id;
    /**
     * The question, e.g. {@code "Account name"}.
     */
    private final String message;
    private final Type type;
    /**
     * If a required prompt is answered with an empty string it will be asked again.
     * Optional prompts take their {@link #getDefaultValue()} if left empty.
     */
    private final boolean required;
    /**
     * The value used if the prompt is left empty.
     * For {@link Type#CONFIRM} this is {@code "true"} or {@code "false"},
     * for {@link Type#CHOICE} one of the {@link #getChoices()}.
     * Never set for {@link Type#SECRET}.
     */
    private final @Nullable String defaultValue;
    /**
     * The possible answers of a {@link Type#CHOICE} prompt, empty otherwise.
     */
    private final List<String> choices;

    /**
     * @return a copy of this prompt with another message, e.g. one with secrets censored.
     */
    public Prompt withMessage(String message) {
        return new Prompt(id, message, type, required, defaultValue, choices);
    }

    /**
     * @return a copy of this prompt with another default value, e.g. none if the default contains secrets.
     */
    public Prompt withDefaultValue(@Nullable String defaultValue) {
        return new Prompt(id, message, type, required, defaultValue, choices);
    }

    public enum Type {
        /**
         * A line of text, the answer is a {@link String}.
         */
        TEXT,
        /**
         * A password or token, which should not be displayed.
         * The answer is a {@link io.github.headlesshq.headlessmc.console.Password}.
         * Consoles that cannot read secrets safely, e.g. MCP, may refuse to ask required secrets
         * and skip optional ones.
         */
        SECRET,
        /**
         * A yes/no question, the answer is a {@link Boolean}.
         */
        CONFIRM,
        /**
         * One of the {@link #getChoices()}, the answer is the chosen {@link String}.
         */
        CHOICE
    }

    static List<String> noChoices() {
        return Collections.emptyList();
    }

}
