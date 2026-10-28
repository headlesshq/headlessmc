package io.github.headlesshq.headlessmc.console.output;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Default implementation of the {@link SecretService}.
 * Secrets shorter than {@value #MIN_LENGTH} characters are ignored,
 * as masking them would garble unrelated output.
 * <p>
 * The secrets of a source that fails, or of all sources if a source logs
 * while it is being queried, are taken from the last successful query,
 * so that a secret that was known once stays hidden.
 */
public class SecretServiceImpl implements SecretService {
    static final int MIN_LENGTH = 6;

    private final ThreadLocal<Boolean> censoring = new ThreadLocal<>();
    private final List<SecretSource> sources;
    private volatile Set<String> lastSecrets = Collections.emptySet();

    public SecretServiceImpl(List<SecretSource> sources) {
        this.sources = new ArrayList<>(sources);
    }

    @Override
    public String censorSecrets(String message) {
        if (message.isEmpty()) {
            return message;
        }

        String result = message;
        for (String secret : getSecrets()) {
            if (result.contains(secret)) {
                result = result.replace(secret, MASK);
            }
        }

        return result;
    }

    private Set<String> getSecrets() {
        if (Boolean.TRUE.equals(censoring.get())) {
            return lastSecrets;
        }

        censoring.set(Boolean.TRUE);
        try {
            Set<String> secrets = new LinkedHashSet<>();
            for (SecretSource source : sources) {
                try {
                    addAll(secrets, source.getSecrets());
                } catch (RuntimeException e) {
                    secrets.addAll(lastSecrets);
                }
            }

            List<String> sorted = new ArrayList<>(secrets);
            // longer secrets first, so that a secret containing another one is masked as a whole
            sorted.sort(Comparator.comparingInt(String::length).reversed());
            Set<String> result = Collections.unmodifiableSet(new LinkedHashSet<>(sorted));
            lastSecrets = result;
            return result;
        } finally {
            censoring.remove();
        }
    }

    private static void addAll(Set<String> secrets, Collection<String> toAdd) {
        for (String secret : toAdd) {
            if (secret != null && secret.trim().length() >= MIN_LENGTH) {
                secrets.add(secret);
            }
        }
    }

}
