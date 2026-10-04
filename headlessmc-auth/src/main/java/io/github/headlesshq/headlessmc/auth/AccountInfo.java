package io.github.headlesshq.headlessmc.auth;

import lombok.Data;

@Data
public class AccountInfo {
    private final String provider;
    private final String name;

}
