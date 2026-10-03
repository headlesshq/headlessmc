package io.github.headlesshq.headlessmc.launcher;

import lombok.Getter;

// TODO: make use of this
//  problem is that this should reach up into the QuarkusEntryPoint...
//  not just HeadlessMcApplication main
//  - could spawn separate non-daemon thread?
/**
 * Special exception thrown to signalize HeadlessMc
 * that mc has been launched and that we can garbage collect
 * HeadlessMc and wait for the Mc process to exit.
 */
@Getter
public class McLaunchedExitHmcException extends RuntimeException {
    private final Process process;

    public McLaunchedExitHmcException(Process process) {
        this.process = process;
    }

}
