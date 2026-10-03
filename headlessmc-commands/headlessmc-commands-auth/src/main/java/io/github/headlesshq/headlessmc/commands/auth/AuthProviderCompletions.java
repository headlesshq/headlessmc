package io.github.headlesshq.headlessmc.commands.auth;

import io.github.headlesshq.headlessmc.auth.AuthProvider;
import io.github.headlesshq.headlessmc.auth.AuthService;
import lombok.RequiredArgsConstructor;

import java.util.Iterator;

@RequiredArgsConstructor
public class AuthProviderCompletions implements Iterable<String> {
    private final AuthService authService;

    @Override
    public Iterator<String> iterator() {
        return authService.getProviders().stream()
            .map(AuthProvider::getName)
            .iterator();
    }

}
