FROM ghcr.io/graalvm/native-image-community:25.0.2 AS graal

FROM mcr.microsoft.com/devcontainers/base:bookworm

# native-image needs a C toolchain and the zlib headers to link executables
RUN apt-get update \
    && apt-get install -y --no-install-recommends build-essential zlib1g-dev \
    && rm -rf /var/lib/apt/lists/*

COPY --from=graal /usr/lib64/graalvm/graalvm-community-java25 /opt/graalvm-25
