package io.github.headlesshq.headlessmc.api.command;

import lombok.Data;
import org.jetbrains.annotations.Nullable;

@Data
public class Suggestion {
    private final String value;
    private final @Nullable String description;
    private final boolean complete;

}
