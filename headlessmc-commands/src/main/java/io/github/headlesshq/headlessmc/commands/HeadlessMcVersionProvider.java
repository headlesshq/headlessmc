package io.github.headlesshq.headlessmc.commands;

import io.github.headlesshq.headlessmc.HeadlessMc;
import io.github.headlesshq.headlessmc.updater.Update;
import io.github.headlesshq.headlessmc.updater.UpdateService;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;
import picocli.CommandLine;

import java.util.ArrayList;
import java.util.List;

/**
 * Provides the output of {@code -V/--version},
 * the version of HeadlessMc and whether a newer release is available.
 */
@Dependent
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class HeadlessMcVersionProvider implements CommandLine.IVersionProvider {
    private final UpdateService updateService;

    @Override
    public String[] getVersion() {
        List<String> lines = new ArrayList<>();
        lines.add(HeadlessMc.NAME + " " + HeadlessMc.VERSION);
        updateService.checkForUpdate()
            .map(Update::getMessage)
            .ifPresent(lines::addAll);
        return lines.toArray(new String[0]);
    }

}
