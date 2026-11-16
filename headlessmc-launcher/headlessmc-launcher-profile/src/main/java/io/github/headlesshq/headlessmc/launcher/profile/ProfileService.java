package io.github.headlesshq.headlessmc.launcher.profile;

import java.util.List;
import java.util.Optional;

/**
 * Stores {@link Profile}s.
 * Profiles of older formats are converted when they are read, see {@link Profile#PROFILE_VERSION},
 * their files are only rewritten when they are {@link #save(Profile) saved}.
 *
 * <p>Profiles are created by their {@link LaunchTarget}, see {@link LaunchTargetService}.</p>
 */
public interface ProfileService {
    /**
     * @param name the name of the profile, case is ignored.
     * @return the stored profile with that name.
     */
    Optional<Profile> getProfile(String name);

    /**
     * @return all stored profiles that can be read. Profiles that are invalid,
     * or have been written by a newer version of HeadlessMc, are logged and left out.
     */
    List<Profile> getProfiles();

    /**
     * Stores a profile under its name in the current format, replacing a stored profile with the same name.
     *
     * @param profile the profile to store.
     * @return the stored profile, as it is read back: equal to what {@link #getProfile(String)} returns for it.
     * Use this profile from now on, and not the one that has been given, their launch target data can differ in
     * its representation, see {@link LaunchTargetHandle}.
     */
    Profile save(Profile profile);

    /**
     * @param profile the profile to remove, its game directory is not touched.
     */
    void remove(Profile profile);

}
