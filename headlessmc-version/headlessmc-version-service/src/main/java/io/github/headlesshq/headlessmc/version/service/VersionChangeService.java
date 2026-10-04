package io.github.headlesshq.headlessmc.version.service;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.version.Version;

import java.util.List;

public interface VersionChangeService {
    List<Version> observeChanges(ChangeAction action) throws HeadlessMcException;
    
    @FunctionalInterface
    interface ChangeAction {
        void run() throws HeadlessMcException;
    }

}
