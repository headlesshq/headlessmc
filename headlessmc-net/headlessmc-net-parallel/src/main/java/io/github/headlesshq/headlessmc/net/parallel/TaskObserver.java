package io.github.headlesshq.headlessmc.net.parallel;

@FunctionalInterface
public interface TaskObserver<T extends ParallelTask> {
    void onTaskCompleted(T task);

    default void onTaskFailed(T task, Exception exception) throws Exception {
        throw exception;
    }

    static <T extends ParallelTask> TaskObserver<T> none() {
        return _ -> {};
    }

}
