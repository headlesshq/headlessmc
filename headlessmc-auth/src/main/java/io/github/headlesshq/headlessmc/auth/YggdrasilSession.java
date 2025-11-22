package io.github.headlesshq.headlessmc.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;

@Data
@AllArgsConstructor
public class YggdrasilSession {
    private final String accessToken;
    private final String clientToken;
    private final String uuid;
    private final String name;
    private final List<Profile> availableProfiles; // 所有可用角色
    private final Profile selectedProfile; // 当前选择的角色

    @Data
    @AllArgsConstructor
    public static class Profile {
        private final String id; // UUID
        private final String name; // 角色名

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Profile profile = (Profile) o;
            return java.util.Objects.equals(id, profile.id) && java.util.Objects.equals(name, profile.name);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(id, name);
        }
    }

    // 兼容旧构造方法
    public YggdrasilSession(String accessToken, String clientToken, String uuid, String name) {
        this(accessToken, clientToken, uuid, name, null, new Profile(uuid, name));
    }
}

