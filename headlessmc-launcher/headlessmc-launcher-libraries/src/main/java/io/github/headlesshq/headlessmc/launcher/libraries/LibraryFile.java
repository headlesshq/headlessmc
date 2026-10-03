package io.github.headlesshq.headlessmc.launcher.libraries;

import io.github.headlesshq.headlessmc.version.Version;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;

public record LibraryFile(
    Version.Library library,
    Path path,
    Version.@Nullable Download download
    // boolean isNative // TODO: find out if newer versions still extract native libraries?
) {

}
