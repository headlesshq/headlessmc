package io.github.headlesshq.headlessmc.api.newapi;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

public interface MainThreadManager {
    boolean isOnMainThread();

    CompletableFuture<Void> submit(Runnable runnable);

    <V> CompletableFuture<V> submit(Callable<V> runnable);

    void blockUntil(CompletableFuture<?> future);

}
