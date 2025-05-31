package io.github.headlesshq.headlessmc.version.parser;

import io.github.headlesshq.headlessmc.version.Arguments;
import lombok.Data;
import org.jetbrains.annotations.Nullable;

@Data
final class ArgumentParseResult {
    private final @Nullable Arguments gameArguments;
    private final @Nullable Arguments jvmArguments;

}
