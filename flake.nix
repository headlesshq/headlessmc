{
  description = "HeadlessMc, a headless Minecraft launcher";

  inputs.nixpkgs.url = "github:NixOS/nixpkgs/nixos-26.05";

  outputs =
    { self, nixpkgs, ... }:
    let
      inherit (nixpkgs) lib;

      systems = [
        "x86_64-linux"
        "aarch64-linux"
        "aarch64-darwin"
      ];

      forAllSystems = f: lib.genAttrs systems (system: f nixpkgs.legacyPackages.${system});

      # the package is only supported on Linux (see meta.platforms), dev shells work everywhere
      forLinux =
        f:
        lib.genAttrs (lib.filter (lib.hasSuffix "-linux") systems) (system: f nixpkgs.legacyPackages.${system});

      jdks = pkgs: {
        "25" = pkgs.temurin-bin-25;
      };

      mkJavaShell =
        pkgs: primary: native:
        let
          available = jdks pkgs;
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
        );
    in
    {
      devShells = forAllSystems (pkgs: {
        default = mkJavaShell pkgs "25" false;
        native = mkJavaShell pkgs "25" true;
      });

      packages = forLinux (pkgs: rec {
        headlessmc = pkgs.callPackage ./packaging/nix/package.nix { };
        headlessmc-native = pkgs.callPackage ./packaging/nix/package.nix { native = true; };
        default = headlessmc;
      });

      apps = forLinux (pkgs: {
        default = {
          type = "app";
          program = lib.getExe self.packages.${pkgs.stdenv.hostPlatform.system}.headlessmc;
          meta.description = "Run HeadlessMc";
        };
      });

      overlays.default = final: _prev: {
        headlessmc = final.callPackage ./packaging/nix/package.nix { };
        headlessmc-native = final.callPackage ./packaging/nix/package.nix { native = true; };
      };

      formatter = forAllSystems (pkgs: pkgs.nixfmt-tree);
    };
}
