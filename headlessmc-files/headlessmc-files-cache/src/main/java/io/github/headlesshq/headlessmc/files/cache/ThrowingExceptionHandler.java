package io.github.headlesshq.headlessmc.files.cache;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;

final class ThrowingExceptionHandler implements CacheExceptionHandler {
    @Override
    public void error(CacheFunction function, String message) throws HeadlessMcException {
        throw new CacheException(message);
    }

    @Override
    public void error(CacheFunction function, String message, Throwable cause) throws HeadlessMcException {
        throw new CacheException(message, cause);
    }

    @Override
    public void info(CacheFunction function, String message) throws HeadlessMcException {
        throw new CacheException(message);
    }

}
