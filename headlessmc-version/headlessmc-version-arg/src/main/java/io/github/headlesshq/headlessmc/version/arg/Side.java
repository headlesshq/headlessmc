package io.github.headlesshq.headlessmc.version.arg;

import io.quarkus.runtime.annotations.RegisterForReflection;

import java.util.Set;

@RegisterForReflection
public enum Side {
    CLIENT,
    SERVER;

    public static final Set<Side> BOTH = Set.of(Side.CLIENT, Side.SERVER);

    public boolean isClient() {
        return CLIENT.equals(this);
    }

    public boolean isServer() {
        return SERVER.equals(this);
    }

}
