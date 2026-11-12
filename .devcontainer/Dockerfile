# GraalVM Community 25.0.x ended at 25.0.2, since then GraalVM only ships "GraalVM Community 25 Innovation N" releases (#97).
# These are the best available GraalVM for JDK 25, so this follows them (tag 25i5 = 25.5.5.0.3, JDK 25.0.5.0.3).
# The floating `25` tag still points to the old 25.0.2, so the tag is pinned explicitly.
# Keep in sync with graalvm-ce from nixos-unstable in flake.nix.
# Whether Renovate proposes newer 25iN tags for this image is unverified, check for new Innovation releases manually.
FROM ghcr.io/graalvm/native-image-community:25i5-25.0.5.0.3@sha256:2800b5bd11cbb9bf11c92591d05ea37d9a735522bdc0e86827302d998843ad79 AS graal

FROM mcr.microsoft.com/devcontainers/base:bookworm@sha256:99f5250dd2d5f8b11046d5662dd94c1e3c1dae56b560c7caad5ceb7edf349046

# native-image needs a C toolchain and the zlib headers to link executables
RUN apt-get update \
    && apt-get install -y --no-install-recommends build-essential zlib1g-dev \
    && rm -rf /var/lib/apt/lists/*

COPY --from=graal /usr/lib64/graalvm/graalvm-community-java25 /opt/graalvm-25
