package io.github.headlesshq.headlessmc.commands.server;

import io.github.headlesshq.headlessmc.commands.util.LaunchTargetCommand;
import io.github.headlesshq.headlessmc.commands.util.LaunchTargetCompletions;
import io.github.headlesshq.headlessmc.commands.util.LaunchTargetNameCompletions;
import io.github.headlesshq.headlessmc.console.Console;
import io.github.headlesshq.headlessmc.console.LongRunningCommand;
import io.github.headlesshq.headlessmc.files.FileService;
import io.github.headlesshq.headlessmc.files.McFilesService;
import io.github.headlesshq.headlessmc.launcher.profile.LaunchTarget;
import io.github.headlesshq.headlessmc.launcher.profile.LaunchTargetService;
import io.github.headlesshq.headlessmc.launcher.profile.Profile;
import io.github.headlesshq.headlessmc.launcher.profile.ProfileService;
import io.github.headlesshq.headlessmc.launcher.profile.TargetArgs;
import io.github.headlesshq.headlessmc.platform.client.ClientInstaller;
import io.github.headlesshq.headlessmc.util.typemap.TypedMap;
import io.github.headlesshq.headlessmc.util.typemap.TypedMapImpl;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.jspecify.annotations.Nullable;
import picocli.CommandLine;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Base class of the commands that add a new {@link Profile}: {@code profile add} and {@code server add}.
 * The arguments say what the profile launches. They are given to the {@link LaunchTargetService},
 * which finds the {@link LaunchTarget} for them, or uses the one named with {@code --target}.
 * That launch target then creates the profile and resolves it, which installs what it launches.
 *
 * <p>A profile needs a name. Launch targets for versions can name the profile after the version,
 * for everything else the name has to be given with {@code --name}.</p>
 */
@Getter
@Setter
@RequiredArgsConstructor
public abstract class AbstractTargetAddCommand implements Runnable, LaunchTargetCommand, LongRunningCommand {
    private final LaunchTargetService launchTargetService;
    private final ProfileService profileService;
    private final McFilesService mcFilesService;
    private final FileService fileService;
    private final Console console;

    @CommandLine.Option(
        names = {"-t", "--target"},
        description = "The launch target that handles the arguments, e.g. version, server, version-file, jar, script, "
            + "packwiz or profile. If it is not specified, it is detected from the arguments.",
        completionCandidates = LaunchTargetNameCompletions.class
    )
    private @Nullable String target;

    @CommandLine.Option(
        names = {"-f", "--force"},
        description = "Overwrites an existing profile and forces reinstallation even if the version is already installed.",
        defaultValue = "false"
    )
    private boolean force;

    @CommandLine.Option(
        names = {"-u", "--url"},
        description = "Uses a custom URL to download the installer."
    )
    private @Nullable String customURL;

    @CommandLine.Option(
        names = {"-d", "--dir"},
        description = "The game directory of the profile, for servers the directory the server is installed in."
    )
    private @Nullable String customDir;

    @CommandLine.Option(
        names = {"--mc"},
        description = "The Minecraft directory to install a client into, by default the primary one (hmc.files.mc)."
    )
    private @Nullable String mc;

    @CommandLine.Option(
        names = {"-n", "--name"},
        description = "The name of the new profile. Only optional if a version is added, which the profile can be named after."
    )
    private @Nullable String name;

    @CommandLine.Parameters(
        paramLabel = "TARGET",
        description = "What the profile launches, e.g. 'fabric 1.21.1', the path of a jar, or with "
            + "'--target profile' the name of a profile to inherit from.",
        completionCandidates = LaunchTargetCompletions.class
    )
    private List<String> targetArgs = new ArrayList<>();

    /**
     * @return what is added by this command, for messages, e.g. {@code profile}.
     */
    protected abstract String getKind();

    /**
     * @return what this command did with what it added, for messages, e.g. {@code Added}.
     */
    protected String getVerb() {
        return "Added";
    }

    @Override
    public boolean acceptsProfileNames() {
        return false;
    }

    @Override
    public void run() {
        add();
    }

    /**
     * Adds a profile for the arguments of this command.
     *
     * @return the stored profile.
     */
    protected Profile add() {
        if (targetArgs.isEmpty()) {
            throw new IllegalArgumentException("Please specify what to add, e.g. 'fabric 1.21.1'.");
        }

        LaunchTargetService.TargetMatch match = launchTargetService.parse(target, new TargetArgs(targetArgs, getSides()));
        String name = this.name;
        if (name == null) {
            // decided before anything is installed
            name = match.target().defaultName(match.parsed().handle()).orElseThrow(() -> new IllegalArgumentException(
                "Please specify a name for the " + getKind() + " with --name."
            ));
        }

        Optional<Profile> existing = profileService.getProfile(name);
        if (existing.isPresent()) {
            checkExisting(existing.get());
            if (!force) {
                throw new IllegalArgumentException(
                    capitalize(getKind()) + " " + name + " already exists, use --force to overwrite it."
                );
            }
        }

        TypedMap options = new TypedMapImpl();
        options.put(LaunchTarget.NEW_PROFILE, true);
        options.put(ClientInstaller.FORCE_INSTALL, force);
        if (customURL != null) {
            options.put(ClientInstaller.CUSTOM_INSTALLER_URL, URI.create(customURL));
        }

        if (customDir != null) {
            options.put(LaunchTarget.GAME_DIR, fileService.getUserPath(customDir));
        }

        if (mc != null) {
            options.put(LaunchTarget.MC_FILES, mcFilesService.of(fileService.getUserPath(mc)));
        }

        Profile profile = launchTargetService.create(name, match, options);
        profile = launchTargetService.resolve(profile, options);
        profile = profileService.save(profile);
        Profile added = profile;
        String what = launchTargetService.describe(added).version()
            .map(version -> "version " + version.withSide(null))
            .orElseGet(() -> launchTargetService.getLabel(added));
        console.write(getVerb() + " " + getKind() + " " + added.name() + " for " + what);
        return added;
    }

    /**
     * Called for a stored profile with the name of the profile to add, before it is overwritten.
     *
     * @param existing the stored profile that is about to be overwritten.
     * @throws IllegalArgumentException if the profile must not be overwritten, not even with {@code --force}.
     */
    protected void checkExisting(Profile existing) throws IllegalArgumentException {
        // by default every profile may be overwritten
    }

    private static String capitalize(String string) {
        return string.isEmpty() ? string : Character.toUpperCase(string.charAt(0)) + string.substring(1);
    }

}
