#!/bin/sh
# Installs the HeadlessMc launcher on Linux and macOS:
#
#   curl -fsSL https://headlesshq.github.io/headlessmc/install.sh | sh
#
# Options, passed as environment variables, e.g. curl ... | HEADLESSMC_VERSION=3.0.0 sh
#   HEADLESSMC_VERSION      release tag to install, e.g. 3.0.0 (default: latest release)
#   HEADLESSMC_INSTALL_DIR  directory to install into (default: ~/.local/bin)
#
# On Windows use install.ps1 instead.
set -eu

REPO="headlesshq/headlessmc"
NAME="headlessmc"
DOCS="https://headlesshq.github.io/headlessmc/getting-started/"
# Newest glibc symbol version the Linux executables link against.
# Update when the native build moves to a newer build environment,
# check with: objdump -T headlessmc-launcher-linux-x64 | grep -o 'GLIBC_[0-9.]*' | sort -uV | tail -1
MIN_GLIBC="2.34"

main() {
  detect_platform
  asset="headlessmc-launcher-$os-$arch"
  if [ -n "${HEADLESSMC_VERSION:-}" ]; then
    url="https://github.com/$REPO/releases/download/$HEADLESSMC_VERSION/$asset"
  else
    url="https://github.com/$REPO/releases/latest/download/$asset"
  fi
  dir="${HEADLESSMC_INSTALL_DIR:-$HOME/.local/bin}"

  mkdir -p "$dir"
  # download next to the target and rename, so a running or half downloaded executable is never overwritten
  tmp="$dir/.$NAME.download.$$"
  trap 'rm -f "$tmp"' EXIT
  trap 'rm -f "$tmp"; exit 130' INT TERM

  say "Downloading $url"
  download "$url" "$tmp"
  chmod +x "$tmp"
  mv -f "$tmp" "$dir/$NAME"
  say "Installed $NAME to $dir/$NAME"

  case ":$PATH:" in
    *":$dir:"*)
      say "Run '$NAME' to get started, see $DOCS" ;;
    *)
      say ""
      say "$dir is not on your PATH yet. Add this line to your shell profile (e.g. ~/.bashrc or ~/.zshrc):"
      say "  export PATH=\"$dir:\$PATH\""
      say "Then run '$NAME' to get started, see $DOCS" ;;
  esac
}

detect_platform() {
  case "$(uname -s)" in
    Linux)  os=linux ;;
    Darwin) os=macos ;;
    MINGW*|MSYS*|CYGWIN*)
      err "on Windows run this in PowerShell instead: irm https://headlesshq.github.io/headlessmc/install.ps1 | iex" ;;
    *) unsupported "operating system $(uname -s)" ;;
  esac

  case "$(uname -m)" in
    x86_64|amd64)  arch=x64 ;;
    arm64|aarch64) arch=arm64 ;;
    *) unsupported "architecture $(uname -m)" ;;
  esac

  if [ "$os" = macos ] && [ "$arch" = x64 ] \
      && [ "$(sysctl -n sysctl.proc_translated 2>/dev/null || true)" = 1 ]; then
    arch=arm64 # x64 shell running under Rosetta on Apple Silicon, prefer the native executable
  fi

  if [ "$os" = linux ]; then
    if [ "$(uname -o 2>/dev/null || true)" = Android ]; then
      unsupported "Android/Termux"
    fi
    if ldd --version 2>&1 | grep -qi musl || ls /lib/ld-musl-* >/dev/null 2>&1; then
      unsupported "musl libc (e.g. Alpine Linux)"
    fi
    glibc="$(getconf GNU_LIBC_VERSION 2>/dev/null | awk '{ print $2 }' || true)"
    if [ -n "$glibc" ] && version_lt "$glibc" "$MIN_GLIBC"; then
      unsupported "glibc $glibc, the executable needs glibc $MIN_GLIBC or newer"
    fi
  fi
}

# version_lt a b: true if version a is lower than version b
version_lt() {
  awk -v a="$1" -v b="$2" 'BEGIN {
    na = split(a, x, "."); nb = split(b, y, ".")
    n = na > nb ? na : nb
    for (i = 1; i <= n; i++) {
      if (x[i] + 0 < y[i] + 0) exit 0
      if (x[i] + 0 > y[i] + 0) exit 1
    }
    exit 1
  }'
}

download() {
  if command -v curl >/dev/null 2>&1; then
    curl -fL --proto '=https' --tlsv1.2 --progress-bar -o "$2" "$1" || err "download of $1 failed"
  elif command -v wget >/dev/null 2>&1; then
    wget -q -O "$2" "$1" || err "download of $1 failed"
  else
    err "curl or wget is required to download HeadlessMc"
  fi
}

unsupported() {
  err "there is no HeadlessMc executable for $1.
Instead, you can run the headlessmc.jar with Java 25 or newer, see $DOCS"
}

say() {
  printf '%s\n' "$1"
}

err() {
  printf 'error: %s\n' "$1" >&2
  exit 1
}

main "$@"
