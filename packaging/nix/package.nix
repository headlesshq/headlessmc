# HeadlessMc built from source with Gradle (:headlessmc-application:quarkusBuild).
#
# Written so that it can be moved to nixpkgs: it only takes its dependencies as arguments.
# Variants:
#   headlessmc         Quarkus fast-jar, run by a Nix JDK
#   headlessmc-native  GraalVM native image, built like scripts/build-native.sh
#
# The Gradle dependencies are pinned in deps.json. To regenerate it after changing dependencies:
#   nix build .#headlessmc.mitmCache.updateScript && ./result
{
  lib,
  stdenv,
  gradle_9,
  jdk25_headless,
  graalvmPackages,
  makeWrapper,
  native ? false,
}:

let
  gradle = gradle_9;
  jdk = jdk25_headless;
  graalvm = graalvmPackages.graalvm-ce;
  nativeBuildArgs = lib.concatStringsSep "," [
    "--initialize-at-run-time=org.jline.nativ"
    "--initialize-at-run-time=org.jline.terminal.impl.ffm"
    "-H:+UnlockExperimentalVMOptions"
    "-H:-ParseRuntimeOptions"
    "-H:+SharedArenaSupport"
    "-H:-UnlockExperimentalVMOptions"
  ];
in
stdenv.mkDerivation (finalAttrs: {
  pname = if native then "headlessmc-native" else "headlessmc";

  # x-release-please-start-version
  version = "3.0.0-RC8";
  # x-release-please-end

  src = lib.cleanSource ../..;

  nativeBuildInputs = [
    gradle
    makeWrapper
  ]
  ++ lib.optional native graalvm;

  mitmCache = gradle.fetchDeps {
    pkg = finalAttrs.finalPackage;
    data = ./deps.json;
  };

  # fetchDeps and the setup hook run gradle for us, we only choose the task
  gradleBuildTask = ":headlessmc-application:quarkusBuild";
  gradleFlags = [
    "-Dorg.gradle.java.home=${if native then graalvm.home else jdk.home}"
  ]
  ++ lib.optionals native [
    "-Dquarkus.native.enabled=true"
    "-Dquarkus.package.jar.enabled=false"
    "-Dquarkus.native.additional-build-args=${nativeBuildArgs}"
  ];

  doCheck = false;

  installPhase =
    if native then
      ''
        runHook preInstall
        install -Dm755 headlessmc-application/build/headlessmc-application-*-runner $out/bin/headlessmc
        runHook postInstall
      ''
    else
      ''
        runHook preInstall
        mkdir -p $out/lib
        cp -r headlessmc-application/build/quarkus-app $out/lib/headlessmc
        # JDKs downloaded by HeadlessMc cannot start on NixOS (their loader is not in the store),
        # so use the Nix JDK, which is also found on the PATH when launching the game
        makeWrapper ${jdk}/bin/java $out/bin/headlessmc \
          --add-flags "-Dhmc.java.download=false" \
          --add-flags "-jar $out/lib/headlessmc/quarkus-run.jar" \
          --prefix PATH : ${lib.makeBinPath [ jdk ]}
        runHook postInstall
      '';

  # the native image launches the game with a JDK from the PATH as well
  postFixup = lib.optionalString native ''
    wrapProgram $out/bin/headlessmc \
      --add-flags "-Dhmc.java.download=false" \
      --prefix PATH : ${lib.makeBinPath [ jdk ]}
  '';

  meta = {
    description = "Headless Minecraft launcher and command line client";
    homepage = "https://github.com/headlesshq/headlessmc";
    license = lib.licenses.mit;
    mainProgram = "headlessmc";
    platforms = [
      "x86_64-linux"
      "aarch64-linux"
      "aarch64-darwin"
    ];
    sourceProvenance = with lib.sourceTypes; [
      fromSource
      binaryBytecode
    ];
  };
})
