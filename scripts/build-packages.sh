#!/bin/sh
# Builds the deb, rpm and archlinux packages from a native executable with nfpm.
#
# Usage: scripts/build-packages.sh <version> <x64|arm64> <native executable> [output directory]
# e.g.   scripts/build-packages.sh 3.0.0-RC8 x64 headlessmc-launcher-linux-x64 build/packages
#
# The package files are named after the release tag, not after the version inside the package (3.0.0~RC8),
# because GitHub replaces the ~ in release asset names.
set -eu

if [ "$#" -lt 3 ]; then
  echo "Usage: $0 <version> <x64|arm64> <native executable> [output directory]" >&2
  exit 1
fi

version="$1"
arch="$2"
executable="$(realpath "$3")"
out="$(realpath -m "${4:-build/packages}")"

case "$arch" in
  x64)   nfpm_arch=amd64; rpm_arch=x86_64;  pacman_arch=x86_64 ;;
  arm64) nfpm_arch=arm64; rpm_arch=aarch64; pacman_arch=aarch64 ;;
  *) echo "Unsupported architecture: $arch" >&2; exit 1 ;;
esac

cd "$(dirname "$0")/.."

# packaging/nfpm.yaml expects the executable here
mkdir -p build/packages/staging "$out"
cp "$executable" build/packages/staging/headlessmc
chmod 755 build/packages/staging/headlessmc

NFPM_VERSION="$version"
NFPM_ARCH="$nfpm_arch"
export NFPM_VERSION NFPM_ARCH

nfpm package --config packaging/nfpm.yaml --packager deb \
  --target "$out/headlessmc_${version}_${nfpm_arch}.deb"
nfpm package --config packaging/nfpm.yaml --packager rpm \
  --target "$out/headlessmc-${version}.${rpm_arch}.rpm"
nfpm package --config packaging/nfpm.yaml --packager archlinux \
  --target "$out/headlessmc-${version}-${pacman_arch}.pkg.tar.zst"

rm -r build/packages/staging
ls -l "$out"
