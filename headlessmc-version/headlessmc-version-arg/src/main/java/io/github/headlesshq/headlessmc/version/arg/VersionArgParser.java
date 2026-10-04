package io.github.headlesshq.headlessmc.version.arg;

import lombok.experimental.UtilityClass;
import org.jspecify.annotations.Nullable;

import java.util.List;

@UtilityClass
final class VersionArgParser {
    static VersionArg parse(List<String> parameters) {
        String[] args = parameters.toArray(String[]::new);
        if (args.length == 1) {
            // 1.21.5
            return VersionArg.builder().version(args[0]).build();
        } else if (args.length == 2) {
            Side side = getSide(args);
            // server/1.21.5
            if (side != null) {
                return VersionArg.builder()
                    .onSide(side)
                    .version(args[1])
                    .build();
            }

            // fabric/1.21.5
            return VersionArg.builder()
                .platform(args[0])
                .version(args[1])
                .build();
        } else if (args.length == 3) {
            Side side = getSide(args);
            if (side != null) {
                // server/fabric/1.21.5
                return VersionArg.builder()
                    .onSide(side)
                    .platform(args[1])
                    .version(args[2])
                    .build();
            }

            // fabric/1.21.5/0.16.14
            return VersionArg.builder()
                .platform(args[0])
                .version(args[1])
                .withBuild(args[2])
                .build();
        } else if (args.length == 4) {
            // server/fabric/1.21.5/0.16.14
            Side side = getSide(args);
            if (side != null) {
                return VersionArg.builder()
                    .platform(args[1])
                    .version(args[2])
                    .withBuild(args[3])
                    .onSide(side)
                    .build();
            }
        }

        throw new IllegalArgumentException(parameters.toString());
    }

    private static @Nullable Side getSide(String[] split) {
        for (Side side : Side.values()) {
            if (side.name().equalsIgnoreCase(split[0])) {
                return side;
            }
        }

        return null;
    }

}
