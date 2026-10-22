package io.github.headlesshq.headlessmc.auth.yggdrasil;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * The request and response bodies of the Yggdrasil authentication protocol.
 *
 * <p>This is an interface instead of a class holding constants, so that it cannot be
 * instantiated and the nested types are implicitly public and static.</p>
 */
public interface YggdrasilSession {

    /**
     * The request body of the {@code authenticate} endpoint.
     */
    @Data
    @NoArgsConstructor
    public static class Authenticate {
        private final Agent agent = new Agent();
        private String username;
        private String password;
        private boolean requestUser = true;

        public Authenticate(String username, String password) {
            this.username = username;
            this.password = password;
        }
    }

    /**
     * The request body of the {@code refresh} endpoint.
     *
     * <p>Setting {@link #selectedProfile} turns the refresh into a profile selection:
     * the new token is bound to that profile, which is required when the account owns
     * multiple profiles, as those tokens are issued without a bound profile.</p>
     */
    @Data
    @NoArgsConstructor
    public static class Refresh {
        private String accessToken;
        private String clientToken;
        /** Servers validate this strictly, so it must never be null. */
        private boolean requestUser = true;
        private Profile selectedProfile;

        public Refresh(String accessToken, String clientToken) {
            this.accessToken = accessToken;
            this.clientToken = clientToken;
        }

        /**
         * Refreshes and binds the new token to the given profile.
         */
        public Refresh(String accessToken, String clientToken, Profile selectedProfile) {
            this(accessToken, clientToken);
            this.selectedProfile = selectedProfile;
        }
    }

    /**
     * The request body of the {@code validate} and {@code invalidate} endpoints.
     */
    @Data
    @NoArgsConstructor
    public static class Validate {
        private String accessToken;
        private String clientToken;

        public Validate(String accessToken, String clientToken) {
            this.accessToken = accessToken;
            this.clientToken = clientToken;
        }
    }

    @Data
    @NoArgsConstructor
    public static class Agent {
        private final String name = "Minecraft";
        private final int version = 1;
    }

    /**
     * The response body of the {@code authenticate} and {@code refresh} endpoints.
     */
    @Data
    @NoArgsConstructor
    public static class Response {
        private String accessToken;
        private String clientToken;
        private Profile selectedProfile;
        private List<Profile> availableProfiles;
        private User user;
    }

    /**
     * A single Minecraft profile (character) of an account.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Profile {
        /** The profile uuid without dashes. */
        private String id;
        private String name;
    }

    @Data
    @NoArgsConstructor
    public static class User {
        private String id;
        private List<Profile> properties;
    }

}
