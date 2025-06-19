package io.github.headlesshq.headlessmc.api.exit;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Manages the exit of HeadlessMc, by default {@link System#exit(int)}.
 */
@Setter
@ApplicationScoped
public class ExitManager {
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

    // TODO: handler for UserInterrupt

    /**
     * Calls the configured exit manager with the given exit code.
     *
     * @param exitCode the exit code to exit the process.
     */
    public void exit(int exitCode) {
        this.exitCode = exitCode;
        this.exitManager.accept(exitCode);
    }

    /**
     * Call this when the application is about to end.
     *
     * @param throwable the Throwable thrown at the end of the main thread.
     */
    public void onMainThreadEnd(@Nullable Throwable throwable) {
        mainThreadEndHook.accept(throwable);
    }

}
