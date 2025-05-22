package io.github.headlesshq.headlessmc.launcher.command;

import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import lombok.val;
import io.github.headlesshq.headlessmc.api.traits.HasName;
import io.github.headlesshq.headlessmc.api.command.CommandException;
import io.github.headlesshq.headlessmc.api.command.CommandUtil;
import io.github.headlesshq.headlessmc.api.command.ParseUtil;
import io.github.headlesshq.headlessmc.launcher.Launcher;
import io.github.headlesshq.headlessmc.launcher.LauncherProperties;
import io.github.headlesshq.headlessmc.launcher.auth.AuthException;
import io.github.headlesshq.headlessmc.launcher.files.FileManager;
import io.github.headlesshq.headlessmc.launcher.launch.ExitToWrapperException;
import io.github.headlesshq.headlessmc.launcher.launch.LaunchException;
import io.github.headlesshq.headlessmc.launcher.server.commands.LaunchServerCommand;
import io.github.headlesshq.headlessmc.launcher.test.CommandTest;
import io.github.headlesshq.headlessmc.launcher.test.CrashReportWatcher;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

import static io.github.headlesshq.headlessmc.api.command.CommandUtil.flag;
import static io.github.headlesshq.headlessmc.launcher.LauncherProperties.RE_THROW_LAUNCH_EXCEPTIONS;

/**
 * This class handles the lifecycle of launching and running a Process.
 * It also handles retries and what we do after the process has shutdown.
 *
 * @see LaunchCommand
 * @see LaunchServerCommand
 */
@CustomLog
@RequiredArgsConstructor
public abstract class AbstractLaunchProcessLifecycle {
    private static final String GARBAGE_COLLECT_HMC = "hmc.wrapper.garbage.collect.hmc";

    protected final Launcher ctx;
    protected final String[] args;

    protected FileManager files;
    protected boolean quit;
    protected boolean prepare;

    protected abstract Path getGameDir();

    protected abstract @Nullable Process createProcess() throws LaunchException, AuthException, IOException, CommandException;

    public void run(HasName version) throws CommandException {
        prepare = CommandUtil.hasFlag("-prepare", args);
        val uuid = UUID.fromString(ctx.getConfig().get(LauncherProperties.EXTRACTED_FILE_CACHE_UUID, UUID.randomUUID().toString()));
        ctx.log((prepare ? "Preparing" : "Launching") + " version " + version.getName() + ", " + uuid);
        ctx.getLoggingService().setLevel(Level.INFO, true);
        files = ctx.getFileManager().createRelative(uuid.toString());

        quit = flag(ctx, "-quit", LauncherProperties.INVERT_QUIT_FLAG, LauncherProperties.ALWAYS_QUIT_FLAG, args);
        int status = 0;
        boolean exitToWrapper = false;
        try {
            status = runProcess();
        } catch (LaunchException | AuthException e) {
            status = -1;
            // ignore this specific message for tests, because otherwise Intellij shows it as red which looks like something failed
            if (!(e instanceof LaunchException && "Mock Factory".equals(e.getMessage()))) {
                log.error(e);
            }

            ctx.log(String.format("Couldn't launch %s: %s", version.getName(), e.getMessage()));
            if (ctx.getConfig().get(RE_THROW_LAUNCH_EXCEPTIONS, false)) {
                throw new IllegalStateException(e);
            }
        } catch (ExitToWrapperException e) {
            exitToWrapper = true;
            throw e;
        } catch (Throwable t) {
            status = -1;
            val msg = String.format("Couldn't launch %s: %s", version.getName(), t.getMessage());
            log.error(msg, t);
            ctx.log(msg);
            throw t;
        } finally {
            if (!exitToWrapper) {
                cleanup(files, args);
                if (!prepare && !CommandUtil.hasFlag("-stay", args)) {
                    ctx.getExitManager().exit(status);
                }
            }
        }

        if (status != 0 && ctx.getConfig().get(RE_THROW_LAUNCH_EXCEPTIONS, false)) {
            throw new IllegalStateException("Minecraft exited with code " + status);
        }

        if (!prepare) {
            try {
                ctx.getCommandLine().open(ctx);
            } catch (IOException ioException) {
                throw new IllegalStateException("Failed to reopen HeadlessMc CommandLineReader", ioException);
            }
        }
    }

    /**
     * Hook in {@link #runProcess} to notify {@link LaunchCommand} to check if we are logged in.
     *
     * @see LaunchCommand
     * @throws CommandException if we cannot log in.
     */
    protected void getAccount() throws CommandException {
        // to be implemented by subclasses
    }

    private int runProcess() throws CommandException, LaunchException, AuthException {
        int status = 0;
        if (CommandUtil.hasFlag("-offline", args)) {
            ctx.getAccountManager().getOfflineChecker().setOffline(true);
        }

        getAccount();
        String retriesOption = CommandUtil.getOption("--retries", args);
        int retries = 0;
        if (retriesOption != null) {
            retries = ParseUtil.parseI(retriesOption);
        }

        Throwable throwable = null;
        for (int i = 0; i < retries + 1; i++) {
            if (i > 0) {
                log.warn("Retrying to launch Minecraft: " + i);
            }

            CrashReportWatcher crashReportWatcher = null;
            try {
                AtomicReference<Process> processRef = new AtomicReference<>();
                AtomicReference<Path> crashReport = new AtomicReference<>();
                crashReportWatcher = createCrashReportWatcher(processRef, crashReport);

                Process process = createProcess();
                processRef.set(process);
                if (prepare) {
                    return 0;
                }

                if (process == null) {
                    ctx.log("InMemory main thread ended.");
                }

                runTest(process);
                if (quit || process == null) {
                    if (crashReport.get() != null) {
                        throw new LaunchException("CrashReport detected " + crashReport.get());
                    }

                    cleanup(files, args);
                    ctx.getExitManager().exit(0);
                    return 0;
                }

                garbageCollectHmc(process);
                try {
                    status = process.waitFor();
                    ctx.log("Minecraft exited with code: " + status);
                } catch (InterruptedException ie) {
                    ctx.log("Launcher has been interrupted...");
                    Thread.currentThread().interrupt();
                }

                if (status == 0) {
                    if (crashReport.get() != null) {
                        throw new LaunchException("CrashReport detected " + crashReport.get());
                    }

                    break;
                }
            } catch (ExitToWrapperException e) {
                log.info("Exiting to wrapper.");
                throw e;
            } catch (Throwable t) {
                status = -1;
                log.error("Failed to start Minecraft on try " + i, t);
                if (throwable == null) {
                    throwable = t;
                } else {
                    throwable.addSuppressed(t);
                }
            } finally {
                if (crashReportWatcher != null) {
                    try {
                        crashReportWatcher.close();
                    } catch (IOException e) {
                        log.error("Failed to close CrashReportWatcher", e);
                    }
                }
            }
        }

        return handleLaunchException(status, throwable);
    }

    private void garbageCollectHmc(@Nullable Process process) throws LaunchException {
        if (process == null) {
            return;
        }

        if (Boolean.parseBoolean(System.getProperty(GARBAGE_COLLECT_HMC, "false"))) {
            try {
                Class<?> processThread = Class.forName("io.github.headlesshq.headlessmc.wrapper.ProcessThread");
                Method method = processThread.getDeclaredMethod("setProcessInstance", Process.class);
                method.invoke(null, process);
                throw new ExitToWrapperException();
            } catch (ReflectiveOperationException e) {
                log.error(e);
                process.destroyForcibly();
                throw new LaunchException(e);
            }
        }
    }

    private void runTest(@Nullable Process process) throws Exception {
        try (CommandTest commandTest = CommandTest.create(process, ctx)) {
            if (commandTest == null) {
                return;
            }

            log.info("Running CommandTest");
            commandTest.run();
            if (commandTest.wasSuccessful()) {
                log.info("CommandTest was successful.");
            } else {
                log.error("CommandTest failed!");
                log.error("Message: " + commandTest.getMessage());
            }

            if (!commandTest.wasSuccessful()
                || ctx.getConfig().get(LauncherProperties.LEAVE_AFTER_TEST, true)) {
                commandTest.awaitExitOrKill();
            }

            if (!commandTest.wasSuccessful()) {
                throw new LaunchException("CommandTest failed");
            }
        }
    }

    private int handleLaunchException(int status, Throwable throwable) throws LaunchException {
        if (status != 0 && throwable != null) {
            if (throwable instanceof RuntimeException) {
                throw (RuntimeException) throwable;
            }

            if (throwable instanceof LaunchException) {
                throw (LaunchException) throwable;
            }

            throw new LaunchException(throwable);
        }

        return status;
    }

    private @Nullable CrashReportWatcher createCrashReportWatcher(
            AtomicReference<Process> processRef,
            AtomicReference<Path> crashReport) throws IOException, InterruptedException
    {
        CrashReportWatcher crashReportWatcher = null;
        if (ctx.getConfig().get(LauncherProperties.CRASH_REPORT_WATCHER, false)) {
            Path gameDir = getGameDir();
            log.info("Initializing Crash Report Watcher for " + gameDir);
            crashReportWatcher = CrashReportWatcher.forGameDir(gameDir);
            crashReportWatcher.addListener(reportPath -> {
                log.error("Crash Report created at :" + reportPath);
                crashReport.set(reportPath);
                if (processRef.get() != null) {
                    processRef.get().destroy();
                } else if (ctx.getConfig().get(LauncherProperties.CRASH_REPORT_WATCHER_EXIT, true)) {
                    System.exit(-1);
                } else {
                    log.info("Crash Report Watcher cannot exit.");
                }
            });

            crashReportWatcher.waitForStart();
        }

        return crashReportWatcher;
    }

    private void cleanup(FileManager files, String... args) {
        // for some reason both ShutdownHooks and File.deleteOnExit are
        // not really working, that's why we Main.deleteOldFiles, too.
        if (!CommandUtil.hasFlag("-keep", args) && !ctx.getConfig().get(LauncherProperties.KEEP_FILES, false)) {
            try {
                log.info("Deleting " + files.getBase().getName());
                ctx.getFileManager().delete(files.getBase());
            } catch (IOException e) {
                log.error("Couldn't delete files of game " + files.getBase().getName(), e);
            }
        }
    }

}
