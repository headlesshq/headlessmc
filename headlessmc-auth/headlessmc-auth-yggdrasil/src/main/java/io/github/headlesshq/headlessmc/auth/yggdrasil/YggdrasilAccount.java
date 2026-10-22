package io.github.headlesshq.headlessmc.auth.yggdrasil;

import io.github.headlesshq.headlessmc.auth.Account;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class YggdrasilAccount {
    private final String serverUrl;
    private final String accessToken;
    private final String clientToken;
    private final String uuid;
    private final String name;
    /** The name used to log in to the Yggdrasil server, may differ from {@link #name}. */
    private final String username;
    /** The password used to log in to the Yggdrasil server. */
    private final String password;

    public Account asAccount() {
        return new Account(
            YggdrasilAuthProvider.NAME,
            name,
            uuid,
            // the game is handed the token, so it has to be the accessToken
            // and not the clientToken
            accessToken,
            YggdrasilAuthProvider.TYPE,
            "" // xuid not supported
        );
    }

}
