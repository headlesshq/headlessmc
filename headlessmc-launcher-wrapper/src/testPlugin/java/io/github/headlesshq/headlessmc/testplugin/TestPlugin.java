package io.github.headlesshq.headlessmc.testplugin;

import lombok.CustomLog;
import io.github.headlesshq.headlessmc.api.command.DefaultCommandLineProvider;
import io.github.headlesshq.headlessmc.java.Java;
import io.github.headlesshq.headlessmc.launcher.Launcher;
import io.github.headlesshq.headlessmc.launcher.LauncherProperties;
import io.github.headlesshq.headlessmc.launcher.plugin.HeadlessMcPlugin;
import org.junit.platform.commons.util.ExceptionUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@CustomLog
public class TestPlugin implements HeadlessMcPlugin {
    @Override
    public String getName() {
        return "Test";
    }

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public void init(Launcher launcher) {
        launcher.getExitManager().setExitManager(i -> assertEquals(0, i, "Exit code should be 0!"));
        launcher.getExitManager().setMainThreadEndHook(throwable -> {
            if (throwable != null) {
                Throwable cause = throwable;
                while (cause != null) {
                    if (cause instanceof ExitTrap.ExitTrappedException) {
                        if (((ExitTrap.ExitTrappedException) cause).getStatus() != 0) {
                            log.error("ExitCode != 0!");
                            ExceptionUtils.throwAsUncheckedException(throwable);
                        }

                        return;
                    }

                    cause = cause.getCause();
                }

                ExceptionUtils.throwAsUncheckedException(throwable); // TODO: this is caught and added as suppressed?
            }
        });
        // TransformerPlugin will not let us load this class
        assertThrows(ClassNotFoundException.class, () -> Class.forName("io.github.headlesshq.headlessmc.testplugin.DummyClassThatCantBeLoaded"));
        // ReadablePrintStream out = new ReadablePrintStream();
        // launcher.getStdIO().setOut(() -> out);
        // launcher.getStdIO().setErr(() -> out);
        TestInputStream in = new TestInputStream();
        launcher.getCommandLine().getStdIO().setIn(() -> in);
        launcher.getCommandLine().setCommandLineProvider(new DefaultCommandLineProvider(launcher.getCommandLine().getStdIO()));

        System.setProperty(LauncherProperties.RE_THROW_LAUNCH_EXCEPTIONS.getName(), "true");
        Java java = launcher.getJavaService().getCurrent();
        if (java == null) {
            java = new Java("unknown", launcher.getConfig().get(LauncherProperties.ASSUMED_JAVA_VERSION, 8L).intValue());
        }

        LaunchTest.build(java, launcher, in);
    }

    @Override
    public String getDescription() {
        return "A test plugin.";
    }

}
