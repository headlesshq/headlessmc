package io.github.headlesshq.headlessmc.launcher;

import io.github.headlesshq.headlessmc.console.Console;
import io.github.headlesshq.headlessmc.exceptions.UncheckedInterruptedException;
import io.github.headlesshq.headlessmc.java.launcher.ProcessHandler;
import io.github.headlesshq.headlessmc.launcher.process.McProcess;
import io.github.headlesshq.headlessmc.launcher.process.ProcessLauncher;
import io.github.headlesshq.headlessmc.test.CommandTest;
import io.github.headlesshq.headlessmc.test.CommandTestService;
import io.github.headlesshq.headlessmc.test.TestConfig;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

// Legacy HeadlessMc code, rewrite candidate
/**
 * Handles the lifecycle of a launched mc process.
 * Retries, testing etc.
 */
@Slf4j
@Getter
@Setter
@RequiredArgsConstructor
public class ProcessLifecycle implements Callable<Integer> {
    private final ProcessLauncher processFactory;
    private final CommandTestService testService;
    private final LauncherConfig config;
    private final TestConfig testConfig;
    private final Console console;
    private final Path gameDir;
    private int retries;

    @Override
    public Integer call() {
        int status = 0;
        for (int i = 0; i < retries + 1; i++) {
            if (i > 0) {
                log.warn("Retrying to launch Minecraft: {}", i);
            }

            AtomicReference<Process> processRef = new AtomicReference<>();
            AtomicReference<Path> crashReport = new AtomicReference<>();
            // CrashReportWatcher listens and kills process if crash-report appears
            try (CrashReportWatcher _ = createCrashReportWatcher(processRef, crashReport, gameDir)) {
                McProcess process = processFactory.launch(testService.isTestActive());
                process.process().ifPresent(javaProcess -> {
                    processRef.set(javaProcess);
                    runTest(javaProcess);
                });

                if (process.process().isEmpty()) {
                    if (crashReport.get() != null) {
                        throw new io.github.headlesshq.headlessmc.launcher.process.LaunchException("CrashReport detected " + crashReport.get());
                    }

                    return 0;
                } else if (i == retries) {
                    garbageCollectHmc(process.process().get());
                }

                status = process.waitFor(ProcessHandler.defaultHandler());
                log.info("Minecraft exited with code: {}", status);
                if (status == 0) {
                    if (crashReport.get() != null) {
                        throw new io.github.headlesshq.headlessmc.launcher.process.LaunchException("CrashReport detected " + crashReport.get());
                    }

                    break;
                }
            } catch (IOException e) {
                throw new io.github.headlesshq.headlessmc.launcher.process.LaunchException(e);
            } catch (InterruptedException e) {
                throw new UncheckedInterruptedException(e);
            }
        }

        return status;
    }

    private void garbageCollectHmc(Process process) {
        if (config.free()) {
            // TODO: implement
            //throw new McLaunchedExitHmcException(process);
        }
    }

    private void runTest(Process process) {
        try (CommandTest commandTest = testService.createCommandTest(process, console::write)) {
            if (commandTest == null) {
                return;
            }

            log.info("Running CommandTest");
            commandTest.run();
            if (commandTest.wasSuccessful()) {
                log.info("CommandTest was successful.");
            } else {
                log.error("CommandTest failed!");
                log.error("Message: {}", commandTest.getMessage());
            }

            if (!commandTest.wasSuccessful() || testConfig.leave()) {
                // TODO: processUtils.exit?
                commandTest.awaitExitOrKill();
            }

            if (!commandTest.wasSuccessful()) {
                throw new io.github.headlesshq.headlessmc.launcher.process.LaunchException("CommandTest failed");
            }
        } catch (IOException e) {
            throw new io.github.headlesshq.headlessmc.launcher.process.LaunchException("Failed to load test", e);
        } catch (InterruptedException e) {
            throw new UncheckedInterruptedException();
        }
    }

    private @Nullable CrashReportWatcher createCrashReportWatcher(
        AtomicReference<Process> processRef,
        AtomicReference<Path> crashReport,
        Path gameDir
    ) throws IOException, InterruptedException {
        CrashReportWatcher crashReportWatcher = null;
        if (config.crashReportWatcher()) {
            log.info("Initializing Crash Report Watcher for {}", gameDir);
            crashReportWatcher = CrashReportWatcher.forGameDir(gameDir);
            crashReportWatcher.addListener(reportPath -> {
                log.error("Crash Report created at :{}", reportPath);
                crashReport.set(reportPath);
                if (processRef.get() != null) {
                    processRef.get().destroy();
                    // TODO: processUtils.exit?
                } else {
                    log.info("Crash Report Watcher cannot exit.");
                }
            });

            crashReportWatcher.waitForStart();
        }

        return crashReportWatcher;
    }

}
