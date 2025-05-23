package io.github.headlesshq.headlessmc.launcher.launch;

import lombok.Builder;
import lombok.CustomLog;
import lombok.Data;
import io.github.headlesshq.headlessmc.api.command.CommandUtil;
import io.github.headlesshq.headlessmc.launcher.Launcher;
import io.github.headlesshq.headlessmc.launcher.auth.LaunchAccount;
import io.github.headlesshq.headlessmc.launcher.files.FileManager;
import io.github.headlesshq.headlessmc.launcher.version.Version;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static io.github.headlesshq.headlessmc.api.command.CommandUtil.flag;
import static io.github.headlesshq.headlessmc.launcher.LauncherProperties.*;

@Data
@Builder
@CustomLog
public class LaunchOptions {
    private final Version version;
    private final Launcher launcher;
    private final FileManager files;
    private final List<String> additionalJvmArgs;
    private final List<String> additionalGameArgs;
    private final LaunchAccount account;
    private final boolean server;
    private final boolean lwjgl;
    private final boolean jndi;
    private final boolean lookup;
    private final boolean paulscode;
    private final boolean noOut;
    private final boolean noIn;
    private final boolean inMemory;
    private final boolean forceSimple;
    private final boolean forceBoot;
    private final boolean xvfb;
    private final boolean prepare;
    private final boolean specifics;
    private final boolean closeCommandLine;

    @SuppressWarnings("unused")
    public static class LaunchOptionsBuilder {
        private LaunchOptionsBuilder() {
            this.additionalJvmArgs = Collections.emptyList();
            this.additionalGameArgs = Collections.emptyList();
        }

        public LaunchOptionsBuilder parseFlags(
            Launcher ctx, boolean quit, String... args) {
            boolean xvfb = false;
            boolean lwjgl = false;
            if (!server) {
                lwjgl = flag(ctx, "-lwjgl", INVERT_LWJGL_FLAG, ALWAYS_LWJGL_FLAG, args);
                // if offline only allow launching with the lwjgl flag!
                if (!lwjgl && launcher.getAccountManager().getOfflineChecker().isOffline()) {
                    xvfb = new XvfbService(launcher.getConfigService(), launcher.getProcessFactory().getOs()).isRunningWithXvfb();
                    if (!xvfb) {
                        log.warning("You are offline, game will start in headless mode!");
                        lwjgl = true;
                    } else {
                        log.info("You are offline but running with xvfb, not using headless mode.");
                    }
                }
            }

            boolean noOut = quit || CommandUtil.hasFlag("-noout", args);
            boolean noIn = quit;
            if (launcher.getConfig().get(SERVER_TEST, false)
                || launcher.getConfig().get(TEST_FILE, null) != null) {
                noOut = true;
                noIn = true;
            }

            boolean specifics = CommandUtil.hasFlag("-specifics", args)
                    || launcher.getConfig().get(AUTO_DOWNLOAD_SPECIFICS, false);

            return this
                .specifics(specifics)
                .lwjgl(lwjgl)
                .inMemory(CommandUtil.hasFlag("-inmemory", args) || launcher.getConfig().get(ALWAYS_IN_MEMORY, false))
                .jndi(flag(ctx, true, "-jndi", INVERT_JNDI_FLAG, ALWAYS_JNDI_FLAG, args))
                .lookup(flag(ctx, true, "-lookup", INVERT_LOOKUP_FLAG, ALWAYS_LOOKUP_FLAG, args))
                .paulscode(flag(ctx, "-paulscode", INVERT_PAULS_FLAG, ALWAYS_PAULS_FLAG, args))
                .noOut(noOut)
                .forceSimple(CommandUtil.hasFlag("-forceSimple", args))
                .forceBoot(CommandUtil.hasFlag("-forceBoot", args))
                .parseJvmArgs(args)
                .parseGameArgs(args)
                .xvfb(xvfb)
                .noIn(noIn);
        }

        public LaunchOptionsBuilder parseJvmArgs(String... args) {
            String jvmArgs = CommandUtil.getOption("--jvm", args);
            if (jvmArgs != null) {
                this.additionalJvmArgs = new ArrayList<>(Arrays.asList(CommandUtil.split(jvmArgs)));
            }

            return this;
        }

        public LaunchOptionsBuilder parseGameArgs(String... args) {
            String gameArgs = CommandUtil.getOption("--game-args", args);
            if (gameArgs != null) {
                this.additionalGameArgs = new ArrayList<>(Arrays.asList(CommandUtil.split(gameArgs)));
            }

            return this;
        }
    }

}
