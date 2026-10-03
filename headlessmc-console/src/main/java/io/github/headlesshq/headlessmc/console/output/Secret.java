package io.github.headlesshq.headlessmc.console.output;

import lombok.Data;

@Data
public class Secret {
    private final String secret;

    @Override
    public String toString() {
        return "***";
    }

}
