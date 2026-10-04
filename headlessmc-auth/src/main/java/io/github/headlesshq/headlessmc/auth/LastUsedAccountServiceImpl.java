package io.github.headlesshq.headlessmc.auth;

import lombok.RequiredArgsConstructor;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;

@RequiredArgsConstructor
public class LastUsedAccountServiceImpl implements LastUsedAccountService {
    public static final long VERSION = 0L;

    private final AuthStore<List<AccountInfo>> store;
    private final AuthService authService;
    private final BiConsumer<String, Throwable> exceptionLogger;

    @Override
    public Optional<Account> getLastUsedAccount() {
        List<AccountInfo> infos = store.getById("latest").map(LinkedList::new).orElse(new LinkedList<>());
        boolean removed = false;
        Iterator<AccountInfo> itr = infos.iterator();
        Account result = null;
        while (itr.hasNext()) {
            AccountInfo info = itr.next();
            try {
                Optional<Account> account = authService.getProvider(info.getProvider())
                    .flatMap(provider -> provider.getAccount(info.getName()));

                if (account.isPresent() && result == null) {
                    result = account.get();
                }
            } catch (AuthException e) {
                exceptionLogger.accept(String.format("Failed to get account %s/%s", info.getProvider(), info.getName()), e);
                removed = true;
                itr.remove();
            }
        }

        if (removed) {
            store.add("latest", infos);
        }

        return Optional.ofNullable(result);
    }

    @Override
    public void setLastUsedAccount(Account account) {
        List<AccountInfo> infos = store.getById("latest").map(LinkedList::new).orElse(new LinkedList<>());
        infos.removeIf(
            info -> info.getProvider().equals(account.getProvider()) && info.getName().equals(account.getName())
        );

        infos.addFirst(new AccountInfo(account.getProvider(), account.getName()));
        store.add("latest", infos);
    }

}
