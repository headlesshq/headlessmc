package io.github.headlesshq.headlessmc.auth;

import lombok.Data;

@Data
public class Account implements Comparable<Account> {
    private static final String OFFLINE_UUID = "22689332a7fd41919600b0fe1135ee34";
    public static final long SCHEMA_VERSION = 0L;
    public static final String TYPE_MSA = "msa";

    private final String provider;
    private final String name;
    private final String uuid;
    private final String token; // TODO: make Secret?!
    private final String type;
    private final String xuid;

    @Override
    public int compareTo(Account o) {
        return String.CASE_INSENSITIVE_ORDER.compare(getName(), o.getName());
    }

    public static Account defaultOfflineAccount() {
        return new Account(
            AuthProvider.OFFLINE,
            "Offline",
            OFFLINE_UUID,
            "",
            TYPE_MSA,
            ""
        );
    }

}
