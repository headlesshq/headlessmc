# Development shell, for `nix-shell` (add `--arg native true` for GraalVM) and the devShells of the flake.
let
  lock = (builtins.fromJSON (builtins.readFile ./flake.lock)).nodes.nixpkgs.locked;
  nixpkgs = fetchTarball {
    url = "https://github.com/${lock.owner}/${lock.repo}/archive/${lock.rev}.tar.gz";
    sha256 = lock.narHash;
  };
in
{
  pkgs ? import nixpkgs { },
  primary ? "25",
  native ? false,
}:
let
  inherit (pkgs) lib;
  available = {
    "25" = pkgs.temurin-bin-25;
  };
  jdk = available.${primary};
  # only the native shell pulls in GraalVM, it drags in AWT/X11 libraries (~2 GB)
  graalvm = pkgs.graalvmPackages.graalvm-ce;
in
pkgs.mkShell (
  {
    # nixpkgs wraps native-image with gcc, glibc and zlib, so it needs nothing else from the shell
    packages = [ jdk ] ++ lib.optional native graalvm;

    JAVA_HOME = jdk.home;
  }
  // lib.optionalAttrs native {
    GRAALVM_HOME = graalvm.home;
  }
  // lib.optionalAttrs (lib.length (lib.attrNames available) > 1) {
    # Gradle ignores org.gradle.java.installations.paths in GRADLE_OPTS,
    # but its toolchain detection scans $SDKMAN_CANDIDATES_DIR/java/*
    SDKMAN_CANDIDATES_DIR = pkgs.linkFarm "jdks" (
      lib.mapAttrs' (version: j: lib.nameValuePair "java/${version}" j.home) available
    );
  }
)
