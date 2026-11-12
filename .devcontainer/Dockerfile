# Deliberately pinned to GraalVM Community 25.0.2, the last release of the 25.0.x line (#97).
# GraalVM now ships only "GraalVM Community 25 Innovation N" releases (e.g. tags 25i5), which are not followed.
# The flake (flake.nix) tracks the nixos-26.05 stable channel, whose graalvm-ce is 25.0.2 (nixpkgs master
# already has 25.5.5.0.3), and the devcontainer should match it.
# Revisit when the flake moves to a nixpkgs channel with a newer graalvm-ce, or when a GraalVM Community
# release line newer than 25.0.x is packaged by the channel the flake tracks.
FROM ghcr.io/graalvm/native-image-community:25.0.2@sha256:0d936f32bb8acb5bc60c41b33e05f064d7a6aaf36b726538296c54949bd4a3c0 AS graal

FROM mcr.microsoft.com/devcontainers/base:bookworm@sha256:99f5250dd2d5f8b11046d5662dd94c1e3c1dae56b560c7caad5ceb7edf349046

# native-image needs a C toolchain and the zlib headers to link executables
RUN apt-get update \
    && apt-get install -y --no-install-recommends build-essential zlib1g-dev \
    && rm -rf /var/lib/apt/lists/*

COPY --from=graal /usr/lib64/graalvm/graalvm-community-java25 /opt/graalvm-25
