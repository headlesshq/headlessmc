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
        lib.genAttrs (lib.filter (lib.hasSuffix "-linux") systems) (
          system: f nixpkgs.legacyPackages.${system}
        );

    in
    {
      devShells = forAllSystems (pkgs: {
        default = import ./shell.nix { inherit pkgs; };
        native = import ./shell.nix {
          inherit pkgs;
          native = true;
        };
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

      # `nix flake check` builds both variants, including their install checks
      checks = forLinux (pkgs: {
        inherit (self.packages.${pkgs.stdenv.hostPlatform.system}) headlessmc headlessmc-native;
      });

      overlays.default = final: _prev: {
        headlessmc = final.callPackage ./packaging/nix/package.nix { };
        headlessmc-native = final.callPackage ./packaging/nix/package.nix { native = true; };
      };

      formatter = forAllSystems (pkgs: pkgs.nixfmt-tree);
    };
}
