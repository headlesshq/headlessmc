package io.github.headlesshq.headlessmc.mods.curseforge;

import io.github.headlesshq.headlessmc.mods.distribution.RemoteMod;
import org.jspecify.annotations.Nullable;

/**
 * Represents a CurseForge mod.
 *
 * @param id                   the id of the mod.
 * @param classId              the class (mods, resource packs, ...) the mod belongs to.
 * @param slug                 the slug of the mod, unique within its class.
 * @param name                 the name of the mod.
 * @param summary              a short description of the mod.
 * @param allowModDistribution whether the author allows the mod to be downloaded by third parties.
 *                             If {@code false} files have no {@link CurseForgeFile#downloadUrl()}.
 * @param links                links to the pages of the mod.
 * @see <a href=https://docs.curseforge.com/rest-api/#tocS_Mod>https://docs.curseforge.com/rest-api/#tocS_Mod</a>
 */
record CurseForgeMod(
    long id,
    @Nullable Integer classId,
    String slug,
    String name,
    String summary,
    @Nullable Boolean allowModDistribution,
    @Nullable Links links
) {
    public RemoteMod toMod() {
        return new RemoteMod(slug, name, summary);
    }

    /**
     * Links to the pages of a {@link CurseForgeMod}.
     *
     * @param websiteUrl the CurseForge page of the mod, e.g. {@code https://www.curseforge.com/minecraft/texture-packs/faithful-32x}.
     */
    record Links(@Nullable String websiteUrl) {

    }

}
