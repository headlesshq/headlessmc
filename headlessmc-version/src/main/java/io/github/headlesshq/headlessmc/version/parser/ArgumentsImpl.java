package io.github.headlesshq.headlessmc.version.parser;

import io.github.headlesshq.headlessmc.version.Argument;
import io.github.headlesshq.headlessmc.version.Arguments;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.List;

@Data
final class ArgumentsImpl implements Arguments {
    private final List<Argument> arguments;
    private final boolean inNewFormat;

    @Override
    public @NotNull Iterator<Argument> iterator() {
        return arguments.iterator();
    }

}
