package io.github.headlesshq.headlessmc.console.output;

/**
 * Removes the secrets of all registered {@link SecretSource}s from messages,
 * before they are written to the console or a log.
 */
public interface SecretService {
    /**
     * Replaces the secrets in censored messages.
     */
    String MASK = "********";

    /**
     * @param message the message to censor.
     * @return the message with every occurrence of a secret replaced by {@link #MASK}.
     */
    String censorSecrets(String message);

    /**
     * @return a SecretService that does not censor anything.
     */
    static SecretService none() {
        return message -> message;
    }

}
