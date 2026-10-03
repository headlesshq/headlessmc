package io.github.headlesshq.headlessmc.launcher.assets;

import java.nio.file.Path;

/**
 * Location of mc assets based on AssetIndex for the arguments
 * to the mc process.
 * <p>Usually this is something like
 * {@code id=1.12.2, location=.minecraft/assets}.
 * <p>But on very old versions (pre-1.6) it's:
 * {@code id=pre-1.6, location=.minecraft/resources}.
 * <p>And on legacy versions:
 * {@code id=legacy, location=.minecraft/assets/virtual/legacy}.
 * <p><p>At least on Ubuntu the official launcher may have a bug:
 * Version 1.4.5 launches with:<br>
 * {@code ----assetsDir .minecraft/assets/virtual/pre-1.6},
 * but that directory is empty and for the legacy index
 * map_to_resources is used, so it should be in the resources dir.
 *
 * @param id the id of the asset index.
 * @param location the location of the asset files.
 */
public record AssetsLocation(String id, Path location) {

}
