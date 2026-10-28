package io.github.headlesshq.headlessmc.console.output;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class SecretServiceImplTest {
    private static SecretService service(SecretSource... sources) {
        return new SecretServiceImpl(Arrays.asList(sources));
    }

    @Test
    void replacesEveryOccurrenceOfEverySecret() {
        SecretService service = service(
            () -> Collections.singletonList("access-token"),
            () -> Collections.singletonList("api-key-123")
        );

        assertEquals(
            "token " + SecretService.MASK + ", key " + SecretService.MASK + ", again " + SecretService.MASK,
            service.censorSecrets("token access-token, key api-key-123, again access-token")
        );
    }

    @Test
    void leavesMessagesWithoutSecretsAlone() {
        SecretService service = service(() -> Collections.singletonList("access-token"));

        assertEquals("hello", service.censorSecrets("hello"));
        assertEquals("", service.censorSecrets(""));
    }

    @Test
    void ignoresShortAndBlankSecrets() {
        SecretService service = service(() -> Arrays.asList("", "      ", "abc", null));

        assertEquals("abc is fine", service.censorSecrets("abc is fine"));
    }

    @Test
    void masksSecretsContainingOtherSecretsAsAWhole() {
        SecretService service = service(() -> Arrays.asList("secret", "secret-and-more"));

        assertEquals(SecretService.MASK, service.censorSecrets("secret-and-more"));
    }

    @Test
    void picksUpNewSecrets() {
        List<String> secrets = new ArrayList<>();
        SecretService service = service(() -> secrets);
        assertEquals("new-secret", service.censorSecrets("new-secret"));

        secrets.add("new-secret");

        assertEquals(SecretService.MASK, service.censorSecrets("new-secret"));
    }

    @Test
    void keepsTheLastKnownSecretsOfAFailingSource() {
        AtomicBoolean fail = new AtomicBoolean();
        SecretService service = service(() -> {
            if (fail.get()) {
                throw new IllegalStateException("failed");
            }

            return Collections.singletonList("known-secret");
        });
        assertEquals(SecretService.MASK, service.censorSecrets("known-secret"));

        fail.set(true);

        assertEquals(SecretService.MASK, service.censorSecrets("known-secret"));
    }

    @Test
    void usesTheLastKnownSecretsWhenASourceCensorsWhileItIsQueried() {
        AtomicReference<SecretService> service = new AtomicReference<>();
        AtomicReference<String> nested = new AtomicReference<>();
        service.set(service(() -> {
            nested.set(service.get().censorSecrets("logged known-secret"));
            return Collections.singletonList("known-secret");
        }));

        assertEquals(SecretService.MASK, service.get().censorSecrets("known-secret"));
        assertEquals("logged known-secret", nested.get(), "nothing known yet on the first query");

        service.get().censorSecrets("again");

        assertEquals("logged " + SecretService.MASK, nested.get());
    }

}
