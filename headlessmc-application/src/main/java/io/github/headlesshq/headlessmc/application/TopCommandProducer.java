package io.github.headlesshq.headlessmc.application;

import io.github.headlesshq.headlessmc.commands.HeadlessMcCommand;
import io.quarkus.picocli.runtime.annotations.TopCommand;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;

/**
 * Produces the root command for the
 * {@link picocli.CommandLine} instance
 * that quarkus-picocli will use.
 */
@Dependent
public class TopCommandProducer {
    /**
     * Produces the {@link TopCommand} for the quarkus-picocli command line.
     *
     * @return {@link HeadlessMcCommand}.class
     */
    @Produces
    @Dependent
    @TopCommand
    public Object topCommand() {
        return HeadlessMcCommand.class;
    }

}
