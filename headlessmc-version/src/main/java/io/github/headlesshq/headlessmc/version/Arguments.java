package io.github.headlesshq.headlessmc.version;

import org.jetbrains.annotations.Unmodifiable;

import java.util.List;

public interface Arguments extends Iterable<Argument> {
    @Unmodifiable
    List<Argument> getArguments();

    boolean isInNewFormat();

}
