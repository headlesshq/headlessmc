package io.github.headlesshq.headlessmc.console.input;

import io.github.headlesshq.headlessmc.console.Password;

public interface Input {
    String read();

    Password readPassword();

}
