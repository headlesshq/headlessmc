package io.github.headlesshq.headlessmc.logging;

import io.github.headlesshq.headlessmc.console.output.SecretService;
import org.jboss.logmanager.ExtLogRecord;
import org.jboss.logmanager.Level;
import org.jboss.logmanager.formatters.PatternFormatter;
import org.jboss.logmanager.handlers.ConsoleHandler;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.util.logging.Handler;

import static org.junit.jupiter.api.Assertions.*;

class CensoringFormatterTest {
    @Test
    void closingTheHandlerDoesNotUseTheSecretService() {
        // Quarkus closes the console handler after the CDI container shut down,
        // the SecretService, a client proxy, cannot be resolved anymore then
        SecretService shutDown = message -> {
            throw new IllegalStateException("ArC container not initialized");
        };
        PatternFormatter pattern = new PatternFormatter("%s%n") {
            @Override
            public String getHead(Handler handler) {
                return "head ";
            }

            @Override
            public String getTail(Handler handler) {
                return "tail";
            }
        };
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ConsoleHandler handler = new ConsoleHandler(new CensoringFormatter(pattern, shutDown));
        handler.setOutputStream(output);

        handler.close();

        assertEquals("head tail", output.toString());
    }

    @Test
    void recordsAreCensored() {
        SecretService service = message -> message.replace("secret", SecretService.MASK);
        CensoringFormatter formatter = new CensoringFormatter(new PatternFormatter("%s"), service);

        String formatted = formatter.format(new ExtLogRecord(Level.INFO, "a secret", CensoringFormatterTest.class.getName()));

        assertEquals("a " + SecretService.MASK, formatted);
    }

}
