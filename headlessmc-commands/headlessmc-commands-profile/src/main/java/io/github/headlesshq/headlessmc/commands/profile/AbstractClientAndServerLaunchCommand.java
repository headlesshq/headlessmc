package io.github.headlesshq.headlessmc.commands.profile;

import io.github.headlesshq.headlessmc.auth.Account;
import io.github.headlesshq.headlessmc.auth.AuthProvider;
import io.github.headlesshq.headlessmc.auth.AuthService;
import io.github.headlesshq.headlessmc.auth.LastUsedAccountService;
import io.github.headlesshq.headlessmc.commands.server.AbstractServerLaunchCommand;
import io.github.headlesshq.headlessmc.console.ArgSplitter;
import io.github.headlesshq.headlessmc.console.Console;
import io.github.headlesshq.headlessmc.files.FileService;
import io.github.headlesshq.headlessmc.files.McFilesService;
import io.github.headlesshq.headlessmc.launcher.LifecycleService;
import io.github.headlesshq.headlessmc.launcher.client.ClientLauncher;
import io.github.headlesshq.headlessmc.launcher.profile.LaunchOptions;
import io.github.headlesshq.headlessmc.launcher.profile.LaunchTargetService;
import io.github.headlesshq.headlessmc.launcher.profile.Profile;
import io.github.headlesshq.headlessmc.launcher.profile.ProfileResolver;
import io.github.headlesshq.headlessmc.launcher.server.ServerLauncher;
import io.github.headlesshq.headlessmc.launcher.server.ServerService;
import io.github.headlesshq.headlessmc.patcher.Patcher;
import io.github.headlesshq.headlessmc.platform.PlatformService;
import io.github.headlesshq.headlessmc.util.typemap.TypedMap;
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
        LaunchTargetService launchTargetService,
        McFilesService mcFilesService,
        FileService fileService,
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
            launchTargetService,
            mcFilesService,
            fileService
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
        checkOptions(baseProfile);
        beforeLaunch(baseProfile);
        TypedMap targetOptions = getTargetOptions();
        // everything that is saved is done with the profile as it is stored: what the launch target finds out ...
        Profile stored = resolve(baseProfile, targetOptions);
        // ... and only what is launched gets the options of this launch
        Profile profile = prepareProfile(stored)
            .withOptions(options.mergeWithDefaults(stored.options()));

        if (profile.isClient()) {
            Account account = getAccount();
            boolean headless = Boolean.TRUE.equals(this.headless);
            if (!headless
                && needsHeadless(account)
                && !hasGame()
                && !xvfbService.isRunningWithXvfb()) {
                log.warn("No account for a purchased copy of Minecraft found, "
                    + "forcing headless mode. Log in with a Microsoft account to launch with a GUI.");
                headless = true;
            }

            // a profile can inherit jvm arguments, check what is actually launched
            warnIfYggdrasilAgentMissing(getLaunchTargetService().effective(profile), account);

            profile = profile.withPatchers(resolvePatchers(profile, headless));
            targetOptions.put(ClientLauncher.ACCOUNT, account);
        } else if (profile.isServer()) {
            if (hasClientOptions(options)) {
                log.warn("Launch options --resolution and --server are client only!");
            }

            maybeEulaLaunch(stored, profile);
            // TODO: patchers for server
        }

        return handle(getLaunchTargetService().launcher(profile, targetOptions));
    }

    /**
     * Something that is neither a client nor a server can be launched, but nothing that is specific to
     * clients or servers applies to it, and nothing must pretend it does.
     */
    @Override
    protected void checkOptions(Profile profile) throws IllegalArgumentException {
        super.checkOptions(profile);
        if (!profile.isClient()
            && !profile.isServer()
            && (resolution != null || server != null || headless != null || offline)) {
            throw new IllegalArgumentException(
                "Profile " + profile.name() + " is neither a client nor a server, the options --resolution, "
                    + "--server, --headless and --offline cannot be used with it."
            );
        }
    }

    private static boolean hasClientOptions(LaunchOptions options) {
        return !options.isEmpty();
    }

    /**
     * Warns when a Yggdrasil account is launched without authlib-injector in the vm args.
     *
     * <p>The client only talks to a third party authentication server when
     * authlib-injector is injected as a java agent. Without it the client sends the
     * {@code session/minecraft/join} request to Mojang, the Minecraft server never
     * finds the session and kicks the player with "invalid session". Since the login
     * itself succeeds, users would not know what is missing.</p>
     */
    private void warnIfYggdrasilAgentMissing(Profile profile, Account account) {
        if (!AuthProvider.YGGDRASIL.equalsIgnoreCase(account.getProvider())) {
            return;
        }

        for (String arg : profile.vmArgs()) {
            if (arg.contains("-javaagent") && arg.contains("authlib-injector")) {
                return;
            }
        }

        log.warn("This account uses third party Yggdrasil authentication. "
            + "To join servers with online-mode enabled, authlib-injector has to be "
            + "added as a java agent, otherwise the game will be kicked with "
            + "\"invalid session\". Download it and add the following to the vm args "
            + "of this profile using: profile edit {} vm-args \"...\"", profile.name());
        log.warn("-javaagent:/path/to/authlib-injector.jar=<your yggdrasil api url>");
    }

    /**
     * The patchers of this launch, added to the ones the profile selects. This only selects patchers:
     * the profile can inherit patchers this command does not see, so patchers replacing each other, like the
     * patched LWJGL replacing the {@link Patcher#LWJGL} patcher selected here, are sorted out when the game is
     * launched, see {@link Patcher#excludes()}.
     */
    private List<String> resolvePatchers(Profile profile, boolean headless) {
        List<String> patchers = new ArrayList<>(this.getPatchers() == null ? List.of() : this.getPatchers());
        patchers.add(Patcher.LOG4J);
        if (headless) {
            patchers.add(Patcher.LWJGL);
            patchers.add(Patcher.PAULSCODE);
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
            // the quick play log is written into the .minecraft directory the game is launched from,
            // which is only known once the launch target has been resolved, see TemplateStringService
            Optional.empty(),
            server == null ? Optional.empty() : Optional.of(new LaunchOptions.Join(LaunchOptions.Join.Type.SERVER, server)),
            false
        );
    }

    /**
     * The account to launch with. A {@link #isDryRun() dry run} only downloads files,
     * so it neither needs an account nor refreshes one.
     */
    private Account getAccount() {
        Optional<Account> account = lastUsedAccountService.getLastUsedAccount();
        if (isDryRun()) {
            return account.orElseGet(Account::defaultOfflineAccount);
        }

        if (account.isPresent()) {
            if (offline) {
                return account.get();
            }

            return authService.refreshIfNeeded(account.get());
        } else if (offline) {
            return Account.defaultOfflineAccount();
        }

        throw new IllegalStateException("You need an account to play the game. Use the login command.");
    }

    /**
     * Whether the user has an account that allows launching with a GUI.
     *
     * <p>Offline accounts are excluded because they carry no information about
     * owning the game, which is why the account used for this launch is checked
     * separately. Yggdrasil accounts are excluded for the same reason, they only
     * authenticate against a third party server.</p>
     */
    private boolean hasGame() {
        return authService.getProviders()
            .stream()
            .filter(provider -> !needsHeadless(provider.getName()))
            .anyMatch(provider -> !provider.getAccounts().isEmpty());
    }

    /**
     * Whether an account of this provider cannot launch with a GUI.
     *
     * <p>Offline accounts may belong to a user who owns the game, see
     * {@link #hasGame()}. Yggdrasil accounts do not, they only authenticate
     * against a third party server.</p>
     */
    private static boolean needsHeadless(String provider) {
        return AuthProvider.OFFLINE.equalsIgnoreCase(provider)
            || AuthProvider.YGGDRASIL.equalsIgnoreCase(provider);
    }

    private static boolean needsHeadless(Account account) {
        return needsHeadless(account.getProvider());
    }

}
