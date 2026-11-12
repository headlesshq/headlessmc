# Deliberately pinned to GraalVM Community 25.0.2, the last release of the 25.0.x line (#97).
# The newer "25iN" Innovation tags are not followed: Renovate cannot track them with docker versioning,
# nixpkgs (flake.nix) only ships 25.0.2, and the devcontainer should match the flake.
# Revisit when GraalVM Community 26 is released and nixpkgs packages it.
FROM ghcr.io/graalvm/native-image-community:25.0.2@sha256:0d936f32bb8acb5bc60c41b33e05f064d7a6aaf36b726538296c54949bd4a3c0 AS graal

FROM mcr.microsoft.com/devcontainers/base:bookworm@sha256:99f5250dd2d5f8b11046d5662dd94c1e3c1dae56b560c7caad5ceb7edf349046

# native-image needs a C toolchain and the zlib headers to link executables
RUN apt-get update \
    && apt-get install -y --no-install-recommends build-essential zlib1g-dev \
    && rm -rf /var/lib/apt/lists/*

COPY --from=graal /usr/lib64/graalvm/graalvm-community-java25 /opt/graalvm-25
