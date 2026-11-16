package io.github.headlesshq.headlessmc.launcher.profile;

import io.github.headlesshq.headlessmc.platform.PlatformService;
import io.github.headlesshq.headlessmc.util.json.JsonService;
import io.github.headlesshq.headlessmc.util.json.jackson.DefaultJacksonJsonService;
import io.github.headlesshq.headlessmc.version.arg.Side;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds a {@link LaunchTargetService} for tests.
 * The targets of the service can be changed after it has been created, which is needed for targets
 * that need the service themselves, like the {@link ProfileLaunchTarget}.
 */
public final class TestLaunchTargets {
    private final List<LaunchTarget> targets = new ArrayList<>();
    private final LaunchTargetService service;
    private final ProfileService profileService;
    private final JsonService jsonService = new DefaultJacksonJsonService();

    public TestLaunchTargets(ProfileService profileService) {
        this.profileService = profileService;
        this.service = new LaunchTargetServiceImpl(targets, profileService);
    }

    /**
     * A service with the targets that do not launch anything: fake targets for client and server versions
     * and the real {@value ProfileLaunchTarget#NAME} target.
     *
     * @param platformService the platforms to resolve versions with.
     * @param profileService  the service profiles are stored in.
     * @param root            the directory the game directories of new profiles are placed in.
     * @return the service.
     */
    public static LaunchTargetService versions(PlatformService platformService, ProfileService profileService, Path root) {
        return new TestLaunchTargets(profileService).withVersions(platformService, root).withProfiles().service();
    }

    /**
     * @return the service, it sees all targets added to this builder, also later ones.
     */
    public LaunchTargetService service() {
        return service;
    }

    /**
     * @return a json service for the handles of targets.
     */
    public JsonService jsonService() {
        return jsonService;
    }

    public TestLaunchTargets with(LaunchTarget... targets) {
        this.targets.addAll(List.of(targets));
        return this;
    }

    public TestLaunchTargets without(String name) {
        targets.removeIf(target -> target.getName().equalsIgnoreCase(name));
        return this;
    }

    /**
     * Adds {@link FakeVersionLaunchTarget}s for clients and servers.
     */
    public TestLaunchTargets withVersions(PlatformService platformService, Path root) {
        return with(
            new FakeVersionLaunchTarget(platformService, jsonService, Side.CLIENT, root),
            new FakeVersionLaunchTarget(platformService, jsonService, Side.SERVER, root)
        );
    }

    /**
     * Adds the real {@link ProfileLaunchTarget}.
     */
    public TestLaunchTargets withProfiles() {
        return with(profileTarget());
    }

    /**
     * @return a new {@link ProfileLaunchTarget} using the service of this builder, it is not added.
     */
    public ProfileLaunchTarget profileTarget() {
        return new ProfileLaunchTarget(service, profileService, jsonService);
    }

}
