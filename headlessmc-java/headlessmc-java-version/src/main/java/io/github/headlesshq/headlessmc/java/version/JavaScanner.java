package io.github.headlesshq.headlessmc.java.version;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;

import java.nio.file.Path;

public interface JavaScanner {
    int scan(Path executable) throws HeadlessMcException;

}
