package io.github.headlesshq.headlessmc.files.cache;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;

public interface CacheExceptionHandler {
    void error(CacheFunction function, String message) throws HeadlessMcException;

    void error(CacheFunction function, String message, Throwable cause) throws HeadlessMcException;

    void info(CacheFunction function, String message) throws HeadlessMcException;

    static CacheExceptionHandler logging() {
        return new Slf4JExceptionHandler();
    }

    static CacheExceptionHandler throwing() {
        return new ThrowingExceptionHandler();
    }

}
