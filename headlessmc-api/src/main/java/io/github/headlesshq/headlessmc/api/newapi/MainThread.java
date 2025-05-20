package io.github.headlesshq.headlessmc.api.newapi;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;

import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@RequiredArgsConstructor
public class MainThread implements Executor, Runnable {
    private final Queue<Runnable> tasks = new ConcurrentLinkedQueue<>();
    @Getter
    private final Object lock = new Object();
    @Getter
    private final Thread thread;

    public boolean isOnMainThread() {
        return getThread() == Thread.currentThread();
    }

    public <V> CompletableFuture<V> submit(Callable<V> task) {
        return CompletableFuture.supplyAsync(() -> getSneaky(task), this);
    }

    public CompletableFuture<Void> submit(Runnable task) {
        return submit(() -> {
            task.run();
            return null;
        });
    }

    public CompletableFuture<?> blockUntil(CompletableFuture<?> future) {
        AtomicBoolean completed = new AtomicBoolean(false);
        future.whenComplete((result, throwable) -> {
            if (throwable != null) {
                submit(() -> {
                    completed.set(true);
                    // TODO: throw
                });
            } else {
                submit(() -> {
                    completed.set(true);
                });
            }
        });

        return submit(() -> {
            synchronized (lock) {
                while (!completed.get()) {
                    lock.wait();
                    Runnable task = tasks.poll();
                    if (task != null) {
                        task.run();
                    }
                }
            }

            return null;
        });
    }

    @Override
    public void run() {
        if (!isOnMainThread()) {
            throw new IllegalStateException("Main thread is not on the main thread");
        }

        blockUntil(new CompletableFuture<>());
    }

    @Override
    public void execute(@NotNull Runnable command) {
        synchronized (lock) {
            if (isOnMainThread()) {
                command.run();
            } else {
                tasks.add(command);
                lock.notifyAll();
            }
        }
    }

    @SneakyThrows
    private <V> V getSneaky(Callable<V> task) {
        return task.call();
    }

}
