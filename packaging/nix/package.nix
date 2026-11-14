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
  installShellFiles,
  removeReferencesTo,
  native ? false,
  # Additional JDKs for Minecraft versions that do not run on Java 25, added to the PATH of HeadlessMc:
  #   headlessmc.override { extraJdks = [ pkgs.jdk8 pkgs.jdk17 pkgs.jdk21 ]; }
  extraJdks ? [ ],
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
  # Native flags compared with scripts/build-native.sh: the same -Dquarkus.native.enabled,
  # -Dquarkus.package.jar.enabled and BUILD_ARGS (nativeBuildArgs). Intentionally omitted:
  # NATIVE_MUSL (--static, --libc=musl; this is a normal glibc build against the store) and
  # SEPARATE_NATIVE_BUILD (sources-only plus a second native-image run, only a CI memory workaround).
  # quarkus.native.container-build is not set by the script either, it defaults to false.
  nativeGradleFlags = [
    "-Dquarkus.native.enabled=true"
    "-Dquarkus.package.jar.enabled=false"
    "-Dquarkus.native.additional-build-args=${nativeBuildArgs}"
  ];
in
# the native build requires GraalVM 25 (only checked for native, so the other variants evaluate regardless)
assert native -> lib.versionAtLeast graalvm.version "25";
stdenv.mkDerivation (finalAttrs: {
  pname = if native then "headlessmc-native" else "headlessmc";

  # x-release-please-start-version
  version = "3.0.0-RC9";
  # x-release-please-end

  # Without flakes the source is the working directory, which may contain build outputs and run directories.
  # These are all untracked, so this filter does not change the source of the flake.
  src = lib.cleanSourceWith {
    src = lib.cleanSource ../..;
    filter =
      path: type:
      !(
        type == "directory"
        && lib.elem (baseNameOf path) [
          "build"
          ".gradle"
          ".idea"
          ".vscode"
          ".claude"
          "HeadlessMC"
          "HeadlessMc"
        ]
      );
  };

  nativeBuildInputs = [
    gradle
    makeWrapper
    installShellFiles
  ]
  ++ lib.optionals native [
    graalvm
    removeReferencesTo
  ];

  buildInputs = lib.optional native zlib;

  mitmCache = gradle.fetchDeps {
    pkg = finalAttrs.finalPackage;
    data = ./deps.json;
  };

  # Plugins re-augment HeadlessMc from the mutable jar (see headlessmc-plugins and docs/plugins.md).
  # Release builds embed a lock to download it from the GitHub release, which the sandbox cannot do,
  # so both variants build it here and point hmc.plugins.mutable-jar at it. PluginBuilder only copies
  # it into its cache before re-augmenting, so it can stay in the read-only store.
  # -PmutableJar needs no extra entries in deps.json (both variants build offline with it): every
  # Quarkus build resolves the deployment dependencies, the mutable jar only keeps them in lib/deployment.
  gradleFlags = [ "-Dorg.gradle.java.home=${if native then graalvm else jdk}" ];

  # `gradle` is the function of the setup hook, which adds gradleFlags and the offline/MITM cache flags.
  # The Quarkus Gradle plugin assembles build/quarkus-app as a fast-jar without lib/deployment,
  # the complete mutable jar is in build/quarkus-build/gen/quarkus-app (like mutableJarZip uses it).
  # The native build runs second, it would replace the generated quarkus-app, so that is moved first.
  buildPhase = ''
    runHook preBuild
    gradle ''${enableParallelBuilding:+--parallel} :headlessmc-application:quarkusBuild -PmutableJar
    mv headlessmc-application/build/quarkus-build/gen/quarkus-app "$NIX_BUILD_TOP/mutable-jar"
  ''
  + lib.optionalString native ''
    gradle ''${enableParallelBuilding:+--parallel} :headlessmc-application:quarkusBuild ${lib.escapeShellArgs nativeGradleFlags}
  ''
  + ''
    runHook postBuild
  '';

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
        mkdir -p $out/lib
        cp -r "$NIX_BUILD_TOP/mutable-jar" $out/lib/headlessmc-mutable
        runHook postInstall
      ''
    else
      ''
        runHook preInstall
        mkdir -p $out/lib
        # the mutable jar runs like the fast-jar, it only adds lib/deployment for the re-augmentation
        cp -r "$NIX_BUILD_TOP/mutable-jar" $out/lib/headlessmc
        # JDKs downloaded by HeadlessMc cannot start on NixOS (their loader is not in the store),
        # so use the Nix JDK, which is also found on the PATH when launching the game.
        # -Dhmc.java.download=false before -jar is a JVM system property, which HeadlessMc reads
        # as a config source (checked in ConfigService / HeadlessMcMain.parseSystemProperties)
        # Updates come through Nix (the store is read-only), so the self-updater is disabled.
        makeWrapper ${jdk}/bin/java $out/bin/headlessmc \
          --add-flags "-Dhmc.java.download=false" \
          --add-flags "-Dhmc.updater.enabled=false" \
          --add-flags "-Dhmc.plugins.mutable-jar=$out/lib/headlessmc" \
          --add-flags "-jar $out/lib/headlessmc/quarkus-run.jar" \
          --prefix PATH : ${lib.makeBinPath ([ gameJdk ] ++ extraJdks)}
        runHook postInstall
      '';

  # the same completion scripts as in the deb, rpm and archlinux packages (packaging/nfpm.yaml)
  postInstall = ''
    installShellCompletion --cmd headlessmc \
      headlessmc-commands/src/main/resources/io/github/headlesshq/headlessmc/commands/completions/headlessmc.{bash,fish,zsh}
  '';

  # runs the wrapped launcher once, for both variants (Gradle tests are not run, see doCheck)
  doInstallCheck = true;
  installCheckPhase = ''
    runHook preInstallCheck
    HOME=$(mktemp -d) $out/bin/headlessmc --version | grep -F "${finalAttrs.version}"
    runHook postInstallCheck
  '';

  # the native image launches the game with a JDK from the PATH as well, and runs HeadlessMc with
  # plugins on it (the build re-augmented from lib/headlessmc-mutable).
  # Leading -D arguments are turned into system properties by HeadlessMcMain.parseSystemProperties,
  # so this is equivalent to the -D of the jar variant (`headlessmc config get hmc.java.download`
  # prints false for both variants). The updater is disabled as well: it would suggest overwriting
  # the binary in the read-only store, updates come through Nix instead.
  # The image heap of the native runner contains a snapshot of the build environment (sandbox PATH,
  # NIX_CFLAGS_COMPILE, NIX_LDFLAGS, JAVA_HOME, ...; config property names and values from
  # System.getenv() during the build), so it references graalvm, gradle, the C toolchain, gtk+3 and
  # more (a ~3 GiB closure instead of ~1 GiB). None of these strings are used at run time: HeadlessMc
  # reads PATH/JAVA_HOME with System.getenv() when looking for Javas. The runner only needs the
  # dynamic loader and its RUNPATH (glibc), so every other store reference is overwritten
  # (remove-references-to only replaces the hash, the image keeps its layout). This runs after the
  # fixup phase shrank the RUNPATH and before wrapProgram renames the runner.
  postFixup = lib.optionalString native ''
    runner=$out/bin/headlessmc
    keep=$({ patchelf --print-interpreter "$runner"; patchelf --print-rpath "$runner" | tr : '\n'; } \
      | grep -oE "^$NIX_STORE/[a-z0-9]{32}-" | sort -u)
    refs=()
    for ref in $(grep -aoE "$NIX_STORE/[a-z0-9]{32}-" "$runner" | sort -u); do
      if [[ "$ref" != "$NIX_STORE/$(basename "$out" | cut -c1-33)" ]] && ! grep -qxF "$ref" <<< "$keep"; then
        refs+=(-t "$ref")
      fi
    done
    if (( ''${#refs[@]} )); then
      remove-references-to "''${refs[@]}" "$runner"
    fi

    wrapProgram $out/bin/headlessmc \
      --add-flags "-Dhmc.java.download=false" \
      --add-flags "-Dhmc.updater.enabled=false" \
      --add-flags "-Dhmc.plugins.mutable-jar=$out/lib/headlessmc-mutable" \
      --prefix PATH : ${lib.makeBinPath ([ gameJdk ] ++ extraJdks)}
  '';

  # guards against the build environment coming back into the native image (see postFixup)
  disallowedReferences = lib.optionals native [
    graalvm
    gradle
    stdenv.cc
  ];

  meta = {
    description = "Headless Minecraft launcher and command line client";
    homepage = "https://github.com/headlesshq/headlessmc";
    changelog = "https://github.com/headlesshq/headlessmc/blob/main/CHANGELOG.md";
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
