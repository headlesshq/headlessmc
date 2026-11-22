FROM ghcr.io/graalvm/native-image-community:25.0.2@sha256:0d936f32bb8acb5bc60c41b33e05f064d7a6aaf36b726538296c54949bd4a3c0 AS graal

FROM mcr.microsoft.com/devcontainers/base:bookworm@sha256:06701e75787b0b948709e803f95b42cabf37607b1c308c57471d6f0252149d87

# native-image needs a C toolchain and the zlib headers to link executables
RUN apt-get update \
    && apt-get install -y --no-install-recommends build-essential zlib1g-dev \
    && rm -rf /var/lib/apt/lists/*

COPY --from=graal /usr/lib64/graalvm/graalvm-community-java25 /opt/graalvm-25
