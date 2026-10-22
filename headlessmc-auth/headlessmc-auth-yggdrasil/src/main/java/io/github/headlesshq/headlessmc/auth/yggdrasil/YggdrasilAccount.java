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
