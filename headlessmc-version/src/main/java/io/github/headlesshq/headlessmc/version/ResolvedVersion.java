package io.github.headlesshq.headlessmc.version;

import lombok.Data;
import org.jetbrains.annotations.Nullable;

import java.util.Deque;

@Data
public class ResolvedVersion {
    private final Deque<Version> family;
    private final @Nullable Version parent;
    private final Version version;

}
