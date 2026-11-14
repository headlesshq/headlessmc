# Entry point for Nix without flakes (nix-build, nix-env, <nixpkgs>-style imports).
# It builds the same packaging/nix/package.nix as the flake, no experimental features needed:
#   nix-build                        build headlessmc into ./result
#   nix-build -A headlessmc-native   build the native executable
#   nix-env -f . -i                  install headlessmc into your profile
# nixpkgs defaults to the revision pinned in flake.lock, pass your own with
#   nix-build --arg pkgs 'import <nixpkgs> { }'
let
  lock = (builtins.fromJSON (builtins.readFile ./flake.lock)).nodes.nixpkgs.locked;
  nixpkgs = fetchTarball {
    url = "https://github.com/${lock.owner}/${lock.repo}/archive/${lock.rev}.tar.gz";
    sha256 = lock.narHash;
  };
in
{
  pkgs ? import nixpkgs { },
}:
let
  headlessmc = pkgs.callPackage ./packaging/nix/package.nix { };
  headlessmc-native = pkgs.callPackage ./packaging/nix/package.nix { native = true; };
in
# The result is the headlessmc derivation itself, so that a plain nix-build or nix-env -i only builds
# the default package, with both variants as attributes for -A.
headlessmc
// {
  inherit headlessmc headlessmc-native;
}
