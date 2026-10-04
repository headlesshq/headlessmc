package io.github.headlesshq.headlessmc.platform.vanilla;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.platform.client.DefaultVersionMatcher;
import io.github.headlesshq.headlessmc.platform.PlatformService;
import io.github.headlesshq.headlessmc.platform.VanillaVersionService;
import io.github.headlesshq.headlessmc.platform.client.VersionMatcher;
import io.github.headlesshq.headlessmc.version.Version;
import io.github.headlesshq.headlessmc.version.VersionProcessor;

public class VanillaVersionMatcher extends DefaultVersionMatcher implements VersionMatcher {
    public VanillaVersionMatcher(VanillaVersionService vanillaVersionService) {
        super(vanillaVersionService, vanillaVersionService);
    }

    @Override
    public boolean canMatch(PlatformService platformService, Version version, VersionProcessor processor) throws HeadlessMcException {
        return vanillaVersionService.getVersion(version.getId()).isPresent();
    }

}
