package io.github.headlesshq.headlessmc.console.prompt;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.UnaryOperator;

/**
 * Declares all {@link Prompt}s a command wants to ask the user at once,
 * e.g. the name, uuid, token and type of an offline account.
 * <p>
 * Asking through a form instead of single {@code Console.read} calls
 * lets the console know the intent up front:
 * a terminal asks the prompts one after another and the user can abort the whole form with Ctrl+C,
 * while e.g. an MCP server can send all questions to its client as a single form,
 * or refuse before asking anything if the form contains secrets it is not allowed to ask for.
 *
 * @see io.github.headlesshq.headlessmc.console.Console#ask(Form)
 */
@Getter
@ToString
@EqualsAndHashCode
public final class Form {
    /**
     * What the form is for, e.g. {@code "Create an offline account"}.
     */
    private final String title;
    private final List<Prompt> prompts;

    private Form(String title, List<Prompt> prompts) {
        this.title = title;
        this.prompts = Collections.unmodifiableList(new ArrayList<>(prompts));
    }

    /**
     * @return whether any prompt of this form asks for a secret.
     */
    public boolean containsSecrets() {
        for (Prompt prompt : prompts) {
            if (prompt.getType() == Prompt.Type.SECRET) {
                return true;
            }
        }

        return false;
    }

    /**
     * @return a copy of this form with the title and the message of every prompt mapped, e.g. to censor secrets.
     */
    public Form mapMessages(UnaryOperator<String> mapper) {
        List<Prompt> mapped = new ArrayList<>(prompts.size());
        for (Prompt prompt : prompts) {
            mapped.add(prompt.withMessage(mapper.apply(prompt.getMessage())));
        }

        return new Form(mapper.apply(title), mapped);
    }

    public static Builder builder(String title) {
        return new Builder(title);
    }

    public static final class Builder {
        private final Set<String> ids = new LinkedHashSet<>();
        private final List<Prompt> prompts = new ArrayList<>();
        private final String title;

        private Builder(String title) {
            this.title = title;
        }

        /**
         * A required line of text.
         */
        public Builder text(String id, String message) {
            return add(new Prompt(id, message, Prompt.Type.TEXT, true, null, Prompt.noChoices()));
        }

        /**
         * An optional line of text, if left empty the answer is the default value or an empty string.
         */
        public Builder optionalText(String id, String message, @Nullable String defaultValue) {
            return add(new Prompt(id, message, Prompt.Type.TEXT, false, defaultValue, Prompt.noChoices()));
        }

        /**
         * A required secret, e.g. a password.
         */
        public Builder secret(String id, String message) {
            return add(new Prompt(id, message, Prompt.Type.SECRET, true, null, Prompt.noChoices()));
        }

        /**
         * An optional secret, e.g. a token, if left empty or skipped the answer is an empty password.
         */
        public Builder optionalSecret(String id, String message) {
            return add(new Prompt(id, message, Prompt.Type.SECRET, false, null, Prompt.noChoices()));
        }

        /**
         * A yes/no question, if left empty the answer is the default value.
         */
        public Builder confirm(String id, String message, boolean defaultValue) {
            return add(new Prompt(id, message, Prompt.Type.CONFIRM, false, String.valueOf(defaultValue), Prompt.noChoices()));
        }

        /**
         * One of the given choices, which have to be answered.
         */
        public Builder choice(String id, String message, List<String> choices) {
            if (choices.isEmpty()) {
                throw new IllegalArgumentException("Choice prompt " + id + " has no choices");
            }

            List<String> copy = Collections.unmodifiableList(new ArrayList<>(choices));
            return add(new Prompt(id, message, Prompt.Type.CHOICE, true, null, copy));
        }

        public Form build() {
            return new Form(title, prompts);
        }

        private Builder add(Prompt prompt) {
            if (!ids.add(prompt.getId())) {
                throw new IllegalArgumentException("Duplicate prompt id " + prompt.getId());
            }

            prompts.add(prompt);
            return this;
        }
    }

}
