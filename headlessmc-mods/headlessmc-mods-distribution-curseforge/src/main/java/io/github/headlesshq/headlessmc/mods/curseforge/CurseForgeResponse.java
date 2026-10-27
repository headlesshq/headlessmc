package io.github.headlesshq.headlessmc.mods.curseforge;

/**
 * The envelope every response of the CurseForge API is wrapped in.
 *
 * @param data the actual response.
 * @param <T>  the type of response.
 */
record CurseForgeResponse<T>(T data) {

}
