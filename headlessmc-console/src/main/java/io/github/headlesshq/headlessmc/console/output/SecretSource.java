package io.github.headlesshq.headlessmc.console.output;

import java.util.Collection;

/**
 * Supplies secrets, like access tokens or api keys,
 * which the {@link SecretService} removes from everything HeadlessMc prints or logs.
 * Sources are queried whenever a message is censored, so they should return the current secrets
 * and be cheap to call.
 * They must not rely on their own output being censored, as the {@link SecretService}
 * falls back to the secrets it knew before if a source logs while being queried.
 */
public interface SecretSource {
    /**
     * @return the secrets that must not be printed.
     */
    Collection<String> getSecrets();

}
