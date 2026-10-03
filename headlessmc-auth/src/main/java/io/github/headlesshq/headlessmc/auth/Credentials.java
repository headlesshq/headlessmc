package io.github.headlesshq.headlessmc.auth;

import lombok.Data;

@Data
public class Credentials {
    private final String email;
    private final String password; // TODO: char[]?!

    @Override
    public String toString() {
        return "Credentials{email='" + email + "', password='***'}";
    }

}
