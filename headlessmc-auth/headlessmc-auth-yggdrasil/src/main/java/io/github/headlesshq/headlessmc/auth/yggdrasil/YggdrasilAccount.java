package io.github.headlesshq.headlessmc.auth.yggdrasil;

import io.github.headlesshq.headlessmc.auth.Account;
import lombok.Data;

@Data // TODO: register for reflection
public class YggdrasilAccount {
    private final String serverUrl;
    private final String accessToken;
    private final String clientToken;
    private final String uuid;
    private final String name;
    private final String username; // 第三方登录用户名
    private final String password; // 第三方登录密码

    public Account asAccount() {
        return new Account(
            YggdrasilAuthProvider.NAME,
            name,
            uuid,
            clientToken,
            YggdrasilAuthProvider.TYPE,
            "" // xuid not supported
        );
    }

}
