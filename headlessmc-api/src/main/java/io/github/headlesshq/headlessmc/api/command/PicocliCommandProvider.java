package io.github.headlesshq.headlessmc.api.command;

@FunctionalInterface
public interface PicocliCommandProvider {
    Object getPicocliCommand();

}
