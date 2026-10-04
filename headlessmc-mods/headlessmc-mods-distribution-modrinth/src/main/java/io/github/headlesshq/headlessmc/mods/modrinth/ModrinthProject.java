package io.github.headlesshq.headlessmc.mods.modrinth;

import io.github.headlesshq.headlessmc.mods.distribution.RemoteMod;

/**
 * Represents a Modrinth project.
 *
 * @param slug        The slug of a project, used for vanity URLs. Regex: ^[\w!@$()`.+,"\-']{3,64}$
 * @param title       The title or name of the project
 *                    //@param team the id of the team that created this mod.
 * @param description A short description of the project
 * @see <a href=https://docs.modrinth.com/api/operations/getproject/>
 * https://docs.modrinth.com/api/operations/getproject/
 * </a>
 */
record ModrinthProject(String slug, String title, String description) {
    public RemoteMod toMod() {
        return new RemoteMod(slug, title, description);
    }

}
