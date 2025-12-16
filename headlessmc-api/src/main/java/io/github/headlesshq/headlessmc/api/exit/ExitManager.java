package io.github.headlesshq.headlessmc.api.exit;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;

/**
 * Manages the exit of HeadlessMc, by default {@link System#exit(int)}.
 */
@Setter
public class ExitManager {
    private final Set<Thread> tasks = Collections.newSetFromMap(new WeakHashMap<>());
    /**
     * Called by {@link #exit(int)}.
     */
    private Consumer<Integer> exitManager = System::exit;
    /**
     * Called by {@link #onMainThreadEnd(Throwable)}.
     */
    private Consumer<@Nullable Throwable> mainThreadEndHook = throwable -> {};
    /**
     * The exit code if {@link #exit(int)} has been called or {@code null}.
     */
    @Getter
    private Integer exitCode;

    /**
     * Adds a task Thread, a Thread that keeps HeadlessMc from exiting until it is finished.
     * These Threads are stored as weak references and will be garbage collected once they are done.
     * 
     * @param thread the task Thread to add.
     */
    public void addTaskThread(Thread thread) {
        synchronized (tasks) {
            tasks.add(thread);
        }
    }

    /**
     * Calls the configured exit manager with the given exit code.
     *
     * @param exitCode the exit code to exit the process.
     */
    public void exit(int exitCode) {
        this.exitCode = exitCode; // in case this doesnt exit the process
        this.exitManager.accept(exitCode);
    }

    /**
     * Call this when the application is about to end.
     *
     * @param throwable the Throwable thrown at the end of the main thread.
     */
    public void onMainThreadEnd(@Nullable Throwable throwable) {
        if (throwable == null) {
            synchronized (tasks) {
                tasks.forEach(thread -> {
                    try {
                        thread.join();
                    } catch (InterruptedException ignored) {

                    }
                });
            }
        }

        mainThreadEndHook.accept(throwable);
    }

}
