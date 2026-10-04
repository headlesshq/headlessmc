package io.github.headlesshq.headlessmc.util.maven;

import org.jspecify.annotations.Nullable;

import java.nio.file.Path;

public record Artifact(String group, String name, String version, @Nullable String classifier) {
    public Artifact(String group, String name, String version) {
        this(group, name, version, null);
    }

    public String path() {
        return String.join("/", group.split("\\.")) + "/" + name + "/" + version;
    }

    public String jar() {
        return getFileName("jar");
    }

    public String getFileName(String extension) {
        StringBuilder res = new StringBuilder(name).append("-").append(version);
        if (classifier != null) {
            res.append("-").append(classifier);
        }

        return res.append(".").append(extension).toString();
    }

    public Path file(Path base, String fileExtension) {
        Path result = base;
        for (String domain : group.split("\\.")) {
            result = result.resolve(domain);
        }

        return result.resolve(name).resolve(version).resolve(getFileName(fileExtension));
    }

    public Path jar(Path base) {
        return file(base, "jar");
    }

    public static Artifact of(String coordinates) throws IllegalArgumentException {
        String[] split = coordinates.split(":");
        if (split.length == 3) {
            return new Artifact(split[0], split[1], split[2]);
        } else if (split.length == 4) {
            return new Artifact(split[0], split[1], split[2], split[3]);
        }

        throw new IllegalArgumentException("Failed to parse " + coordinates + "as group:name:version (:classifier)");
    }

}
