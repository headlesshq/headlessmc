package io.github.headlesshq.headlessmc.launcher.server;

import io.github.headlesshq.headlessmc.files.FileService;
import io.github.headlesshq.headlessmc.launcher.profile.PathCandidates;
import io.github.headlesshq.headlessmc.reflection.ReflectionRegistered;
import io.github.headlesshq.headlessmc.util.json.JsonService;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * The {@value #NAME} launch target: launches a script, e.g. the {@code run.sh} or {@code run.bat}
 * of a server, see {@link ScriptLauncher}.
 * The script is run with the Java of the profile first on the path, and with the jvm arguments
 * of the profile in {@code JDK_JAVA_OPTIONS}.
 *
 * <p>Arguments: {@code [client|server] <path/to/script>}, e.g. {@code server ./run.sh}.
 * The file has to exist and be a script: an executable file, or a file with one of the extensions
 * {@code .sh}, {@code .bat}, {@code .cmd} and {@code .ps1}. Jars are launched by the {@link JarLaunchTarget}.
 * See {@link AbstractExecutableLaunchTarget} for the side and the game directory of such a profile.</p>
 *
 * <p>A profile without a Java version gets the Java version HeadlessMc itself runs on.</p>
 */
@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class ScriptLaunchTarget extends AbstractExecutableLaunchTarget<ScriptLaunchTarget.Handle> {
    /**
     * The name of this launch target.
     */
    public static final String NAME = "script";
    private static final String[] EXTENSIONS = {".sh", ".bat", ".cmd", ".ps1"};

    private final ServerLauncher serverLauncher;
    private final FileService fileService;
    private final JsonService jsonService;

    @Override
    protected JsonService jsonService() {
        return jsonService;
    }

    @Override
    protected FileService fileService() {
        return fileService;
    }

    @Override
    protected ServerLauncher serverLauncher() {
        return serverLauncher;
    }

    @Override
    protected Class<Handle> handleType() {
        return Handle.class;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    protected Path getFile(Handle handle) {
        return handle.script();
    }

    @Override
    protected Handle newHandle(Path file) {
        return new Handle(file);
    }

    @Override
    protected Predicate<Path> filter() {
        return ScriptLaunchTarget::isScript;
    }

    @Override
    protected void verify(Path file) throws IllegalArgumentException {
        if (!isScript(file)) {
            throw new IllegalArgumentException(
                file + " is not a script, it is neither executable nor a .sh, .bat, .cmd or .ps1 file"
            );
        }
    }

    @Override
    protected int defaultJavaVersion(Path file) {
        return Runtime.version().feature();
    }

    private static boolean isScript(Path file) {
        if (PathCandidates.hasExtension(file, ".jar", ".json", ".toml")) {
            return false;
        }

        return PathCandidates.hasExtension(file, EXTENSIONS) || Files.isExecutable(file);
    }

    /**
     * The data of the {@value #NAME} launch target.
     *
     * @param script the script that is launched.
     */
    @RegisterForReflection
    public record Handle(Path script) implements ReflectionRegistered {
        @SuppressWarnings("ConstantValue")
        public Handle {
            Objects.requireNonNull(script, "Launch target data is missing 'script'");
        }
    }

}
