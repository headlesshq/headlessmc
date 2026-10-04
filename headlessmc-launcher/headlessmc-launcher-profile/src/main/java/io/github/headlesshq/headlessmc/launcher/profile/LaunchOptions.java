package io.github.headlesshq.headlessmc.launcher.profile;

import io.github.headlesshq.headlessmc.reflection.ReflectionRegistered;
import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.With;

import java.util.Locale;
import java.util.Optional;

@With
@RegisterForReflection
public record LaunchOptions(
    Optional<Resolution> resolution,
    Optional<String> quickPlayPath,
    Optional<Join> join,
    boolean demo
) implements ReflectionRegistered {
    public boolean isEmpty() {
        return resolution.isEmpty() && join.isEmpty() && !demo;
    }

    @RegisterForReflection
    public record Resolution(int width, int height) implements ReflectionRegistered {
        @Override
        public String toString() {
            return width + "x" + height;
        }

        public static Resolution parse(String string) {
            String[] split = string.toLowerCase(Locale.ENGLISH).split("x");
            if (split.length == 2) {
                return new Resolution(Integer.parseInt(split[0].trim()), Integer.parseInt(split[1].trim()));
            }

            throw new IllegalArgumentException(
                "Failed to parse resolution " + string + ", expected <width>x<height>, e.g. 800x600"
            );
        }
    }

    @RegisterForReflection
    public record Join(Type type, String target) implements ReflectionRegistered {
        @RegisterForReflection
        public enum Type implements ReflectionRegistered {
            SERVER,
            SINGLEPLAYER,
            REALMS
        }
    }

    public LaunchOptions mergeWithDefaults(LaunchOptions defaultOptions) {
        Optional<Resolution> resolution = this.resolution.or(defaultOptions::resolution);
        Optional<String> quickPlayPath = this.quickPlayPath.or(defaultOptions::quickPlayPath);
        Optional<Join> join = this.join.or(defaultOptions::join);
        boolean demo = this.demo;
        return new LaunchOptions(resolution, quickPlayPath, join, demo);
    }

}
