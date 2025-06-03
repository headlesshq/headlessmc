package io.github.headlesshq.headlessmc.launcher.launch;

import io.github.headlesshq.headlessmc.java.Java;
import io.github.headlesshq.headlessmc.launcher.auth.AuthException;
import io.github.headlesshq.headlessmc.version.Version;
import lombok.CustomLog;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

// TOP 10 worst ideas #9 this
@Getter
@CustomLog
public class InMemoryLauncher extends AbstractInMemoryGameProcessLauncher {
    private final JavaLaunchCommandBuilder command;
    private final Version version;

    public InMemoryLauncher(LaunchOptions options, JavaLaunchCommandBuilder command, Version version, @Nullable Java java) {
        super(options, java);
        this.command = command;
        this.version = version;
    }

    @Override
    protected String getMainClassName() {
        return command.getActualMainClass(new ArrayList<>());
    }

    @Override
    protected List<String> getClasspath() {
        return command.getClasspath();
    }

    @Override
    protected List<String> buildCommand() throws LaunchException, AuthException {
        return command.build();
    }

    @Override
    protected boolean needsJava9(Java java) {
        return java.getVersion() > 8
            && !options.isForceSimple()
            && ("cpw.mods.bootstraplauncher.BootstrapLauncher".equals(version.getMainClass())
                || options.isForceBoot());
    }

}
