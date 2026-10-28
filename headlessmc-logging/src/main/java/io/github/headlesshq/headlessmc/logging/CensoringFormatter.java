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

    @Override
    public String getHead(Handler handler) {
        return secretService.censorSecrets(formatter.getHead(handler));
    }

    @Override
    public String getTail(Handler handler) {
        return secretService.censorSecrets(formatter.getTail(handler));
    }

    @Override
    public boolean isCallerCalculationRequired() {
        return !(formatter instanceof ExtFormatter extFormatter) || extFormatter.isCallerCalculationRequired();
    }

}
