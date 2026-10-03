package io.github.headlesshq.headlessmc.files.cache;

import org.slf4j.LoggerFactory;

final class Slf4JExceptionHandler implements CacheExceptionHandler {
    @Override
    public void error(CacheFunction function, String message) {
        LoggerFactory.getLogger(function.toString()).error(message);
    }

    @Override
    public void error(CacheFunction function, String message, Throwable cause) {
        LoggerFactory.getLogger(function.toString()).error(message, cause);
    }

    @Override
    public void info(CacheFunction function, String message) {
        LoggerFactory.getLogger(function.toString()).info(message);
    }

}
