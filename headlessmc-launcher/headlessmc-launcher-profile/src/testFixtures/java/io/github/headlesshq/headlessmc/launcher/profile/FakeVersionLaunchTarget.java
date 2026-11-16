package io.github.headlesshq.headlessmc.launcher.profile;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.launcher.process.LaunchException;
import io.github.headlesshq.headlessmc.launcher.process.ProcessLauncher;
import io.github.headlesshq.headlessmc.platform.PlatformService;
import io.github.headlesshq.headlessmc.platform.VersionID;
import io.github.headlesshq.headlessmc.util.json.JsonService;
import io.github.headlesshq.headlessmc.util.typemap.TypedMap;
import io.github.headlesshq.headlessmc.version.arg.Side;

import java.nio.file.Path;
import java.util.List;

/**
 * The launch target for versions of one side, without resolving and launching:
 * arguments are parsed and described like by the real targets, see {@link AbstractVersionLaunchTarget},
 * but a profile is resolved to itself and cannot be launched.
 * For tests that need profiles for versions, but do not launch them.
 */
public class FakeVersionLaunchTarget extends AbstractVersionLaunchTarget {
    private final PlatformService platformService;
    private final JsonService jsonService;
    private final Side side;
    private final Path root;

    /**
     * @param platformService the platforms to resolve versions with.
     * @param jsonService     the service to convert handles with.
     * @param side            the side of this target.
     * @param root            the directory the game directories of new profiles are placed in.
     */
    public FakeVersionLaunchTarget(PlatformService platformService, JsonService jsonService, Side side, Path root) {
        this.platformService = platformService;
        this.jsonService = jsonService;
        this.side = side;
        this.root = root;
    }

    @Override
    protected JsonService jsonService() {
        return jsonService;
    }

    @Override
    protected PlatformService platformService() {
        return platformService;
    }

    @Override
    protected Side getSide() {
        return side;
    }

    @Override
    protected Path defaultPath(String name, VersionHandle handle, TypedMap options) {
        return root.resolve(name);
    }

    /**
     * The fake platforms of tests usually have no client or server support, as they cannot install anything.
     * This target does not install anything either, so it accepts them.
     */
    @Override
    protected boolean isSupported(VersionID id) {
        return true;
    }

    @Override
    protected List<String> defaultGameArgs() {
        return side.isServer() ? List.of("nogui") : List.of();
    }

    @Override
    public Profile resolve(Profile profile, TypedMap options) throws HeadlessMcException {
        validate(profile);
        return profile;
    }

    @Override
    public ProcessLauncher launcher(Profile profile, TypedMap options) throws HeadlessMcException {
        throw new LaunchException("FakeVersionLaunchTarget cannot launch " + profile.name());
    }

}
