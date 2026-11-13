package io.github.headlesshq.headlessmc.logging;

import io.github.headlesshq.headlessmc.console.output.SecretService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jboss.logmanager.ExtFormatter;
import org.jboss.logmanager.ExtLogRecord;

import java.util.logging.Formatter;
import java.util.logging.Handler;

/**
 * Removes secrets with the {@link SecretService} from
 * everything another {@link Formatter} produces.
 */
@RequiredArgsConstructor
public class CensoringFormatter extends ExtFormatter {
    @Getter
    private final Formatter formatter;
    private final SecretService secretService;

    @Override
    public String format(ExtLogRecord record) {
        return secretService.censorSecrets(formatter.format(record));
    }

    // The head and tail are fixed texts of the formatter, they cannot contain secrets.
    // They must not use the SecretService: Quarkus closes the console handler, which writes the tail,
    // after it shut down the CDI container, where the SecretService cannot be resolved anymore.
    @Override
    public String getHead(Handler handler) {
        return formatter.getHead(handler);
    }

    @Override
    public String getTail(Handler handler) {
        return formatter.getTail(handler);
    }

    @Override
    public boolean isCallerCalculationRequired() {
        return !(formatter instanceof ExtFormatter extFormatter) || extFormatter.isCallerCalculationRequired();
    }

}
