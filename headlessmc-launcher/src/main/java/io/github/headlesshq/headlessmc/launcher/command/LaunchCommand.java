package io.github.headlesshq.headlessmc.launcher.command;

import lombok.CustomLog;
import io.github.headlesshq.headlessmc.api.command.CommandException;
import io.github.headlesshq.headlessmc.launcher.Launcher;
import io.github.headlesshq.headlessmc.launcher.LauncherProperties;
import io.github.headlesshq.headlessmc.launcher.auth.AuthException;
import io.github.headlesshq.headlessmc.launcher.auth.LaunchAccount;
import io.github.headlesshq.headlessmc.auth.ValidatedAccount;
import io.github.headlesshq.headlessmc.launcher.command.download.AbstractDownloadingVersionCommand;
import io.github.headlesshq.headlessmc.launcher.launch.LaunchException;
import io.github.headlesshq.headlessmc.launcher.launch.LaunchOptions;
import io.github.headlesshq.headlessmc.launcher.version.Version;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

@CustomLog
public class LaunchCommand extends AbstractDownloadingVersionCommand {
    public LaunchCommand(Launcher launcher) {
        super(launcher, "launch", "Launches the game.");
        args.put("<version/id>", "Name or id of the version to launch. If you use the id you need to use the -id flag as well.");
        args.put("-id", "Use if you specified an id instead of a version name.");
        args.put("-commands", "Starts the game with the built-in command line support.");
        args.put("-lwjgl", "Removes lwjgl code, causing Minecraft not to render anything.");
        args.put("-inmemory", "Launches the game in the same JVM headlessmc is running in.");
        args.put("-jndi", "Patches the Log4J vulnerability.");
        args.put("-lookup", "Patches the Log4J vulnerability even harder.");
        args.put("-paulscode", "Removes some error messages from the PaulsCode library which may annoy you if you started the game with the -lwjgl flag.");
        args.put("-noout", "Doesn't print Minecrafts output to the console."); // TODO: is this really necessary?
        args.put("-quit", "Quit HeadlessMc after launching the game.");
        args.put("-offline", "Launch Mc in offline mode.");
        args.put("--jvm", "Jvm args to use.");
        args.put("--retries", "The amount of times you want to retry running Minecraft.");
    }

    @Override
    public void execute(Version version, String... args) throws CommandException {
        ClientLaunchProcessLifecycle lifecycle = new ClientLaunchProcessLifecycle(version, args);
        lifecycle.run(version);
    }

    private class ClientLaunchProcessLifecycle extends AbstractLaunchProcessLifecycle {
        private final Version version;
        private @Nullable LaunchAccount account;

        public ClientLaunchProcessLifecycle(Version version, String[] args) {
            super(LaunchCommand.this.ctx, args);
            this.version = version;
        }

        @Override
        protected void getAccount() throws CommandException {
            this.account = LaunchCommand.this.getAccount();
        }

        @Override
        protected Path getGameDir() {
            return Paths.get(ctx.getConfig().get(LauncherProperties.GAME_DIR, ctx.getGameDir(version).getPath())).toAbsolutePath();
        }

        @Override
        protected @Nullable Process createProcess() throws LaunchException, AuthException, IOException {
            return ctx.getProcessFactory().run(
                    LaunchOptions.builder()
                            .account(account)
                            .version(version)
                            .launcher(ctx)
                            .files(files)
                            .closeCommandLine(!prepare)
                            .parseFlags(ctx, quit, args)
                            .prepare(prepare)
                            .build()
            );
        }
    }

    protected LaunchAccount getAccount() throws CommandException {
        try {
            ValidatedAccount account = ctx.getAccountManager().getPrimaryAccount();
            if (account == null) {
                if (ctx.getAccountManager().getOfflineChecker().isOffline()) {
                    return ctx.getAccountManager().getOfflineAccount(ctx.getConfig());
                }

                throw new AuthException("You can't play the game without an account! Please use the login command.");
            } else {
                if (ctx.getConfig().get(LauncherProperties.REFRESH_ON_GAME_LAUNCH, true)) {
                    try {
                        account = ctx.getAccountManager().refreshAccount(account, ctx.getConfig());
                    } catch (AuthException e) {
                        if (ctx.getConfig().get(LauncherProperties.FAIL_LAUNCH_ON_REFRESH_FAILURE, false)) {
                            throw e;
                        }
                    }
                }

                return toLaunchAccount(account);
            }
        } catch (AuthException e) {
            throw new CommandException(e.getMessage());
        }
    }

    private LaunchAccount toLaunchAccount(ValidatedAccount account) {
        return new LaunchAccount("msa",
                account.getSession().getMcProfile().getName(),
                account.getSession().getMcProfile().getId().toString(),
                account.getSession().getMcProfile().getMcToken().getAccessToken(),
                account.getXuid());
    }

}
