package io.github.headlesshq.headlessmc.commands.profile;

import io.github.headlesshq.headlessmc.auth.Account;
import io.github.headlesshq.headlessmc.auth.AuthProvider;
import io.github.headlesshq.headlessmc.auth.AuthService;
import io.github.headlesshq.headlessmc.auth.LastUsedAccountService;
import io.github.headlesshq.headlessmc.commands.server.AbstractServerLaunchCommand;
import io.github.headlesshq.headlessmc.console.ArgSplitter;
import io.github.headlesshq.headlessmc.console.Console;
import io.github.headlesshq.headlessmc.files.McFiles;
import io.github.headlesshq.headlessmc.launcher.LifecycleService;
import io.github.headlesshq.headlessmc.launcher.client.ClientLauncher;
import io.github.headlesshq.headlessmc.launcher.profile.LaunchOptions;
import io.github.headlesshq.headlessmc.launcher.profile.Profile;
import io.github.headlesshq.headlessmc.launcher.profile.ProfileResolver;
import io.github.headlesshq.headlessmc.launcher.server.ServerLauncher;
import io.github.headlesshq.headlessmc.launcher.server.ServerService;
import io.github.headlesshq.headlessmc.platform.PlatformService;
import io.github.headlesshq.headlessmc.version.arg.Side;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import picocli.CommandLine;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Slf4j
@Getter
@Setter
public abstract class AbstractClientAndServerLaunchCommand extends AbstractServerLaunchCommand {
    private final LastUsedAccountService lastUsedAccountService;
    private final ClientLauncher clientLauncher;
    private final XvfbService xvfbService;
    private final AuthService authService;

    @CommandLine.Option(
        names = {"-g", "--game"},
        description = "Arguments for the started game, e.g. --game \"--quickPlayRealms <realms-server>\""
    )
    private @Nullable String gameArgs;

    @CommandLine.Option(names = {"--headless", "-lwjgl"}, description = "Patches LWJGL to not render anything.")
    private @Nullable Boolean headless;

    @CommandLine.Option(names = {"--offline"}, description = "Launches with an offline account.")
    private boolean offline = false;

    @CommandLine.Option(names = {"--server"}, description = "Joins a server immediately after launching the game")
    private @Nullable String server;

    @CommandLine.Option(
        names = {"--resolution", "-res"},
        description = "Resolution to start the client with, <width>x<height>, e.g. 800x600"
    )
    private @Nullable String resolution;

    public AbstractClientAndServerLaunchCommand(
        LifecycleService lifecycleService,
        ProfileResolver profileResolver,
        PlatformService platformService,
        ServerLauncher serverLauncher,
        ServerService serverService,
        ClientLauncher clientLauncher,
        ArgSplitter splitter,
        Console console,
        McFiles mcFiles,
        LastUsedAccountService lastUsedAccountService,
        XvfbService xvfbService,
        AuthService authService
    ) {
        super(
            lifecycleService,
            profileResolver,
            platformService,
            serverLauncher,
            serverService,
            splitter,
            console,
            mcFiles
        );
        this.lastUsedAccountService = lastUsedAccountService;
        this.clientLauncher = clientLauncher;
        this.xvfbService = xvfbService;
        this.authService = authService;
    }

    @Override
    public Integer call() {
        LaunchOptions options = getLaunchOptions();
        Profile baseProfile = getProfile();
        Profile profile = prepareProfile(baseProfile)
            .withOptions(options.mergeWithDefaults(baseProfile.options()));

        if (Side.CLIENT.equals(profile.side())) {
            Account account = getAccount();
            boolean headless = Boolean.TRUE.equals(this.headless);
            if (!headless
                && AuthProvider.OFFLINE.equalsIgnoreCase(account.getProvider())
                && !hasGame()
                && !xvfbService.isRunningWithXvfb()) {
                log.warn("You are offline without the game, forcing headless mode.");
                headless = true;
            }

            profile = profile.withPatchers(resolvePatchers(profile, headless));
            return handle(getClientLauncher().launcher(profile, account));
        } else {
            if (!options.isEmpty()) {
                log.warn("Launch options --resolution and --server are client only!");
            }

            maybeEulaLaunch(profile);
            // TODO: patchers for server
            return handle(getServerLauncher().launcher(profile));
        }
    }

    // TODO: improve?!
    private List<String> resolvePatchers(Profile profile, boolean headless) {
        List<String> patchers = this.getPatchers();
        if (patchers == null) {
            patchers = new ArrayList<>();
        }

        patchers.add("log4j");
        if (headless) {
            patchers.add("lwjgl");
            patchers.add("paulscode");
        }

        patchers.addAll(profile.patchers());
        return patchers.stream().map(string -> string.toLowerCase(Locale.ENGLISH)).distinct().toList();
    }

    private LaunchOptions getLaunchOptions() {
        Optional<LaunchOptions.Resolution> resolution = Optional.empty();
        String resolutionArg = this.resolution;
        if (resolutionArg != null) {
            resolution = Optional.of(LaunchOptions.Resolution.parse(resolutionArg));
        }

        return new LaunchOptions(
            resolution,
            Optional.of(
                getMcFiles().getMcDir()
                    .resolve("quickPlay")
                    .resolve("java")
                    .resolve(System.currentTimeMillis() + ".json")
                    .toAbsolutePath()
                    .toString()
            ),
            server == null ? Optional.empty() : Optional.of(new LaunchOptions.Join(LaunchOptions.Join.Type.SERVER, server)),
            false
        );
    }

    private Account getAccount() {
        Optional<Account> account = lastUsedAccountService.getLastUsedAccount();
        if (account.isPresent()) {
            if (offline) {
                return account.get();
            }

            return authService.refresh(account.get());
        } else if (offline) {
            return Account.defaultOfflineAccount();
        }

        throw new IllegalStateException("You need an account to play the game. Use the login command.");
    }

    private boolean hasGame() {
        return authService.getProviders()
            .stream()
            .filter(provider -> !AuthProvider.OFFLINE.equalsIgnoreCase(provider.getName()))
            .anyMatch(provider -> !provider.getAccounts().isEmpty());
    }

}
