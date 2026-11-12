# HeadlessMc built from source with Gradle (:headlessmc-application:quarkusBuild).
#
# Written so that it can be moved to nixpkgs: it only takes its dependencies as arguments.
# Variants:
#   headlessmc         Quarkus fast-jar, run by a Nix JDK
#   headlessmc-native  GraalVM native image, built like scripts/build-native.sh
#
# The Gradle dependencies are pinned in deps.json. To regenerate it after changing dependencies:
#   nix build .#headlessmc.mitmCache.updateScript && ./result
# Both variants share it: the native build resolves no extra Gradle artifacts (checked by
# generating it from .#headlessmc-native too, which gave an identical file, and by building both).
# Without a /run directory or a writable store (e.g. in a container) run it with USE_BWRAP=0.
{
  lib,
  stdenv,
  gradle_9,
  jdk25,
  zlib,
  graalvmPackages,
  makeWrapper,
  native ? false,
}:

let
  gradle = gradle_9;
  # One full JDK for building, running the launcher and launching the game. The game needs
  # libawt_xawt/libjawt, which the headless JDK lacks. The launcher must not run on a headless JDK:
  # CurrentJavaSource registers the running JVM's java.home as a candidate Java, so it could be
  # picked for the game instead of the full JDK on the PATH (costs closure size).
  jdk = jdk25;
  gameJdk = jdk25;
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
# the native build requires GraalVM 25 (only checked for native, so the other variants evaluate regardless)
assert native -> lib.versionAtLeast graalvm.version "25";
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

  buildInputs = lib.optional native zlib;

  mitmCache = gradle.fetchDeps {
    pkg = finalAttrs.finalPackage;
    data = ./deps.json;
  };

  # fetchDeps and the setup hook run gradle for us, we only choose the task
  gradleBuildTask = ":headlessmc-application:quarkusBuild";
  # Native flags compared with scripts/build-native.sh: the same -Dquarkus.native.enabled,
  # -Dquarkus.package.jar.enabled and BUILD_ARGS (nativeBuildArgs). Intentionally omitted:
  # NATIVE_MUSL (--static, --libc=musl; this is a normal glibc build against the store) and
  # SEPARATE_NATIVE_BUILD (sources-only plus a second native-image run, only a CI memory workaround).
  # quarkus.native.container-build is not set by the script either, it defaults to false.
  gradleFlags = [
    "-Dorg.gradle.java.home=${if native then graalvm else jdk}"
  ]
  ++ lib.optionals native [
    "-Dquarkus.native.enabled=true"
    "-Dquarkus.package.jar.enabled=false"
    "-Dquarkus.native.additional-build-args=${nativeBuildArgs}"
  ];

  # Quarkus finds native-image through GRAALVM_HOME (the GraalVM output is a JDK-style home)
  env = lib.optionalAttrs native { GRAALVM_HOME = "${graalvm}"; };

  doCheck = false;

  installPhase =
    if native then
      ''
        runHook preInstall
        runner=$(echo headlessmc-application/build/*-runner)
        test -f "$runner" || { echo "expected exactly one native runner, got: $runner" >&2; exit 1; }
        install -Dm755 "$runner" $out/bin/headlessmc
        runHook postInstall
      ''
    else
      ''
        runHook preInstall
        mkdir -p $out/lib
        cp -r headlessmc-application/build/quarkus-app $out/lib/headlessmc
        # JDKs downloaded by HeadlessMc cannot start on NixOS (their loader is not in the store),
        # so use the Nix JDK, which is also found on the PATH when launching the game.
        # -Dhmc.java.download=false before -jar is a JVM system property, which HeadlessMc reads
        # as a config source (checked in ConfigService / HeadlessMcMain.parseSystemProperties)
        makeWrapper ${jdk}/bin/java $out/bin/headlessmc \
          --add-flags "-Dhmc.java.download=false" \
          --add-flags "-jar $out/lib/headlessmc/quarkus-run.jar" \
          --prefix PATH : ${lib.makeBinPath [ gameJdk ]}
        runHook postInstall
      '';

  # the native image launches the game with a JDK from the PATH as well
  # Leading -D arguments are turned into system properties by HeadlessMcMain.parseSystemProperties,
  # so this is equivalent to the -D of the jar variant (`headlessmc config get hmc.java.download`
  # prints false for both variants).
  postFixup = lib.optionalString native ''
    wrapProgram $out/bin/headlessmc \
      --add-flags "-Dhmc.java.download=false" \
      --prefix PATH : ${lib.makeBinPath [ gameJdk ]}
  '';

  meta = {
    description = "Headless Minecraft launcher and command line client";
    homepage = "https://github.com/headlesshq/headlessmc";
    license = lib.licenses.mit;
    mainProgram = "headlessmc";
    platforms = [
      "x86_64-linux"
      "aarch64-linux"
    ];
    sourceProvenance = with lib.sourceTypes; [
      fromSource
      binaryBytecode
    ];
  };
})
