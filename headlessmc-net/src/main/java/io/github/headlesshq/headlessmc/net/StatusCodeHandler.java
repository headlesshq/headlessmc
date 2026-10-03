package io.github.headlesshq.headlessmc.net;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.exceptions.RequestException;
import io.github.headlesshq.headlessmc.exceptions.UncheckedInterruptedException;

import java.util.concurrent.Callable;

@FunctionalInterface
public interface StatusCodeHandler {
    void accept(int code, Callable<String> responseDescription) throws HeadlessMcException;
    
    static StatusCodeHandler error() {
        return (code, responseDescription) -> {
            if (code >= 400) {
                String response;
                try {
                    response = responseDescription.call();
                } catch (UncheckedInterruptedException | InterruptedException e) {
                    throw new UncheckedInterruptedException("Interrupted while getting error-response: " + code, e);
                } catch (/* okay-to-catch-marker */Exception e) {
                    response = "(failed to get reason: " + e.getMessage() + ")";
                }

                throw new RequestException("Status code: " + code + ", " + response);
            }
        };
    }
    
}
