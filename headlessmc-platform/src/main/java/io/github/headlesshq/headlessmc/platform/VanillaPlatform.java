package io.github.headlesshq.headlessmc.platform;

import io.github.headlesshq.headlessmc.platform.client.ClientInstaller;
import io.github.headlesshq.headlessmc.platform.server.ServerInstaller;
import jakarta.enterprise.inject.Default;

/**
 * The Vanilla Platform is a special {@link Platform}
 * for the default Minecraft versions supplied by mojang.
 * It can be injected as a {@link Platform} by using
 * the {@link Vanilla} qualifier or as a {@link VanillaPlatform}
 * using the {@link Default} qualifier.
 *
 * @see <a href=https://www.minecraft.net>https://www.minecraft.net</a>
 */
public interface VanillaPlatform extends Platform {
    /**
     * The VanillaPlatform provides an extended implementation of
     * {@link VersionService}, a {@link VanillaVersionService},
     * which targets {@link VanillaVersion}s.
     *
     * @return the version service for the vanilla platform.
     */
    @Override
    VanillaVersionService getVersionService();

    /**
     * The vanilla platform provides both a
     * {@link ClientInstaller} and a {@link ServerInstaller}.
     *
     * @return the installer for the vanilla platform.
     */
    VanillaInstaller getInstaller();

}
