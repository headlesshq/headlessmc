package io.github.headlesshq.headlessmc.version.id;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.Locale;

@ApplicationScoped
public class VersionIDParser {
    public VersionID parse(String string) {
        String[] split = string.split(":");
        if (split.length == 1) {
            // 1.21.5
            return VersionID.builder().version(split[0]).build();
        } else if (split.length == 2) {
            // server:1.21.5
            if (string.toLowerCase(Locale.ENGLISH).startsWith("server:")) {
                return VersionID.builder()
                        .server(true)
                        .version(split[1])
                        .build();
            } else {
                // fabric:1.21.5
                return VersionID.builder()
                        .platform(split[0])
                        .version(split[1])
                        .build();
            }
        } else if (split.length == 3) {
            if (string.toLowerCase(Locale.ENGLISH).startsWith("server:")) {
                // server:fabric:1.21.5
                return VersionID.builder()
                        .server(true)
                        .platform(split[1])
                        .version(split[2])
                        .build();
            } else {
                // fabric:1.21.5:0.16.14
                return VersionID.builder()
                        .platform(split[0])
                        .version(split[1])
                        .build(split[2])
                        .build();
            }
        } else if (split.length == 4) {
            // server:fabric:1.21.5:0.16.14
            if (string.toLowerCase(Locale.ENGLISH).startsWith("server:")) {
                return VersionID.builder()
                        .platform(split[1])
                        .version(split[2])
                        .build(split[3])
                        .server(true)
                        .build();
            }
        }

        throw new VersionIDParseException(string);
    }

}
