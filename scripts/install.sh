#!/bin/sh
# Installs the HeadlessMc launcher on Linux and macOS:
#
#   curl -fsSL https://headlesshq.github.io/headlessmc/install.sh | sh
#
# Options, passed as environment variables, e.g. curl ... | HEADLESSMC_VERSION=3.0.0 sh
#   HEADLESSMC_VERSION      release tag to install, e.g. 3.0.0 (default: latest release)
#   HEADLESSMC_INSTALL_DIR  directory to install into (default: ~/.local/bin)
#   HEADLESSMC_NO_COMPLETIONS  if set, does not install the bash, zsh and fish completions
#   HEADLESSMC_MUSL         if set, installs the fully static musl executable on Linux x64,
#                           which is used automatically on musl systems, NixOS and with an older glibc
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
  variant=""
  detect_platform
  asset="headlessmc-launcher-$os-$arch$variant"
  if [ -n "${HEADLESSMC_VERSION:-}" ]; then
    url="https://github.com/$REPO/releases/download/$HEADLESSMC_VERSION/$asset"
  else
    url="https://github.com/$REPO/releases/latest/download/$asset"
  fi
  dir="${HEADLESSMC_INSTALL_DIR:-$HOME/.local/bin}"

  mkdir -p "$dir"
  # absolute and without a trailing slash, to compare it with the PATH entries
  dir="$(cd "$dir" && pwd)"
  # download next to the target and rename, so a running or half downloaded executable is never overwritten
  tmp="$dir/.$NAME.download.$$"
  trap 'rm -f "$tmp"' EXIT
  trap 'rm -f "$tmp"; exit 130' INT TERM

  say "Downloading $url"
  download "$url" "$tmp"
  chmod +x "$tmp"
  mv -f "$tmp" "$dir/$NAME"
  say "Installed $NAME to $dir/$NAME"

  if [ -z "${HEADLESSMC_NO_COMPLETIONS:-}" ]; then
    install_completions "$dir/$NAME"
  fi

  case ":$PATH:" in
    *":$dir:"*)
      found="$(command -v "$NAME" 2>/dev/null || true)"
      if [ -n "$found" ] && [ "$found" != "$dir/$NAME" ]; then
        say ""
        say "warning: $found comes before $dir on your PATH and is run instead,"
        say "e.g. because a package manager installed it. Uninstall it, or run $dir/$NAME."
      fi
      say "Run '$NAME' to get started, see $DOCS" ;;
    *)
      say ""
      say "$dir is not on your PATH yet. Add this line to your shell profile (e.g. ~/.bashrc or ~/.zshrc):"
      say "  export PATH=\"$dir:\$PATH\""
      say "Then run '$NAME' to get started, see $DOCS" ;;
  esac
}

# Writes the shell completions of the installed executable into directories that bash, zsh and fish
# load them from automatically, like the Linux packages do, so no shell profile has to be edited.
# The scripts call 'headlessmc __complete' on the PATH, they keep working when HeadlessMc updates itself.
install_completions() {
  # releases before the shell completions do not have the completion command
  "$1" completion bash >/dev/null 2>&1 || return 0

  shells=""
  if command -v bash >/dev/null 2>&1; then
    # loaded by the bash-completion package when you first press tab
    bash_dir="${BASH_COMPLETION_USER_DIR:-${XDG_DATA_HOME:-$HOME/.local/share}/bash-completion}"
    write_completion "$1" bash "${bash_dir%%:*}/completions/$NAME" && shells="$shells bash"
  fi
  if command -v fish >/dev/null 2>&1; then
    # fish 3.3 (Ubuntu 22.04) has no user vendor_completions.d yet, this works in every version
    write_completion "$1" fish "${XDG_CONFIG_HOME:-$HOME/.config}/fish/completions/$NAME.fish" && shells="$shells fish"
  fi
  zsh_hint=""
  if command -v zsh >/dev/null 2>&1; then
    zsh_dir="$(zsh_site_functions)"
    if [ -n "$zsh_dir" ] && write_completion "$1" zsh "$zsh_dir/_$NAME"; then
      shells="$shells zsh"
    else
      zsh_hint=1
    fi
  fi

  if [ -n "$shells" ]; then
    say "Installed the shell completions for${shells}, they work in new shells"
  fi
  if [ -n "$zsh_hint" ]; then
    say "For completions in zsh, add this line to your ~/.zshrc after compinit:"
    say "  source <($NAME completion zsh)"
  fi
}

# write_completion executable shell file
write_completion() {
  "$1" completion "$2" -o "$3" >/dev/null 2>&1
}

# Prints the first site-functions directory on the default fpath of zsh that can be written to,
# e.g. /usr/local/share/zsh/site-functions as root or Homebrew's on macOS. Users have no such directory,
# without one they would have to add a directory to their fpath in their ~/.zshrc themselves.
zsh_site_functions() {
  # -f: only the fpath zsh has been built with, without the user's startup files
  zsh -fc 'print -rl -- $fpath' 2>/dev/null | while IFS= read -r d; do
    case "$d" in
      */site-functions) ;;
      *) continue ;;
    esac
    if [ -d "$d" ]; then
      [ -w "$d" ] || continue
    else
      mkdir -p "$d" 2>/dev/null || continue
    fi
    printf '%s\n' "$d"
    break
  done
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
    static_reason=""
    if [ -n "${HEADLESSMC_MUSL:-}" ]; then
      static_reason="HEADLESSMC_MUSL"
    elif ldd --version 2>&1 | grep -qi musl || ls /lib/ld-musl-* >/dev/null 2>&1; then
      static_reason="musl libc (e.g. Alpine Linux)"
    elif [ ! -e "$(glibc_loader)" ]; then
      static_reason="a system without $(glibc_loader) (e.g. NixOS)"
    else
      glibc="$(getconf GNU_LIBC_VERSION 2>/dev/null | awk '{ print $2 }' || true)"
      if [ -n "$glibc" ] && version_lt "$glibc" "$MIN_GLIBC"; then
        static_reason="glibc $glibc, the glibc executable needs glibc $MIN_GLIBC or newer"
      fi
    fi
    if [ -n "$static_reason" ]; then
      [ "$arch" = x64 ] || unsupported "$static_reason on $arch"
      say "Using the static musl executable, detected $static_reason"
      variant="-musl"
    fi
  fi
}

# The dynamic loader the glibc executables are linked against.
glibc_loader() {
  case "$arch" in
    x64) echo /lib64/ld-linux-x86-64.so.2 ;;
    arm64) echo /lib/ld-linux-aarch64.so.1 ;;
  esac
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
    curl -fL --proto '=https' --tlsv1.2 --retry 3 --retry-delay 2 --progress-bar -o "$2" "$1" || err "download of $1 failed"
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
