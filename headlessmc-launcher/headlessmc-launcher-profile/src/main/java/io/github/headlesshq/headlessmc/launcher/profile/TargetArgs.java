package io.github.headlesshq.headlessmc.launcher.profile;

import io.github.headlesshq.headlessmc.version.arg.Side;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * The arguments of a command that a {@link LaunchTarget} has to make sense of,
 * e.g. the {@code fabric 1.21.1} in {@code launch fabric 1.21.1}.
 *
 * @param args  the arguments. For {@link LaunchTarget#complete(TargetArgs) completions} the last argument
 *              is the one that is being typed, it can be empty.
 * @param sides the sides the command allows. {@link Side#BOTH} for commands that launch anything,
 *              only {@link Side#SERVER} for the server commands.
 */
public record TargetArgs(List<String> args, Set<Side> sides) {
    public TargetArgs {
        args = List.copyOf(args);
        sides = Set.copyOf(sides);
    }

    /**
     * @param args the arguments.
     * @return arguments for a command that allows everything.
     */
    public static TargetArgs of(List<String> args) {
        return new TargetArgs(args, Side.BOTH);
    }

    /**
     * @param args the arguments.
     * @return arguments for a command that allows everything.
     */
    public static TargetArgs of(String... args) {
        return of(List.of(args));
    }

    /**
     * @return {@code true} if the command only allows servers, e.g. {@code server add}.
     */
    public boolean isServerOnly() {
        return sides.size() == 1 && sides.contains(Side.SERVER);
    }

    /**
     * @return {@code true} if the command only allows clients.
     */
    public boolean isClientOnly() {
        return sides.size() == 1 && sides.contains(Side.CLIENT);
    }

    /**
     * Checks if the command accepts something of the given side.
     * Something without a side is only accepted by commands that are not restricted to one side.
     *
     * @param side the side to check.
     * @return {@code true} if the command allows the given side.
     */
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public boolean allows(Optional<Side> side) {
        return side.map(sides::contains).orElseGet(() -> sides.containsAll(Side.BOTH));
    }

    /**
     * @param args the new arguments.
     * @return arguments for the same command.
     */
    public TargetArgs withArgs(List<String> args) {
        return new TargetArgs(args, sides);
    }

    @Override
    public String toString() {
        return String.join(" ", args);
    }

}
