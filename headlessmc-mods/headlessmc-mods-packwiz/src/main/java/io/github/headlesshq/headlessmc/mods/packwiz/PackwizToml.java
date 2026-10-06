package io.github.headlesshq.headlessmc.mods.packwiz;

import io.quarkus.runtime.annotations.RegisterForReflection;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * The {@code pack.toml} file of a <a href="https://packwiz.infra.link/reference/pack-format/pack-toml/">packwiz</a>
 * modpack. E.g.:
 * <pre>
 * {@code
 * name = "My Modpack"
 * author = "headlesshq"
 * version = "1.0.0"
 * pack-format = "packwiz:1.1.0"
 *
 * [index]
 * file = "index.toml"
 * hash-format = "sha256"
 * hash = "..."
 *
 * [versions]
 * minecraft = "1.21.1"
 * fabric = "0.16.14"
 * }
 * </pre>
 *
 * <p>We only need the {@code [versions]} table, the other fields are commented out
 * and ignored when parsing, but kept here for reference.
 *
 * @param versions the versions of the components of this modpack.
 *                 Always contains {@code "minecraft"}, and potentially
 *                 loaders such as {@code "fabric"}, {@code "forge"},
 *                 {@code "neoforge"}, {@code "quilt"} or {@code "liteloader"}.
 *                 The existence of a component implies that it should be installed.
 */
@RegisterForReflection
record PackwizToml(
    // the name of the modpack.
    // String name,
    // the author(s) of the modpack.
    // @Nullable String author,
    // the version of the modpack itself.
    // @Nullable String version,
    // a short description of the modpack.
    // @Nullable String description,
    // the format version of this file, e.g. "packwiz:1.1.0".
    // @JsonProperty("pack-format") @Nullable String packFormat,
    // information about the index file.
    // @Nullable Index index,
    @Nullable Map<String, String> versions
) {
    public static final String MINECRAFT = "minecraft";

    // /**
    //  * @param file       path to the index file, relative to the pack.toml.
    //  * @param hashFormat the hash algorithm, e.g. sha256, sha512, sha1, md5 or murmur2.
    //  * @param hash       the hash of the index file.
    //  */
    // @RegisterForReflection
    // public record Index(
    //     String file,
    //     @JsonProperty("hash-format") String hashFormat,
    //     String hash
    // ) {}

}
