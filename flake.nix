{
  description = "Development shell for HeadlessMc";

  inputs.nixpkgs.url = "github:NixOS/nixpkgs/nixos-26.05";

  outputs =
    { nixpkgs, ... }:
    let
      inherit (nixpkgs) lib;

      systems = [
        "x86_64-linux"
        "aarch64-linux"
        "aarch64-darwin"
      ];

      forAllSystems = f: lib.genAttrs systems (system: f nixpkgs.legacyPackages.${system});

      jdks = pkgs: {
        "25" = pkgs.temurin-bin-25;
      };

      mkJavaShell =
        pkgs: primary:
        let
          available = jdks pkgs;
          jdk = available.${primary};
          graalvm = pkgs.graalvmPackages.graalvm-ce;
        in
        pkgs.mkShell (
          {
            # nixpkgs wraps native-image with gcc, glibc and zlib, so it needs nothing else from the shell
            packages = [ jdk ];

            JAVA_HOME = jdk.home;
            GRAALVM_HOME = graalvm.home;
          }
          // lib.optionalAttrs (lib.length (lib.attrNames available) > 1) {
            # Gradle ignores org.gradle.java.installations.paths in GRADLE_OPTS,
            # but its toolchain detection scans $SDKMAN_CANDIDATES_DIR/java/*
            SDKMAN_CANDIDATES_DIR = pkgs.linkFarm "jdks" (
              lib.mapAttrs' (version: j: lib.nameValuePair "java/${version}" j.home) available
            );
          }
        );
    in
    {
      devShells = forAllSystems (pkgs: {
        default = mkJavaShell pkgs "25";
      });

      formatter = forAllSystems (pkgs: pkgs.nixfmt-tree);
    };
}
